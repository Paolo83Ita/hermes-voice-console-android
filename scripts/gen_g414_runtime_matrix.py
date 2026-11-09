#!/usr/bin/env python3
"""Graph 4.14 — generate ANDROID_RUNTIME_REQUIREMENT_MATRIX.{json,md} (CP-G4.14-002/003).

Single source of truth for the runtime matrix; emits both artifacts.
Rows cover EVERY bundled/downloadable component visible in Models and Voice.
Decisions use the charter §5 vocabulary exactly.
"""
import json, os

OUT = "/home/z/my-project/hermes-voice-console-android/docs/governance"

# Decision vocabulary (charter §5):
D_REUSE = "REUSE_EXISTING_RUNTIME"
D_EXTEND = "EXTEND_EXISTING_RUNTIME"
D_ADD = "ADD_VERIFIED_ANDROID_RUNTIME"
D_ADAPT = "ADAPT_MODEL_TO_EXISTING_RUNTIME"
D_REPLACE = "REPLACE_MODEL_WITH_COMPATIBLE_ARTIFACT"
D_LAB = "KEEP_DOWNLOAD_ONLY_IN_ADVANCED_LAB"
D_BLOCK = "BLOCK_UNTIL_RUNTIME_AVAILABLE"
D_REMOVE = "REMOVE_MISLEADING_ENTRY"

COMMON_ABI = ["arm64-v8a"]
MIN_SDK = 30
ORT = "com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED)"

rows = [
 dict(componentId="org.vosk.model.small-it-0.22", displayName="Vosk Italian (small)", capability="SPEECH_TO_TEXT",
      modelFormat="kaldi-zip (int8)", quantization="int8", currentArtifact="vosk-model-small-it-0.22.zip (verified 2026-11-02, 49,665,141 B, sha256 9ec65e75…f3267)",
      requiredAndroidRuntime="com.alphacephei:vosk-android:0.3.75 (Maven Central)",
      currentRuntimeState="INTEGRATED — VoskFrameSttEngine loads org.vosk.Model from filesDir/models/stt/vosk-small-it; real Recognizer 16 kHz",
      authoritativeRuntimeSource="Maven Central (com.alphacephei:vosk-android)", runtimeVersion="0.3.75", immutableRevision="vosk-android 0.3.75 + model small-it-0.22 frozen alphacephei.com URL",
      license="Apache-2.0 (runtime) / Apache-2.0 (model)", mavenCoordinates="com.alphacephei:vosk-android:0.3.75",
      supportedAbis=COMMON_ABI, minSdk=21, nativeDependencies="libvosk (JNI, bundled by AAR)",
      preprocessing="16 kHz mono PCM16 20 ms frames (AudioRecordFrameSource)",
      postprocessing="Kaldi JSON partial/final → Transcript(text, confidence, isFinal, it)",
      memoryEstimateMB=256, storageEstimateMB=91, loadApi="VoskNativeBridge.loadModel(dir)",
      inferenceApi="Recognizer.acceptWaveForm / finalResult", unloadApi="Model finalization (bridge release)",
      fallback="Android system SpeechRecognizer (EXTRA_PREFER_OFFLINE)", rollback="Keep system STT route; model removal → NOT_INSTALLED",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING (Owner device)"),
 dict(componentId="org.sherpa-onnx.tts.runtime", displayName="sherpa-onnx TTS runtime", capability="TTS_ENGINE",
      modelFormat="native-runtime (code)", quantization="n/a", currentArtifact="AAR v1.12.24 (sha256 ad052d51…6c7e, JitPack tag build, inspected CP-G4.9-003)",
      requiredAndroidRuntime="com.github.k2-fsa:sherpa-onnx:v1.12.24 (JitPack, immutable tag)",
      currentRuntimeState="INTEGRATED — SherpaOnnxTtsEngine + SherpaRealNativeBridge (OfflineTts VITS); AudioTrackPlayer playback; TtsRouteSwitch authority",
      authoritativeRuntimeSource="JitPack com.github.k2-fsa:sherpa-onnx:v1.12.24", runtimeVersion="v1.12.24",
      immutableRevision="JitPack tag v1.12.24 (v2.x line exists, un-adopted per pinning policy)",
      license="Apache-2.0", mavenCoordinates="com.github.k2-fsa:sherpa-onnx:v1.12.24",
      supportedAbis=["arm64-v8a", "armeabi-v7a", "x86", "x86_64"], minSdk=21, nativeDependencies="libsherpa-onnx-jni + libonnxruntime (pickFirsts)",
      preprocessing="text → VITS phonemizer (espeak-ng-data inside voice bundle)",
      postprocessing="PCM → AudioTrack MODE_STATIC playback; latency read-back",
      memoryEstimateMB=128, storageEstimateMB=0, loadApi="SherpaRealNativeBridge.createVits(model, tokens, dataDir)",
      inferenceApi="OfflineTts.generate(text, sid, speed)", unloadApi="OfflineTts release via bridge",
      fallback="Android system TTS (TtsRouteSwitch SYSTEM route)", rollback="Route switch back to SYSTEM; engines remain installed",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING (Owner device)"),
 dict(componentId="org.piper.voice.it-riccardo-x-low", displayName="Piper Italian voice — Riccardo (x-low)", capability="VOICE",
      modelFormat="vits-onnx-bundle", quantization="x-low fp32", currentArtifact="vits-piper-it_IT-riccardo-x_low.tar.bz2 (verified 2026-11-02, sha256 8ad07915…7021)",
      requiredAndroidRuntime="sherpa-onnx v1.12.24 (INTEGRATED, see runtime row)",
      currentRuntimeState="RUNTIME AVAILABLE IN CODE — engine wired; capability ladder must advance INSTALLED→RUNTIME_RESOLVED→…→ACTIVE; activation gated by physical qualification",
      authoritativeRuntimeSource="k2-fsa/sherpa-onnx tts-models release (asset pinned by sha256)", runtimeVersion="1.12.24",
      immutableRevision="sherpa-onnx release tag tts-models", license="MIT",
      mavenCoordinates="n/a (model artifact; runtime com.github.k2-fsa:sherpa-onnx)",
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="sherpa-onnx jni",
      preprocessing="espeak-ng phonemization (bundle data)", postprocessing="AudioTrack playback",
      memoryEstimateMB=128, storageEstimateMB=39, loadApi="createVits(modelPath, tokensPath, espeak-ng-data)",
      inferenceApi="generate(text, sid=0, speed)", unloadApi="release",
      fallback="Android system TTS Italian voice", rollback="ModelRollbackController; deactivate → ladder down",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING"),
 dict(componentId="org.piper.voice.it-paola-medium", displayName="Piper Italian voice — Paola (medium)", capability="VOICE",
      modelFormat="vits-onnx-bundle", quantization="medium fp32", currentArtifact="vits-piper-it_IT-paola-medium.tar.bz2 (verified 2026-11-02, sha256 7541f757…16b4)",
      requiredAndroidRuntime="sherpa-onnx v1.12.24 (INTEGRATED)",
      currentRuntimeState="RUNTIME AVAILABLE IN CODE — same ladder as Riccardo; alternative voice (larger, higher first-audio latency)",
      authoritativeRuntimeSource="k2-fsa/sherpa-onnx tts-models release", runtimeVersion="1.12.24",
      immutableRevision="sherpa-onnx release tag tts-models", license="MIT",
      mavenCoordinates="n/a (model artifact)", supportedAbis=COMMON_ABI, minSdk=MIN_SDK,
      nativeDependencies="sherpa-onnx jni", preprocessing="espeak-ng phonemization", postprocessing="AudioTrack playback",
      memoryEstimateMB=256, storageEstimateMB=81, loadApi="createVits(modelPath, tokensPath, espeak-ng-data)",
      inferenceApi="generate(text, sid=0, speed)", unloadApi="release",
      fallback="Android system TTS Italian voice", rollback="ModelRollbackController",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING"),
 dict(componentId="org.gtcrn.speech-enhancement.simple", displayName="GTCRN speech enhancement (denoiser)", capability="NOISE_SUPPRESSION",
      modelFormat="onnx fp32", quantization="fp32", currentArtifact="gtcrn_simple.onnx (verified, 535,638 B, sha256 e77603ac…1b534; bundled asset)",
      requiredAndroidRuntime=ORT, currentRuntimeState="INTEGRATED — GtcrnDenoiserEngine real streaming WOLA STFT→ORT→iSTFT; optional, off by default",
      authoritativeRuntimeSource="Maven Central com.microsoft.onnxruntime", runtimeVersion="1.20.0",
      immutableRevision="sherpa-onnx speech-enhancement-models asset (sha-pinned)", license="MIT",
      mavenCoordinates=ORT, supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="libonnxruntime.so",
      preprocessing="STFT 512/256 WOLA + 3 streaming caches", postprocessing="iSTFT → overlap-add → denoised PCM",
      memoryEstimateMB=32, storageEstimateMB=1, loadApi="OrtOnnxRunner.createSession(path)",
      inferenceApi="runner.run([1,257,1,2] mix + caches)", unloadApi="session close",
      fallback="raw audio passthrough (typed)", rollback="disable denoising setting",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING (A/B on device)"),
 dict(componentId="org.silero.vad.v5", displayName="Silero VAD", capability="VAD",
      modelFormat="onnx fp32", quantization="fp32", currentArtifact="silero_vad.onnx (verified 2026-11-02, 2,327,524 B, sha256 2623a295…d788f)",
      requiredAndroidRuntime=ORT,
      currentRuntimeState="ENGINE GAP REPAIRED IN 4.14 — SileroVadEngine added on the existing ORT runner (state/stateN streaming); EnergyVadEngine stays fallback",
      authoritativeRuntimeSource="Maven Central ORT + snakers4/silero-vad tag v5.1.2", runtimeVersion="1.20.0",
      immutableRevision="tag v5.1.2 (raw.githubusercontent, sha-pinned)", license="MIT",
      mavenCoordinates=ORT, supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="libonnxruntime.so",
      preprocessing="16 kHz mono PCM frames (576-sample windows, v5.1.2 contract)",
      postprocessing="speech probability → hysteresis + hangover → isSpeech",
      memoryEstimateMB=16, storageEstimateMB=3, loadApi="OrtOnnxRunner.createSession(path)",
      inferenceApi="run(input, state, sr) → (prob, stateN)", unloadApi="session close",
      fallback="EnergyVadEngine (RMS, dependency-free)", rollback="VAD selection setting reverts to energy baseline",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING"),
 dict(componentId="org.openwakeword.model.melspectrogram", displayName="openWakeWord melspectrogram (engine dependency)", capability="WAKE_WORD",
      modelFormat="onnx fp32", quantization="fp32", currentArtifact="melspectrogram.onnx (verified, sha256 ba2b0e0f…76f)",
      requiredAndroidRuntime=ORT, currentRuntimeState="INTEGRATED as pipeline dependency (RealMelEmbedFrontend); not user-activatable standalone",
      authoritativeRuntimeSource="openWakeWord release v0.5.1", runtimeVersion="1.20.0",
      immutableRevision="release v0.5.1", license="Apache-2.0", mavenCoordinates=ORT,
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="libonnxruntime.so",
      preprocessing="16 kHz melspec frames", postprocessing="96×96 features → embedding model",
      memoryEstimateMB=40, storageEstimateMB=2, loadApi="OrtOnnxRunner", inferenceApi="run", unloadApi="session close",
      fallback="n/a (dependency)", rollback="wake-word off by default",
      physicalQualification="covered by pipeline qualification"),
 dict(componentId="org.openwakeword.model.embedding", displayName="openWakeWord embedding model (engine dependency)", capability="WAKE_WORD",
      modelFormat="onnx fp32", quantization="fp32", currentArtifact="embedding_model.onnx (verified, sha256 70d16429…c1f)",
      requiredAndroidRuntime=ORT, currentRuntimeState="INTEGRATED as pipeline dependency (RealEmbedHead); not user-activatable standalone",
      authoritativeRuntimeSource="openWakeWord release v0.5.1", runtimeVersion="1.20.0",
      immutableRevision="release v0.5.1", license="Apache-2.0", mavenCoordinates=ORT,
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="libonnxruntime.so",
      preprocessing="melspec features", postprocessing="embedding → head scorer",
      memoryEstimateMB=40, storageEstimateMB=2, loadApi="OrtOnnxRunner", inferenceApi="run", unloadApi="session close",
      fallback="n/a", rollback="wake-word off by default", physicalQualification="covered by pipeline qualification"),
 dict(componentId="org.openwakeword.wakeword.hey-jarvis", displayName="openWakeWord 'Hey Jarvis'", capability="WAKE_WORD",
      modelFormat="onnx fp32", quantization="fp32", currentArtifact="hey_jarvis_v0.1.onnx (verified, sha256 94a13cfe…2cb; bundled)",
      requiredAndroidRuntime=ORT, currentRuntimeState="ENGINE REAL, OFF BY DEFAULT (SEC-04 false-accept policy) — pipeline qualification fixture (EN)",
      authoritativeRuntimeSource="openWakeWord release v0.5.1", runtimeVersion="1.20.0",
      immutableRevision="release v0.5.1", license="Apache-2.0", mavenCoordinates=ORT,
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="libonnxruntime.so",
      preprocessing="melspec→embed chain", postprocessing="threshold+cooldown+duplicate suppression+false-accept counter",
      memoryEstimateMB=40, storageEstimateMB=2, loadApi="OrtOnnxRunner", inferenceApi="run", unloadApi="session close",
      fallback="manual push-to-talk (Interrompi path)", rollback="keep OFF",
      physicalQualification="BLOCKED until false-accept/false-reject testing passes on Owner device"),
 dict(componentId="org.hermestower.wakeword.hermes-it", displayName="Hermes Italian wake phrase (custom model)", capability="WAKE_WORD",
      modelFormat="onnx (openWakeWord-compatible)", quantization="pending", currentArtifact="NONE — no verified source exists upstream",
      requiredAndroidRuntime="openWakeWord ORT chain (exists) + a TRAINED verified model (does not exist)",
      currentRuntimeState="SOURCE NEVER FOUND — download refused (SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED); honest pending entry",
      authoritativeRuntimeSource="none (no verified upstream)", runtimeVersion="pending",
      immutableRevision="none", license="pending (cannot be assumed)", mavenCoordinates="n/a",
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="n/a",
      preprocessing="n/a", postprocessing="n/a", memoryEstimateMB=40, storageEstimateMB=0,
      loadApi="n/a", inferenceApi="n/a", unloadApi="n/a",
      fallback="push-to-talk + continuous listening (physical PASS, preserved)",
      rollback="n/a", physicalQualification="NOT_APPLICABLE (no artifact)"),
 dict(componentId="org.qwen.qwen2.5-0.5b-instruct.q5-k-m", displayName="Qwen2.5 0.5B Instruct (GGUF q5_k_m)", capability="LOCAL_LLM",
      modelFormat="gguf", quantization="q5_k_m",
      currentArtifact="qwen2.5-0.5b-instruct-q5_k_m.gguf (pinned HF commit 9217f5db…, sha256 041474553f…, 522,186,592 B)",
      requiredAndroidRuntime="dev.ffmpegkit-maintained:llama-android:0.1.1 (Maven Central) — llama.cpp b9878 (submodule 2da6686) prebuilt AAR",
      currentRuntimeState="INTEGRATION COMPLETED IN 4.14 — LlamaCppAndroidRuntime binds Llama.loadModel/complete/releaseModel behind LlamaNativeBridge; NoVerifiedLlmRuntime retained only as typed fallback",
      authoritativeRuntimeSource="Maven Central dev.ffmpegkit-maintained:llama-android:0.1.1 (AAR sha256 fdccf04c…7501, inspected in-sandbox)",
      runtimeVersion="0.1.1", immutableRevision="llama.cpp b9878 / submodule 2da6686 / Maven 0.1.1",
      license="MIT (AAR + llama.cpp) / Apache-2.0 (Qwen model)", mavenCoordinates="dev.ffmpegkit-maintained:llama-android:0.1.1",
      supportedAbis=COMMON_ABI, minSdk=24, nativeDependencies="libllama.so + libggml*.so + libc++_shared.so (bundled by AAR)",
      preprocessing="SafePromptBuilder (bounded Italian system context; never Tower truth)",
      postprocessing="bounded completion text; typed diagnostics on failure",
      memoryEstimateMB=1024, storageEstimateMB=522, loadApi="Llama.loadModel(modelPath, LlamaConfig(contextSize, threads))",
      inferenceApi="Llama.complete(model, prompt, systemPrompt, maxTokens) → LlamaResult(text, tokensPerSecond)",
      unloadApi="Llama.releaseModel(model)", fallback="NoVerifiedLlmRuntime typed refusal → route to Tower (TOWER_FIRST/HYBRID)",
      rollback="disable local intelligence; catalog entry removable; model deletion via Remove",
      physicalQualification="PHYSICAL_QUALIFICATION_PENDING (Owner device: load latency, token rate, thermal)"),
 dict(componentId="org.hermestower.test.probe-artifact", displayName="Hermes Test Probe Artifact", capability="WAKE_WORD",
      modelFormat="hermes-probe", quantization="none", currentArtifact="deterministic 4,096 B fixture (sha256 2d717056…527)",
      requiredAndroidRuntime="none (download-pipeline qualification fixture)",
      currentRuntimeState="TEST_FIXTURE_ONLY — never offered as a production component",
      authoritativeRuntimeSource="in-process mock server", runtimeVersion="1.0.0",
      immutableRevision="fixture-v1", license="Apache-2.0", mavenCoordinates="n/a",
      supportedAbis=COMMON_ABI, minSdk=MIN_SDK, nativeDependencies="none",
      preprocessing="n/a", postprocessing="n/a", memoryEstimateMB=1, storageEstimateMB=0,
      loadApi="n/a", inferenceApi="n/a", unloadApi="n/a", fallback="n/a", rollback="n/a",
      physicalQualification="NOT_APPLICABLE (test fixture)"),
]

DECISIONS = {
 "org.vosk.model.small-it-0.22": (D_REUSE, "Runtime integrated and real since Graph 4 FINAL; this graph produces the end-to-end runtime receipt and keeps the ladder authoritative."),
 "org.sherpa-onnx.tts.runtime": (D_REUSE, "JitPack tag build verified CP-G4.9-003; catalog stale CANDIDATE_PENDING_DISCOVERY label corrected to RUNTIME_BUNDLED/INTEGRATED truth."),
 "org.piper.voice.it-riccardo-x-low": (D_REUSE, "Engine + native bridge real; ladder activation path documented; ONE primary action per state; qualification pending on device."),
 "org.piper.voice.it-paola-medium": (D_REUSE, "Same as Riccardo; alternative voice."),
 "org.gtcrn.speech-enhancement.simple": (D_REUSE, "Real streaming ORT inference; optional by default."),
 "org.silero.vad.v5": (D_EXTEND, "NEW SileroVadEngine on the existing ORT runner closes the engine gap (download-exposed-but-unusable FINDING-01 case); EnergyVadEngine stays fallback."),
 "org.openwakeword.model.melspectrogram": (D_REUSE, "Engine dependency of the openWakeWord chain; hidden from ordinary download offers as standalone."),
 "org.openwakeword.model.embedding": (D_REUSE, "Engine dependency; same handling."),
 "org.openwakeword.wakeword.hey-jarvis": (D_LAB, "Real engine exists but stays OFF by default (SEC-04); download offered only in the Advanced Laboratory as the pipeline qualification fixture."),
 "org.hermestower.wakeword.hermes-it": (D_BLOCK, "No verified source exists upstream; BLOCK_UNTIL_RUNTIME_AVAILABLE — honest 'Non ancora usabile' in ordinary UI, download refused; never guessed."),
 "org.qwen.qwen2.5-0.5b-instruct.q5-k-m": (D_ADD, "§9 terminal decision VERIFIED_LOCAL_LLM_RUNTIME_INTEGRATED: llama-android 0.1.1 (Maven Central, MIT, llama.cpp b9878, arm64-v8a, minSdk 24) consumes the EXISTING pinned GGUF — no model replacement required; GGUF stays the compatible artifact."),
 "org.hermestower.test.probe-artifact": (D_REMOVE, "Removed from the ordinary catalog presentation (test fixture must not appear as a downloadable product component); retained in the registry for pipeline qualification evidence."),
}

for r in rows:
    r["decision"] = DECISIONS[r["componentId"]][0]
    r["decisionRationale"] = DECISIONS[r["componentId"]][1]

os.makedirs(OUT, exist_ok=True)
with open(f"{OUT}/ANDROID_RUNTIME_REQUIREMENT_MATRIX.json", "w") as f:
    json.dump({
        "manifestVersion": "graph-4.14-runtime-matrix-1",
        "mission": "GRAPH 4.14 CP-G4.14-002/003",
        "decisionVocabulary": [D_REUSE, D_EXTEND, D_ADD, D_ADAPT, D_REPLACE, D_LAB, D_BLOCK, D_REMOVE],
        "noComponentRemainsDownloadableButUnusable": True,
        "entries": rows,
    }, f, indent=1)

# Markdown summary
cols = ["componentId", "capability", "requiredAndroidRuntime", "runtimeVersion", "decision", "currentRuntimeState"]
md = ["# ANDROID RUNTIME REQUIREMENT MATRIX (Graph 4.14, CP-G4.14-002/003)", "",
      "Machine-readable authority: ANDROID_RUNTIME_REQUIREMENT_MATRIX.json (same directory).",
      "Decision vocabulary = charter §5. Runtime integration order (§6) satisfied:",
      "Vosk IT → sherpa-onnx → Piper Riccardo → Piper Paola → GTCRN → wake-word → optional LLM.",
      "", "| Component | Capability | Required Android runtime | Runtime version | Decision | Runtime state (post-4.14) |",
      "| --- | --- | --- | --- | --- | --- |"]
for r in rows:
    md.append("| {} | {} | {} | {} | **{}** | {} |".format(
        r["componentId"], r["capability"], r["requiredAndroidRuntime"].split(" — ")[0].split(" (")[0],
        r["runtimeVersion"], r["decision"].replace("_", " "), r["currentRuntimeState"][:150]))
md += ["", "## Per-component detail", ""]
for r in rows:
    md += ["### {} — {}".format(r["componentId"], r["displayName"]), "",
           "- Decision: **{}** — {}".format(r["decision"], r["decisionRationale"]),
           "- Model: {} / {} ; artifact: {}".format(r["modelFormat"], r["quantization"], r["currentArtifact"]),
           "- Runtime: {} @ {} (immutable revision: {})".format(r["requiredAndroidRuntime"], r["runtimeVersion"], r["immutableRevision"]),
           "- Coordinates: {} ; ABIs: {} ; minSdk {}".format(r["mavenCoordinates"], ", ".join(r["supportedAbis"]), r["minSdk"]),
           "- License: {} (no license assumption; sources read this mission)".format(r["license"]),
           "- Native deps: {}".format(r["nativeDependencies"]),
           "- Pre/post: {} → {}".format(r["preprocessing"], r["postprocessing"]),
           "- Memory ≈{} MB ; storage ≈{} MB".format(r["memoryEstimateMB"], r["storageEstimateMB"]),
           "- Load: {} ; Inference: {} ; Unload: {}".format(r["loadApi"], r["inferenceApi"], r["unloadApi"]),
           "- Fallback: {} ; Rollback: {}".format(r["fallback"], r["rollback"]),
           "- Physical qualification: {}".format(r["physicalQualification"]), ""]
with open(f"{OUT}/ANDROID_RUNTIME_REQUIREMENT_MATRIX.md", "w") as f:
    f.write("\n".join(md))
print("JSON entries:", len(rows))
print("MD lines:", len(md))
