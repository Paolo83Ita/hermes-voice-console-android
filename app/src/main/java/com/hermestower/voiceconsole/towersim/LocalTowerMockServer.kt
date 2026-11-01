package com.hermestower.voiceconsole.towersim

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Deterministic, scriptable local HTTP mock of the Hermes Tower (GRAPH 2 §10/§12).
 * Runs on 127.0.0.1 with an ephemeral port; used by the Self-Test Center transport
 * checks, the RealTowerBridge adapter tests, and the model-artifact download path.
 * Zero external dependencies (raw ServerSocket); behavior is explicit via a script
 * map so tests never rely on timing except where a delay/timeout is the subject.
 */
class LocalTowerMockServer(
    private val protocolVersion: String = "hermes.console.v0.1",
) {
    enum class Behavior {
        OK,                    // normal success responses
        AUTH_FAILURE,          // 401 on authenticated endpoints
        PROTOCOL_MISMATCH,     // handshake reports an incompatible protocol version
        DELAYED,               // adds a fixed delay to the response
        TIMEOUT,               // accepts the connection but never responds
        MALFORMED_RESPONSE,    // responds with non-JSON garbage
        INTERRUPTED_STREAM,    // streaming response cut mid-body
        DUPLICATE_DETECT,      // second identical idempotency key gets 409
        DISCONNECT_ON_CONNECT, // closes the socket immediately after accept
    }

    data class Artifact(val bytes: ByteArray, val delayMs: Long = 0)

    private val socket: ServerSocket = ServerSocket(0, 64, java.net.InetAddress.getByName("127.0.0.1"))
    @Volatile private var running = true
    private val thread: Thread
    private val behaviors = ConcurrentHashMap<String, Behavior>()
    private val artifacts = ConcurrentHashMap<String, Artifact>()
    private val seenIdempotencyKeys = ConcurrentHashMap.newKeySet<String>()
    val requestCount = AtomicInteger(0)
    val lastAuthHeader = ConcurrentHashMap<String, String>()

    val port: Int get() = socket.localPort
    val baseUrl: String get() = "http://127.0.0.1:$port"

    fun setBehavior(path: String, behavior: Behavior) {
        behaviors[path] = behavior
    }

    fun behaviorOf(path: String): Behavior = behaviors[path] ?: Behavior.OK

    fun putArtifact(name: String, artifact: Artifact) {
        artifacts[name] = artifact
    }

    init {
        thread = Thread({ loop() }, "hermes-towersim").apply {
            isDaemon = true
            start()
        }
    }

    fun close() {
        running = false
        runCatching { socket.close() }
        runCatching { thread.interrupt() }
    }

    private fun loop() {
        while (running) {
            val client = try {
                socket.accept()
            } catch (_: SocketException) {
                return
            } catch (_: IOException) {
                return
            }
            Thread({ handle(client) }, "hermes-towersim-conn").apply { isDaemon = true; start() }
        }
    }

    private fun handle(raw: Socket) {
        try {
            raw.soTimeout = 15_000
            raw.use { socket ->
                val out: OutputStream = socket.getOutputStream()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                val requestLine = reader.readLine() ?: return
                requestCount.incrementAndGet()
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val headers = HashMap<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) drain(reader, contentLength)
                val path = parts[1].substringBefore('?')
                when {
                    path.startsWith("/artifacts/") -> serveArtifact(path.removePrefix("/artifacts/"), headers, out)
                    else -> serveTower(parts[0], path, headers, out)
                }
            }
        } catch (_: SocketException) {
            // expected for DISCONNECT / INTERRUPTED_STREAM behaviors
        } catch (_: IOException) {
            // client aborted mid-response
        } catch (_: Exception) {
            // the mock server must never crash the test or app process
        }
    }

    private fun drain(reader: BufferedReader, count: Int) {
        var remaining = count
        val buffer = CharArray(1024)
        while (remaining > 0) {
            val read = reader.read(buffer, 0, minOf(buffer.size, remaining))
            if (read < 0) break
            remaining -= read
        }
    }

    private fun serveTower(method: String, path: String, headers: Map<String, String>, out: OutputStream) {
        headers["authorization"]?.let { lastAuthHeader[path] = it }
        val behavior = behaviors[path] ?: Behavior.OK
        when (behavior) {
            Behavior.DISCONNECT_ON_CONNECT -> return // handler exit closes the socket
            Behavior.TIMEOUT -> {
                Thread.sleep(14_000) // caller's bounded read timeout fires first
                return
            }
            Behavior.DELAYED -> Thread.sleep(RESPONSE_DELAY_MS)
            else -> Unit
        }
        when (behavior) {
            Behavior.AUTH_FAILURE -> writeJson(out, 401, """{"classification":"AUTH","retryable":false,"message":"invalid token"}""")
            Behavior.PROTOCOL_MISMATCH -> writeJson(out, 200, """{"protocol":"hermes.tower.v9.9","accepted":false,"message":"protocol mismatch"}""")
            Behavior.MALFORMED_RESPONSE -> writeJson(out, 200, "<not-json>")
            Behavior.DUPLICATE_DETECT -> {
                val key = headers["idempotency-key"]
                if (key != null && !seenIdempotencyKeys.add(key)) {
                    writeJson(out, 409, """{"classification":"DUPLICATE","retryable":false,"message":"idempotency key already used"}""")
                } else {
                    writeJson(out, 200, okJson(path))
                }
            }
            Behavior.INTERRUPTED_STREAM -> {
                val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 4096\r\nConnection: close\r\n\r\n"
                out.write(header.toByteArray(StandardCharsets.US_ASCII))
                out.write("""{"partial":true,"data":"trunc""".toByteArray(StandardCharsets.UTF_8))
                out.flush()
                // handler exits: socket closes with far fewer bytes than promised
            }
            else -> {
                if (method == "POST" && path.contains("request")) {
                    writeNdjson(out, ndjsonRequestEvents())
                } else {
                    writeJson(out, 200, okJson(path))
                }
            }
        }
    }

    /** POST /request answers with the proposed contract event stream (NDJSON). */
    private fun ndjsonRequestEvents(): String = listOf(
        """{"protocol":"$protocolVersion","type":"ACK","stage":"ACCEPTED","timestamp":"2026-11-01T00:00:00Z"}""",
        """{"protocol":"$protocolVersion","type":"PROGRESS","stage":"ROUTED","timestamp":"2026-11-01T00:00:00Z"}""",
        """{"protocol":"$protocolVersion","type":"DELTA","deltaText":"Hello ","timestamp":"2026-11-01T00:00:00Z"}""",
        """{"protocol":"$protocolVersion","type":"FINAL","finalText":"Hello from the mock Tower.","state":"COMPLETED","receiptRef":"rcp-sim-0001","timestamp":"2026-11-01T00:00:00Z"}""",
    ).joinToString("\n")

    private fun writeNdjson(out: OutputStream, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        writeStatus(out, 200, "OK", mapOf("Content-Type" to "application/x-ndjson"), bytes, 0, bytes.size)
    }

    private fun okJson(path: String): String = when {
        path == "/tower/protocol" -> """{"protocol":"$protocolVersion","accepted":true}"""
        path.contains("handshake") || path.contains("connect") ->
            """{"protocol":"$protocolVersion","accepted":true,"server":"hermes-towersim"}"""
        path.contains("stream") ->
            """{"protocol":"$protocolVersion","type":"STREAM","streaming":true}"""
        else -> """{"protocol":"$protocolVersion","accepted":true,"receipt":"rcp-sim-0001"}"""
    }

    private fun serveArtifact(name: String, headers: Map<String, String>, out: OutputStream) {
        val artifact = artifacts[name] ?: run {
            writeJson(out, 404, """{"error":"artifact not found"}""")
            return
        }
        if (artifact.delayMs > 0) Thread.sleep(artifact.delayMs)
        val bytes = artifact.bytes
        val range = headers["range"]?.removePrefix("bytes=")
        if (range != null && range.contains('-')) {
            val startStr = range.substringBefore('-')
            val endStr = range.substringAfter('-', "")
            val start = startStr.toLongOrNull() ?: 0L
            if (start >= bytes.size) {
                writeStatus(out, 416, "Range Not Satisfiable", emptyMap(), ByteArray(0))
                return
            }
            val end = if (endStr.isBlank()) bytes.size - 1 else minOf(endStr.toLongOrNull() ?: (bytes.size - 1), bytes.size - 1)
            val len = (end - start + 1).toInt()
            val header = mapOf(
                "Content-Type" to "application/octet-stream",
                "Content-Range" to "bytes $start-$end/${bytes.size}",
                "Accept-Ranges" to "bytes",
            )
            writeStatus(out, 206, "Partial Content", header, bytes, start.toInt(), len)
        } else {
            val header = mapOf(
                "Content-Type" to "application/octet-stream",
                "Accept-Ranges" to "bytes",
            )
            writeStatus(out, 200, "OK", header, bytes, 0, bytes.size)
        }
    }

    private fun writeJson(out: OutputStream, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val header = mapOf("Content-Type" to "application/json")
        writeStatus(out, status, reason(status), header, bytes, 0, bytes.size)
    }

    private fun writeStatus(
        out: OutputStream,
        status: Int,
        reason: String,
        headers: Map<String, String>,
        body: ByteArray,
        off: Int,
        len: Int,
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
        headers.forEach { (k, v) -> sb.append(k).append(": ").append(v).append("\r\n") }
        sb.append("Content-Length: ").append(len).append("\r\n")
        sb.append("Connection: close\r\n\r\n")
        out.write(sb.toString().toByteArray(StandardCharsets.US_ASCII))
        if (len > 0) out.write(body, off, len)
        out.flush()
    }

    private fun reason(status: Int): String = when (status) {
        200 -> "OK"; 206 -> "Partial Content"; 401 -> "Unauthorized"; 404 -> "Not Found"
        409 -> "Conflict"; 416 -> "Range Not Satisfiable"; else -> "Error"
    }

    companion object {
        const val RESPONSE_DELAY_MS = 400L
    }
}
