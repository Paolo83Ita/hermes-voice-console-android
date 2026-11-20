# PATCH NOTES — PairingFlow / PairingScreen (typed failure mapping)

Scope: `apps/android-voice-console/app/src/main/java/com/hermestower/voiceconsole/pairing/PairingFlow.kt`
and `ui/PairingScreen.kt` in the canonical Tower repository. These notes describe the
SMALL delta required so the pairing UI consumes the repaired TowerStore's typed
failure states. They do NOT rewrite the pairing protocol (claim/attest/ack/PoP stay
exactly as they are — that lane is the live Tower contract).

## 1. Why the delta exists

With the repaired TowerStore, `setEndpoint` no longer throws on keystore failure —
it records a typed failure and writes nothing. The OLD PairingFlow has no failure
mapping around persistence, so a failed commit would previously continue silently
into claim/attest with an unpaired store. The mission requires:

- `PAIRING_PERSISTENCE_FAILED` — surfaced, not swallowed;
- `PAIRING_ABORTED_NO_STATE_COMMITTED` — pairing aborts BEFORE any server call
  when local state cannot be committed;
- no client-side bypass (the flow never "pairs" without the Tower).

## 2. PairingFlow delta (pairFromQr and pairFromCode — same shape for both lanes)

```kotlin
// BEFORE (original, PairingFlow.kt:54 area):
suspend fun pairFromQr(endpoint: String, pairingId: String, numericCode: String, label: String): Step {
    store.setEndpoint(endpoint)
    return claimAndAttest(pairingId, numericCode, label)
}

// AFTER (repair):
suspend fun pairFromQr(endpoint: String, pairingId: String, numericCode: String, label: String): Step {
    if (!store.commitPairingPlaceholderForEndpoint(endpoint)) {  // see note
        return Step.Failed("PAIRING_PERSISTENCE_FAILED", store.lastFailure?.name ?: "UNKNOWN")
    }
    return claimAndAttest(pairingId, numericCode, label)
}
```

NOTE — keep it minimal and signature-stable: the repaired TowerStore exposes
`commitPairing(endpoint, deviceId, consoleToken, deviceLabel)`. The pairing flow only
knows the endpoint at this stage (deviceId/token arrive from the claim/attest
responses), so the correct minimal integration is:

```kotlin
suspend fun pairFromQr(endpoint: String, pairingId: String, numericCode: String, label: String): Step {
    // Encrypt-and-write the endpoint alone: on ANY typed failure the pairing
    // aborts with NOTHING committed (no endpoint persistence after encryption
    // failure - mission section 5).
    store.setEndpoint(endpoint)
    if (store.lastFailure != null) {
        return Step.Failed(
            kind = "PAIRING_PERSISTENCE_FAILED",
            detail = "endpoint persistence failed: ${store.lastFailure?.name}; no state committed",
        )
    }
    return claimAndAttest(pairingId, numericCode, label)
}
```

And after the claim/attest responses deliver `deviceId`/`consoleToken`, replace the
sequence of individual setters with the ATOMIC commit:

```kotlin
// BEFORE:
store.setDeviceId(deviceId)
store.setConsoleToken(consoleToken)
store.setDeviceLabel(label)

// AFTER (single atomic editor commit - all-or-nothing):
if (!store.commitPairing(endpoint, deviceId, consoleToken, label)) {
    return Step.Failed(
        kind = "PAIRING_PERSISTENCE_FAILED",
        detail = "pairing state commit failed: ${store.lastFailure?.name}",
    )
}
```

(`Step.Failed` already exists in the Step machine — keep its exact signature; the
kind/detail strings above are the typed contract the UI renders.)

## 3. PairingScreen delta (typed user-visible states)

Map the store failure states to the screen's state card so the Owner sees a typed
message INSTEAD of a crash (mission section 6 typed states):

| Store failure | Screen state to render |
|---|---|
| `ANDROID_KEYSTORE_INITIALIZATION_FAILED` | `PAIRING_PERSISTENCE_FAILED` card + "riprova" action |
| `ANDROID_KEY_INVALIDATED` | `PAIRING_PERSISTENCE_FAILED` card + "stato precedente eliminato, ripeti il pairing" |
| `ENCRYPTED_STORE_CORRUPT` | `PAIRING_PERSISTENCE_FAILED` card + "record corrotto eliminato" |
| `PAIRING_ABORTED_NO_STATE_COMMITTED` | `PAIRING_ABORTED_NO_STATE_COMMITTED` card + "nessuno stato scritto, ripeti" |

Rule: the screen NEVER shows raw exception text and NEVER shows secrets — the
failure enum name and a redacted detail string only.

## 4. Explicitly NOT changed

- claim/attest/ack/PoP protocol and endpoints (live Tower contract — untouched);
- QR v2 payload parser (untouched);
- DeviceKeys EC-P256 identity (already correct);
- any Tower server code (out of scope for the Android executor — §2 boundary).
