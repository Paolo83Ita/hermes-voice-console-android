package com.hermestower.voiceconsole.store

import com.hermestower.voiceconsole.domain.LocalEventStore
import java.time.Instant
import java.util.ArrayDeque

/** In-memory ring buffer (500 entries). Room-backed persistence is conditional at V0.3+ (D-004). */
class InMemoryEventStore(private val capacity: Int = 500) : LocalEventStore {

    private val buffer = ArrayDeque<LocalEventStore.Event>(capacity)
    private var seq = 0L

    override fun log(type: String, detail: String) {
        synchronized(buffer) {
            if (buffer.size >= capacity) buffer.removeFirst()
            buffer.addLast(LocalEventStore.Event(Instant.now(), type, detail))
            seq++
        }
    }

    override fun recent(limit: Int): List<LocalEventStore.Event> = synchronized(buffer) {
        buffer.toList().takeLast(limit.coerceAtMost(buffer.size))
    }

    fun size(): Int = synchronized(buffer) { buffer.size }

    fun sequence(): Long = seq

    override fun clear() = synchronized(buffer) { buffer.clear() }
}
