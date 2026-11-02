package com.hermestower.voiceconsole.pairing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 Owner delta §11: complete QR parser test matrix on deterministic
 * fixtures. NO claim of real Tower pairing — parser-level qualification only;
 * physical pairing requires the Tower Graph 1.7 server contract.
 */
class PairingPayloadParserTest {

    private val now = 1_700_000_000_000L
    private val validPayload =
        "hermes-pair://v1/tower-alpha" +
            "?env=local&iat=${now - 60_000}&exp=${now + 300_000}" +
            "&n=NONCE12345678&c=CHALLENGE9&ep=https%3A%2F%2Ftower.local%3A8443%2Fpair&s=console,voice"

    private fun parser(nonceStore: PairingNonceStore = InMemoryPairingNonceStore()) =
        PairingPayloadParser(nonceStore) { now }

    private fun kindOf(raw: String, nonceStore: PairingNonceStore = InMemoryPairingNonceStore()): PairingRejectKind {
        val r = parser(nonceStore).parse(raw)
        val ex = r.exceptionOrNull()
        assertTrue("expected rejection, got $r", ex is PairingRejectException)
        return (ex as PairingRejectException).reject.kind
    }

    @Test
    fun `valid QR parses with all fields`() {
        val r = parser().parse(validPayload)
        val p = r.getOrThrow()
        assertEquals(1, p.schemeVersion)
        assertEquals("tower-alpha", p.towerId)
        assertEquals("local", p.environment)
        assertEquals("NONCE12345678", p.nonce)
        assertEquals("CHALLENGE9", p.challenge)
        assertEquals("https://tower.local:8443/pair", p.pairingEndpoint)
        assertEquals(listOf("console", "voice"), p.requestedScopes)
    }

    @Test
    fun `malformed QR rejected`() {
        assertEquals(PairingRejectKind.MALFORMED, kindOf("not a qr at all"))
        assertEquals(PairingRejectKind.MALFORMED, kindOf("hermes-pair://v1/noquery"))
        assertEquals(PairingRejectKind.MALFORMED, kindOf("hermes-pair://v1/tower-x?env=local&iat=abc&exp=def&n=X&c=Y&ep=https%3A%2F%2Fa.b"))
    }

    @Test
    fun `unsupported scheme rejected`() {
        assertEquals(PairingRejectKind.UNSUPPORTED_SCHEME, kindOf("https://evil.example/pair?env=local"))
        assertEquals(PairingRejectKind.UNSUPPORTED_SCHEME, kindOf("otpauth://totp/x"))
    }

    @Test
    fun `unsupported protocol version rejected`() {
        val v2 = validPayload.replace("v1/", "v2/")
        assertEquals(PairingRejectKind.UNSUPPORTED_PROTOCOL, kindOf(v2))
        val vBad = validPayload.replace("v1/", "vX/")
        assertEquals(PairingRejectKind.UNSUPPORTED_PROTOCOL, kindOf(vBad))
    }

    @Test
    fun `expired QR rejected`() {
        val expired = "hermes-pair://v1/tower-a?env=local&iat=${now - 700_000}&exp=${now - 100_000}&n=NONCE12345678&c=C&ep=https%3A%2F%2Fa.b"
        assertEquals(PairingRejectKind.EXPIRED, kindOf(expired))
    }

    @Test
    fun `future-issued QR rejected`() {
        val future = "hermes-pair://v1/tower-a?env=local&iat=${now + 120_000}&exp=${now + 400_000}&n=NONCE12345678&c=C&ep=https%3A%2F%2Fa.b"
        assertEquals(PairingRejectKind.FUTURE_ISSUED, kindOf(future))
    }

    @Test
    fun `missing nonce rejected`() {
        val noNonce = validPayload.replace("&n=NONCE12345678", "")
        assertEquals(PairingRejectKind.MISSING_NONCE, kindOf(noNonce))
    }

    @Test
    fun `missing challenge rejected`() {
        val noChallenge = validPayload.replace("&c=CHALLENGE9", "")
        assertEquals(PairingRejectKind.MISSING_CHALLENGE, kindOf(noChallenge))
    }

    @Test
    fun `altered payload — oversized query and short nonce rejected`() {
        val oversized = validPayload + "&pad=" + "A".repeat(600)
        assertEquals(PairingRejectKind.ALTERED_PAYLOAD, kindOf(oversized))
        val shortNonce = validPayload.replace("n=NONCE12345678", "n=SHORT")
        assertEquals(PairingRejectKind.ALTERED_PAYLOAD, kindOf(shortNonce))
    }

    @Test
    fun `wrong Tower environment rejected`() {
        val cloud = validPayload.replace("env=local", "env=cloud-prod")
        assertEquals(PairingRejectKind.WRONG_ENVIRONMENT, kindOf(cloud))
        val noEnv = validPayload.replace("env=local&", "")
        assertEquals(PairingRejectKind.WRONG_ENVIRONMENT, kindOf(noEnv))
    }

    @Test
    fun `replayed nonce rejected on second scan`() {
        val store = InMemoryPairingNonceStore()
        assertTrue(parser(store).parse(validPayload).isSuccess)
        assertEquals(PairingRejectKind.REPLAYED_NONCE, kindOf(validPayload, store))
    }

    @Test
    fun `bad endpoint rejected — no invented endpoint is ever accepted`() {
        val ftp = validPayload.replace("ep=https%3A%2F%2Ftower.local%3A8443%2Fpair", "ep=ftp%3A%2F%2Fx.y")
        assertEquals(PairingRejectKind.BAD_ENDPOINT, kindOf(ftp))
        val noEp = validPayload.replace("&ep=https%3A%2F%2Ftower.local%3A8443%2Fpair", "")
        assertEquals(PairingRejectKind.BAD_ENDPOINT, kindOf(noEp))
    }

    @Test
    fun `describe is redaction-safe — no endpoint, no challenge, no nonce`() {
        val p = parser().parse(validPayload).getOrThrow()
        val d = parser().describe(p)
        assertFalse(d.contains("tower.local"))
        assertFalse(d.contains("CHALLENGE9"))
        assertFalse(d.contains("NONCE12345678"))
        assertTrue(d.contains("tower-alpha"))
    }

    @Test
    fun `mock pairing client is deterministic and never touches the network`() = runBlocking {
        val client = MockTowerPairingClient { now }
        val p = parser().parse(validPayload).getOrThrow()
        val r1 = client.pair(p, "paolo")
        val r2 = client.pair(p, "paolo")
        assertTrue(r1 is PairingResult.Success)
        assertEquals((r1 as PairingResult.Success).matchPhrase, (r2 as PairingResult.Success).matchPhrase)
        assertTrue("match phrase is 4 glyphs joined by dashes", r1.matchPhrase.length == 7)
        assertTrue("session id is the deterministic mock form", r1.sessionId.startsWith("pair-mock-"))
    }
}
