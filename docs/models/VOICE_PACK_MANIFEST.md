# VOICE PACK MANIFEST — schema and shipped catalog

Manifest file: `app/src/main/assets/models/voice_pack_catalog.json` — root object:

```json
{
  "manifestVersion": "1.0.0",
  "generatedFor": "hermes-voice-console-android-0.2.0-debug",
  "entries": [ ModelCatalogEntry, ... ]
}
```

`ModelCatalogEntry` (kotlinx-serialization, `models/ModelCatalogEntry.kt`):

| Field | Type | Meaning |
| --- | --- | --- |
| componentType | enum name | WAKE_WORD · VAD · NOISE_SUPPRESSION · SPEECH_TO_TEXT · LOCAL_LLM · TTS_ENGINE · VOICE |
| identity | string | exact component identity (reverse-DNS) |
| displayName / version | string | user-visible name and exact version |
| sourceUri | string | where the artifact would come from |
| provider | string | repository or provider |
| license / licenseApproved | string / bool | license id and review state |
| format / quantization | string | artifact format and quantization |
| supportedAbis | string[] | ABI compatibility |
| androidMinVersion | int | minimum Android API |
| downloadSizeBytes / installedSizeBytes | long | sizes shown before any download |
| sha256 | string | lowercase hex; all-zero ⇒ no verified binary published (never downloadable) |
| manifestVersion | string | manifest schema of the entry |
| storageLocation | string | filesDir-relative destination |
| ramEstimateBytes | long | RAM estimate used by gates |
| latencyClass | string | NEGLIGIBLE · REALTIME · SLOW |
| languages / italianSupported | string[] / bool | language support |
| knownLimitations | string[] | honest limitations |
| state / active / previousVersion / rollbackAvailable / lastVerificationAt / lastQualificationAt | lifecycle | maintained by the installation registry |

## Verification rules (DefaultModelManifestVerifier)

identity/version/sourceUri/license/manifestVersion non-blank; sizes > 0; sha256 must match
`^[0-9a-f]{64}$`; ABIs non-empty; androidMinVersion > 0. Entries failing any rule never
enter the download-approval flow.

## Shipped entries

Probe artifact (downloadable, deterministic test fixture) · openWakeWord Noah · Silero VAD ·
whisper.cpp tiny · Gemma-2B IT (license review pending) · Piper it_IT riccardo · RNNoise ·
Piper voice. The six real candidates are DISCOVERED/LICENSE_PENDING — they become
downloadable only after their real binaries and hashes are published in a future manifest.

## Secret-clean rule

Manifests carry no secrets and no user data. The manifest is the ONLY place a component's
expected SHA-256 lives; the download manager refuses any artifact whose hash differs.
