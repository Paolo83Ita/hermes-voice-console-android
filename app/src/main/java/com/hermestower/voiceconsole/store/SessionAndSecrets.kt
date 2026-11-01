package com.hermestower.voiceconsole.store

import com.hermestower.voiceconsole.domain.SecureConfigurationStore
import com.hermestower.voiceconsole.domain.SessionController
import com.hermestower.voiceconsole.domain.SessionController.Session
import java.time.Instant
import java.util.UUID

/** V0.1 session bookkeeping: fresh session per renewal, persistent conversation id. */
class DefaultSessionController : SessionController {

    private val conversationId = "conv-${UUID.randomUUID()}"
    private var session = Session(
        sessionId = "sess-${UUID.randomUUID()}",
        conversationId = conversationId,
        openedAt = Instant.now(),
    )

    override fun current(): Session = session

    override fun renew(): Session = Session(
        sessionId = "sess-${UUID.randomUUID()}",
        conversationId = conversationId,
        openedAt = Instant.now(),
    ).also { session = it }
}

/**
 * V0.1 secret store: in-memory only and intentionally EMPTY of secrets (SEC-01/SEC-02).
 * Keystore-backed implementation arrives with pairing in V0.5; nothing real is stored here.
 */
class InMemorySecureConfigurationStore : SecureConfigurationStore {

    private val map = HashMap<String, CharArray>()

    override val capability: SecureConfigurationStore.Capability =
        SecureConfigurationStore.Capability.IN_MEMORY_ONLY

    override fun put(key: String, value: CharArray) {
        synchronized(map) { map[key] = value.copyOf() }
    }

    override fun get(key: String): CharArray? = synchronized(map) { map[key]?.copyOf() }

    override fun clear(key: String) {
        synchronized(map) {
            map.remove(key)?.fill(' ')
        }
    }
}
