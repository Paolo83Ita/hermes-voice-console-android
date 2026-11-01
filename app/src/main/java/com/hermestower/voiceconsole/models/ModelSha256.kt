package com.hermestower.voiceconsole.models

import java.io.InputStream
import java.security.MessageDigest

/** Streaming SHA-256 for download verification (bounded memory, no whole-file reads). */
object ModelSha256 {

    fun ofBytes(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun ofStream(stream: InputStream, bufferSize: Int = 64 * 1024): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return digest.digest().toHex()
    }

    fun ofFile(path: java.nio.file.Path): String =
        ofStream(java.nio.file.Files.newInputStream(path))

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

/** Deterministic test-artifact generator: same seed → same bytes → same SHA-256. */
object DeterministicTestArtifact {

    /**
     * Small (default 4096 bytes) deterministic artifact used to qualify the whole
     * download→verify→install→activate→rollback→remove path without any real model.
     * Not a model; a probe fixture generated for testing (GRAPH 2 §5).
     */
    fun generate(seed: String, sizeBytes: Int = 4096): ByteArray {
        require(sizeBytes in 1..8 * 1024 * 1024) { "test artifact size out of bounds" }
        val out = ByteArray(sizeBytes)
        var h = 1125899906842597L
        seed.forEach { h = 31 * h + it.code }
        for (i in out.indices) {
            h = h xor (h shl 13)
            h = h xor (h ushr 17)
            h = h xor (h shl 5)
            out[i] = ((h ushr 24) and 0xFF).toByte()
        }
        out[0] = 'H'.code.toByte()
        out[1] = 'V'.code.toByte()
        out[2] = 'C'.code.toByte()
        out[3] = '2'.code.toByte()
        return out
    }
}
