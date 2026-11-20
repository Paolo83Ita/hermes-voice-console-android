# A4R NODE A2 — Candidate B AndroidKeyStore Repair (drop-in package)

**Finding repaired:** A4R-P1-001 ANDROID_KEYSTORE_PAIRING_CRASH (physically confirmed
on the Owner's Xiaomi 11 Lite 5G NE, MIUI feedback screenshots 2026-11-20 09:50) and
A4R-P1-002 JVM_TEST_FAILED_TO_REPRESENT_ANDROID_KEYSTORE.

**Target file:** `apps/android-voice-console/app/src/main/java/com/hermestower/voiceconsole/data/TowerStore.kt`
in `Paolo83Ita/Hermes-Tower` (delivered here because the canonical Tower repository is
not writable from the Android executor account — Tower access mode is READ_ONLY except
for the Owner-approved handoff contribution mechanism).

## Root cause (physically proven)

`TowerStore$Keystore.key()` line 35 called:

```kotlin
kg.init(256)   // KeyGenerator on the "AndroidKeyStore" provider, NO spec
```

The AndroidKeyStore KeyGenerator requires a `KeyGenParameterSpec`; without one it
throws on every real device:

```
java.lang.UnsupportedOperationException: Cannot initialize without a
android.security.keystore.KeyGenParameterSpec parameter
  at android.security.keystore2.AndroidKeyStoreKeyGeneratorSpi.engineInit(...:163)
  at javax.crypto.KeyGenerator.init(...:582) / init(...:566)
  at com.hermestower.voiceconsole.data.TowerStore$Keystore.key(TowerStore.kt:35)
  at ...TowerStore.encrypt(42) -> putEncrypted(59) -> setEndpoint(75)
  at ...PairingFlow.pairFromQr(54) -> PairingScreen.kt:148
```

The crash fired BEFORE any network call (setEndpoint is the first pairing write), so
every QR pairing attempt died. The JVM test suite could not catch it because the JVM
has no AndroidKeyStore provider — the 17 client tests never executed this line
(A4R-P1-002). Note the asymmetry: `DeviceKeys.kt` (the EC-P256 device identity) uses
the correct spec API; only the AES storage key path was wrong.

## What to integrate (Tower account)

1. **Replace** `TowerStore.kt` with the repaired file in this package (public API is
   1:1 compatible: constructor, endpoint/deviceId/consoleToken/deviceLabel/
   pairingFingerprint accessors, `isPaired()`, `clear()`, `nextTurnSeq()`. New
   members are additive: `lastFailure`, `failureHistory()`, `commitPairing(...)`,
   optional test-only `encryptInterceptor` constructor param defaulting to null).
2. **Add** `PairingPersistence.kt` beside it (`KeystoreFailure`,
   `TowerStoreKeystoreException`, `BlobFraming`).
3. **Add** `BlobFramingTest.kt` to the JVM unit suite (pure byte framing truth).
4. **Add** `TowerStoreInstrumentedTest.kt` to `app/src/androidTest/java/...`. If the
   module does not yet declare androidTest dependencies, add:
   `androidTestImplementation "androidx.test.ext:junit:1.2.1"` and
   `androidTestImplementation "androidx.test:runner:1.6.2"` (or the versions already
   pinned by the Tower project), plus `testInstrumentationRunner
   "androidx.test.runner.AndroidJUnitRunner"` in the module build.
5. **Apply** the small PairingFlow delta described in `PATCH_NOTES_PairingFlow.md`
   (map typed store failures to the pairing Step machine; no client-side bypass).

## Mission contract compliance (section 5)

| Requirement | Where |
|---|---|
| AES, GCM, NoPadding | `TRANSFORMATION = "AES/GCM/NoPadding"` |
| explicit KeyGenParameterSpec | `keystoreKey()` generation branch |
| 256-bit key where supported | `.setKeySize(256)` |
| randomized encryption required | `.setRandomizedEncryptionRequired(true)` |
| fresh IV per encryption | IV read from cipher after doFinal (keystore randomized IV) |
| IV persisted with ciphertext | `BlobFraming` layout `[len][iv][ct]` |
| authentication tag verified | decrypt catches AEADBadTagException → typed CORRUPT |
| safe alias handling | missing alias → generate; key-loss class → typed wipe + regenerate |
| invalidated-key handling | `ANDROID_KEY_INVALIDATED` (deterministic: fresh key + old records) |
| corrupted-record handling | `ENCRYPTED_STORE_CORRUPT` + single-record wipe |
| no fallback to plaintext | every failure path leaves records absent/wiped, never decrypted-plain |
| no endpoint persistence after encryption failure | setters write only on success |
| typed states (5) | `KeystoreFailure` enum, all five present |
| never catch and ignore | every catch records a typed failure state first |
| instrumentation test with real keystore | `TowerStoreInstrumentedTest` (9 tests) |
| first-install / update / uninstall-reinstall / key-invalidated / corrupted / QR / numeric / no-partial-state tests | instrumented suite methods, one per mission item |

## Verification gates after integration (Tower account)

1. `./gradlew :app:testDebugUnitTest` — the 17 existing client tests + BlobFramingTest.
2. `./gradlew :app:connectedDebugAndroidTest` — the 9 instrumented tests on an
   emulator AND on the Xiaomi 11 Lite 5G NE.
3. Rebuild the APK (keep the SAME debug keystore for update continuity), register the
   new SHA-256 in the fingerprint matrix, deliver via the handoff mechanism.
4. The Owner repeats ONLY the failed physical step (QR pairing) — the failed APK
   remains recorded in the ledger with finding + repair + retest columns.
