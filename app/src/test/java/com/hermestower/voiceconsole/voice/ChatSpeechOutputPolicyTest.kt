package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-004: the chat-speech output policy (§10) — the Owner's
 * P0-05 missing mute + P0-04 speaker semantics, with every binding invariant
 * pinned on the JVM.
 */
class ChatSpeechOutputPolicyTest {

    @Test
    fun `default mode is AUTO_SPEAK`() {
        assertEquals(ChatSpeechOutputPolicy.Mode.AUTO_SPEAK, ChatSpeechOutputPolicy(InMemoryKeyValueStore()).mode.value)
    }

    @Test
    fun `MUTED keeps auto-speak off but is not the microphone policy`() {
        val p = ChatSpeechOutputPolicy(InMemoryKeyValueStore())
        p.setMode(ChatSpeechOutputPolicy.Mode.MUTED, "test")
        assertFalse(p.shouldAutoSpeak())
        assertTrue(p.isMuted())
        // Unmute restores AUTO_SPEAK (not MANUAL).
        p.toggleMuted()
        assertEquals(ChatSpeechOutputPolicy.Mode.AUTO_SPEAK, p.mode.value)
        assertTrue(p.shouldAutoSpeak())
    }

    @Test
    fun `MANUAL speaks only through explicit playback`() {
        val p = ChatSpeechOutputPolicy(InMemoryKeyValueStore())
        p.setMode(ChatSpeechOutputPolicy.Mode.MANUAL, "test")
        assertFalse(p.shouldAutoSpeak())
        assertFalse(p.isMuted()) // MANUAL is not a mute
    }

    @Test
    fun `mode persists across a restart (new instance, same store)`() {
        val store = InMemoryKeyValueStore()
        val p1 = ChatSpeechOutputPolicy(store)
        p1.setMode(ChatSpeechOutputPolicy.Mode.MUTED, "test")
        val p2 = ChatSpeechOutputPolicy(store) // process restart simulation
        assertEquals(ChatSpeechOutputPolicy.Mode.MUTED, p2.mode.value)
    }

    @Test
    fun `stopping current speech never changes the policy (stopSpeaking is policy-free)`() {
        // The policy object has no stop API by design: ScriptedConsoleDriver
        // stopSpeaking() only interrupts the engine. Pinned as a contract test
        // on the driver in ScriptedConsoleDriverTest (mute toggle invariance).
        val p = ChatSpeechOutputPolicy(InMemoryKeyValueStore())
        p.setMode(ChatSpeechOutputPolicy.Mode.AUTO_SPEAK, "test")
        assertTrue(p.shouldAutoSpeak())
    }

    @Test
    fun `all three modes round-trip through the store`() {
        val store = InMemoryKeyValueStore()
        for (mode in ChatSpeechOutputPolicy.Mode.entries) {
            ChatSpeechOutputPolicy(store).setMode(mode, "test")
            assertEquals(mode, ChatSpeechOutputPolicy(store).mode.value)
        }
    }
}
