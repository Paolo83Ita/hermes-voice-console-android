package com.hermestower.voiceconsole.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest

/**
 * GRAPH 4.6 directive §6 — OPTIONAL LOCAL LLM (one verified candidate, DEFAULT DISABLED).
 *
 * Candidate (already verified in the shipped catalog, revision-bound):
 *   org.qwen.qwen2.5-0.5b-instruct.q5-k_m — Qwen2.5-0.5B-Instruct GGUF q5_k_m,
 *   Apache-2.0, source pinned to HF commit 9217f5db… with a known SHA-256 and size.
 *
 * State machine (honest, typed):
 *   DISABLED (default — the console is fully usable without the LLM)
 *     → NOT_INSTALLED (owner enabled it; delivery chain not completed)
 *     → LOADING → LOADED (runtime bound) | ERROR (typed reason)
 *   prompt() from any state that is not LOADED returns a TYPED diagnostic reply,
 *   fromModel=false — the reply path exists and is safe in every state.
 *
 * Safe-prompt contract (LOCAL_MODEL_CANDIDATE_PLAN.md §1, FR-19):
 *  - the model is never a second Tower authority; the system prompt states the
 *    bounded scope verbatim, strips control characters, caps input length and
 *    caps generated tokens;
 *  - forbidden uses (mission reasoning, receipts, approvals, Tower-state claims)
 *    are enforced by scope + labeling, never by trusting the model.
 */
class LocalLlmEngine(
    /** Root directory of the installed GGUF (filesDir/models/llm/qwen25-05b/). */
    private val modelRoot: File,
    /** Expected SHA-256 of the installed file (from the verified catalog entry). */
    private val expectedSha256: String,
    /** Expected size in bytes (from the verified catalog entry). */
    private val expectedSizeBytes: Long,
    /** Read-only enablement flag; default DISABLED is enforced by the store. */
    private val enabledProvider: () -> Boolean,
    /** Runtime seam; the shipped build always injects [NoVerifiedLlmRuntime]. */
    private val runtime: LlmRuntime = NoVerifiedLlmRuntime(),
    /** Candidate identity for diagnostics/receipts (never invented). */
    private val candidateIdentity: String = "org.qwen.qwen2.5-0.5b-instruct.q5-k-m",
    /** GRAPH 4.10 §4: resolved response-language tag for the system prompt. */
    private val localeTagProvider: () -> String = { "en" },
) {

    enum class State { DISABLED, NOT_INSTALLED, LOADING, LOADED, ERROR }

    enum class DiagnosticCode(val asText: String) {
        DISABLED_BY_DEFAULT("LLM_DISABLED_BY_DEFAULT"),
        NOT_INSTALLED("LLM_MODEL_NOT_INSTALLED"),
        HASH_MISMATCH("LLM_HASH_MISMATCH"),
        SIZE_MISMATCH("LLM_SIZE_MISMATCH"),
        RUNTIME_UNAVAILABLE("LLM_RUNTIME_PENDING_VERIFIED_SOURCE"),
        LOAD_FAILED("LLM_LOAD_FAILED"),
        PROMPT_TOO_LONG("LLM_PROMPT_TOO_LONG"),
        OK("LLM_OK"),
    }

    data class LlmDiagnostics(
        val enabled: Boolean,
        val state: State,
        val candidateIdentity: String,
        val runtimeName: String,
        val installedFileExists: Boolean,
        val installedSizeBytes: Long?,
        val shaVerified: Boolean,
        val lastDiagnosticCode: DiagnosticCode,
    )

    data class LlmReply(
        val text: String,
        val intent: String,
        val fromModel: Boolean,
        val diagnosticCode: DiagnosticCode,
    )

    private val _state = MutableStateFlow(State.DISABLED)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * GRAPH 4.14 §25: the SELECTABILITY truth for the Local-experimental
     * profile. True when a verified runtime is injected (llama.cpp in-tree
     * since CP-G4.14-005); false only for the NoVerifiedLlmRuntime fallback.
     * Model delivery/activation remains the catalog ladder's job.
     */
    val hasVerifiedRuntime: Boolean
        get() = runtime !is NoVerifiedLlmRuntime

    @Volatile
    private var lastCode: DiagnosticCode = DiagnosticCode.DISABLED_BY_DEFAULT

    @Volatile
    private var lastIntegrityCheck: DiagnosticCode = DiagnosticCode.NOT_INSTALLED

    @Volatile
    private var loadedModelFile: File? = null

    /** Resolves the installed GGUF file (delivery chain may nest one level deep). */
    fun resolveInstalledFile(): File? {
        if (!modelRoot.isDirectory) return null
        val direct = File(modelRoot, "qwen2.5-0.5b-instruct-q5_k_m.gguf")
        if (direct.isFile) return direct
        return modelRoot.walkTopDown()
            .maxDepth(2)
            .firstOrNull { it.isFile && it.name.endsWith(".gguf") }
    }

    /** Real integrity check: file present, size equal, SHA-256 equal. */
    fun verifyInstalledIntegrity(): DiagnosticCode {
        val result = run {
            val f = resolveInstalledFile() ?: return@run DiagnosticCode.NOT_INSTALLED
            val size = f.length()
            if (expectedSizeBytes > 0 && size != expectedSizeBytes) {
                return@run DiagnosticCode.SIZE_MISMATCH
            }
            if (expectedSha256.isNotBlank()) {
                val digest = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
                val hex = digest.joinToString("") { "%02x".format(it) }
                if (!hex.equals(expectedSha256, ignoreCase = true)) {
                    return@run DiagnosticCode.HASH_MISMATCH
                }
            }
            DiagnosticCode.OK
        }
        lastCode = result
        lastIntegrityCheck = result
        return result
    }

    /** Enable is an explicit Owner action; the engine NEVER self-enables. */
    fun syncFromEnabledFlag() {
        if (_state.value == State.LOADED || _state.value == State.LOADING) return
        _state.value = if (enabledProvider()) State.NOT_INSTALLED else State.DISABLED
    }

    fun load() {
        if (!enabledProvider()) {
            lastCode = DiagnosticCode.DISABLED_BY_DEFAULT
            _state.value = State.DISABLED
            return
        }
        val integrity = verifyInstalledIntegrity()
        if (integrity != DiagnosticCode.OK) {
            _state.value = State.NOT_INSTALLED
            return
        }
        _state.value = State.LOADING
        runCatching {
            val f = resolveInstalledFile() ?: error("model vanished during load")
            runtime.load(f, contextTokens = DEFAULT_CONTEXT_TOKENS)
            loadedModelFile = f
            lastCode = DiagnosticCode.OK
            _state.value = State.LOADED
        }.onFailure { t ->
            loadedModelFile = null
            lastCode = if (t is LlmRuntimeUnavailableException) {
                DiagnosticCode.RUNTIME_UNAVAILABLE
            } else {
                DiagnosticCode.LOAD_FAILED
            }
            _state.value = State.ERROR
        }
    }

    fun unload() {
        runCatching { runtime.unload() }
        loadedModelFile = null
        lastCode = DiagnosticCode.OK
        _state.value = if (enabledProvider()) State.NOT_INSTALLED else State.DISABLED
    }

    /** The ALWAYS-SAFE reply path: typed diagnostics when the model cannot run. */
    fun prompt(userText: String, statusProvider: () -> String): LlmReply {
        if (!enabledProvider()) {
            return diagnosticReply(
                DiagnosticCode.DISABLED_BY_DEFAULT,
                "The optional local model is disabled (default). The console remains fully usable: HYBRID routing reaches Hermes Tower, and the rule-based local intents keep answering.",
            )
        }
        if (_state.value != State.LOADED) {
            return if (lastCode == DiagnosticCode.RUNTIME_UNAVAILABLE) {
                diagnosticReply(
                    DiagnosticCode.RUNTIME_UNAVAILABLE,
                    "No verified Android inference runtime is linked yet (typed LLM_RUNTIME_PENDING_VERIFIED_SOURCE). The candidate is catalog-verified and delivery-ready; generation stays blocked until a runtime with pinned source and known SHA-256 is adopted.",
                )
            } else {
                diagnosticReply(
                    DiagnosticCode.NOT_INSTALLED,
                    "The local model is enabled but not installed and verified yet. Open Models and Voice to run the verified delivery chain.",
                )
            }
        }
        val built = SafePromptBuilder.build(userText, statusProvider(), localeTagProvider())
        val generated = runCatching { runtime.generate(built, maxTokens = MAX_GENERATION_TOKENS) }
            .getOrElse { t ->
                val code = if (t is LlmRuntimeUnavailableException) {
                    DiagnosticCode.RUNTIME_UNAVAILABLE
                } else {
                    DiagnosticCode.LOAD_FAILED
                }
                return diagnosticReply(code, "Generation failed with a typed error; the console falls back to the rule-based local intents.")
            }
        return LlmReply(
            text = generated.take(MAX_REPLY_CHARS),
            intent = "LOCAL_LLM",
            fromModel = true,
            diagnosticCode = DiagnosticCode.OK,
        )
    }

    private fun diagnosticReply(code: DiagnosticCode, explanation: String) = LlmReply(
        text = explanation,
        intent = "LLM_${code.name}",
        fromModel = false,
        diagnosticCode = code,
    )

    fun diagnostics(): LlmDiagnostics {
        val f = resolveInstalledFile()
        // SHA re-hash deliberately NOT executed here: hashing the ~522 MB GGUF on
        // every recomposition would stall the UI thread. shaVerified reflects the
        // last explicit integrity check (load()/verifyInstalledIntegrity()).
        return LlmDiagnostics(
            enabled = enabledProvider(),
            state = _state.value,
            candidateIdentity = candidateIdentity,
            runtimeName = runtime.name,
            installedFileExists = f != null,
            installedSizeBytes = f?.length(),
            shaVerified = lastIntegrityCheck == DiagnosticCode.OK,
            lastDiagnosticCode = lastCode,
        )
    }

    companion object {
        /** Bounded context for the 0.5B class on a 780G device (plan §5 RAM budget). */
        const val DEFAULT_CONTEXT_TOKENS = 2048
        const val MAX_GENERATION_TOKENS = 256
        const val MAX_INPUT_CHARS = 512
        const val MAX_REPLY_CHARS = 1024
    }
}

/**
 * Deterministic safe-prompt construction — pure JVM logic, unit-tested.
 * The system prompt states the model's bounded role verbatim (plan §1), the
 * user turn is sanitized (control characters stripped, hard length cap) and
 * the truthful device status is included so the model never invents telemetry.
 */
object SafePromptBuilder {

    /** Hard cap mirrors LocalLlmEngine.MAX_INPUT_CHARS (single source in tests). */
    const val MAX_INPUT_CHARS = 512

    const val SYSTEM_SCOPE =
        "You are the optional local assistant of the Hermes Voice Console. " +
            "You are NOT Hermes Tower and never speak for it. " +
            "Permitted: greetings, small talk, command cleanup, intent normalization, " +
            "help, device-status explanation, short offline answers. " +
            "Forbidden: mission planning, consequential actions, receipts, approvals, " +
            "claiming Tower authority or Tower work. " +
            "Answer briefly in the user's language. Truthful device status: "

    fun build(userText: String, deviceStatus: String, localeTag: String = "en"): String {
        val sanitized = sanitize(userText)
        val languageDirective = " Respond in language code '$localeTag' only."
        return "$SYSTEM_SCOPE$deviceStatus$languageDirective\n\nUser: $sanitized\n\nAssistant:"
    }

    /** Control characters are stripped; the input is capped (typed at the caller). */
    fun sanitize(userText: String): String {
        val stripped = userText.replace(Regex("[\\p{Cntrl}]"), " ")
        return stripped.take(MAX_INPUT_CHARS).trim()
    }
}
