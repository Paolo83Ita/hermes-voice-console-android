package com.hermestower.voiceconsole.local

import java.io.File

/**
 * GRAPH 4.6 directive §6 — OPTIONAL LOCAL LLM.
 *
 * The runtime seam separates "the candidate is delivered and its integrity is
 * verified" (real, implemented) from "a verified on-device inference runtime is
 * linked" (NOT YET — no llama.cpp-derived Android artifact on a reviewed host
 * with a pinned revision and a known SHA-256 has been verified this mission;
 * the sherpa-onnx catalog incident forbids claiming one without proof).
 *
 * Contract:
 *  - [load] binds the runtime to an installed, hash-verified GGUF file;
 *  - [unload] releases every native resource and resets the binding;
 *  - [generate] runs the safe prompt and returns model text (never Tower truth);
 *  - every failure is TYPED — no silent substitution, no fabricated output.
 */
interface LlmRuntime {
    /** Human-visible runtime identity for the Diagnostics card (never invented). */
    val name: String

    /** Binds the runtime to the installed model file. Throws [LlmRuntimeUnavailableException] when unbindable. */
    fun load(modelFile: File, contextTokens: Int)

    /** Releases every resource held for the current binding (idempotent). */
    fun unload()

    /** Generates a bounded completion for an already-built safe prompt. */
    fun generate(prompt: String, maxTokens: Int): String
}

/** Typed failure carrying the exact gap for the Diagnostics card and receipts. */
class LlmRuntimeUnavailableException(val reasonCode: String) :
    IllegalStateException("LLM runtime unavailable: $reasonCode")

/**
 * The ONLY runtime implementation shipped in this release. Every call fails
 * with the same typed reason (LLM_RUNTIME_PENDING_VERIFIED_SOURCE): the engine
 * above it reports the candidate, its integrity and the exact gap — and never
 * invents a completion. When a verified Android llama.cpp runtime is adopted
 * (source pinned + hashed + revision-bound), it implements this interface and
 * the engine switches by constructor injection only.
 */
class NoVerifiedLlmRuntime : LlmRuntime {
    override val name: String = "no verified Android runtime linked"

    override fun load(modelFile: File, contextTokens: Int) {
        throw LlmRuntimeUnavailableException("LLM_RUNTIME_PENDING_VERIFIED_SOURCE")
    }

    override fun unload() {
        // Nothing was ever bound — unload stays a no-op by construction.
    }

    override fun generate(prompt: String, maxTokens: Int): String {
        throw LlmRuntimeUnavailableException("LLM_RUNTIME_PENDING_VERIFIED_SOURCE")
    }
}
