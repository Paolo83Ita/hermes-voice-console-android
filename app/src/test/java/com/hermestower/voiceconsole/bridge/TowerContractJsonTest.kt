package com.hermestower.voiceconsole.bridge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerContractJsonTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `request frame round-trips`() {
        val frame = TowerRequestFrame(
            deviceId = "console-01",
            userIdRef = "owner-paolo",
            sessionId = "sess-1",
            conversationId = "conv-1",
            requestId = "req-1",
            transcript = "piano di missione per domani",
            confidence = 0.92f,
            requestedMode = "HYBRID",
            effectiveMode = "HYBRID",
            localRouting = LocalRoutingHint("ROUTE_TOWER", "consequential pattern", "CONSEQUENTIAL", true),
            timestamp = "2026-10-30T12:00:00Z",
        )
        val encoded = json.encodeToString(frame)
        assertTrue(encoded.contains("\"protocol\":\"${TowerProtocol.VERSION}\""))
        assertTrue(encoded.contains("it-IT"))
        val decoded = json.decodeFromString<TowerRequestFrame>(encoded)
        assertEquals(frame, decoded)
    }

    @Test
    fun `event frame round-trips with error body`() {
        val event = TowerEventFrame(
            type = TowerEventFrame.ERROR,
            requestId = "req-9",
            error = TowerErrorBody("TRANSPORT", retryable = true, message = "simulated failure"),
            timestamp = "2026-10-30T12:00:01Z",
        )
        val decoded = json.decodeFromString<TowerEventFrame>(json.encodeToString(event))
        assertEquals(event, decoded)
        assertEquals("TRANSPORT", decoded.error?.classification)
    }

    @Test
    fun `unknown fields are ignored, never fatal`() {
        val payload = """
            {"protocol":"${TowerProtocol.VERSION}","type":"ACK","requestId":"req-1",
             "stage":"ACCEPTED","someFutureField":{"nested":true},"timestamp":"2026-10-30T12:00:02Z"}
        """.trimIndent()
        val decoded = json.decodeFromString<TowerEventFrame>(payload)
        assertEquals("ACK", decoded.type)
        assertEquals("ACCEPTED", decoded.stage)
    }

    @Test
    fun `hello frame carries protocol version and capabilities`() {
        val hello = TowerHelloFrame(
            deviceId = "console-01",
            deviceModel = "Xiaomi 11 Lite 5G (verify on device)",
            appVersion = "0.1.0-alpha",
            capabilities = listOf("stt:it-IT", "tts", "captions", "privacy_mute", "queue"),
            timestamp = "2026-10-30T12:00:03Z",
        )
        val encoded = json.encodeToString(hello)
        assertTrue(encoded.contains("\"type\":\"HELLO\""))
        assertTrue(encoded.contains("privacy_mute"))
    }
}
