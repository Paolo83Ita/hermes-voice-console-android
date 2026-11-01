# PAIRING AND SECURE STORAGE — V0.2

## Threat model (short)

Protect pairing codes / access tokens at rest on a single-user device. The device itself
is trusted hardware; the threats are plaintext leakage via settings files, backups,
logs, exports, diagnostics and crash reports.

## Implementation

- **Vault**: `tower/KeystoreVault` — AES-256-GCM with a key generated inside
  **AndroidKeyStore** (`hermes_tower_vault_key`, 256-bit, GCM, no padding, randomized
  encryption required). The key is non-exportable; encryption/decryption happen inside
  the keystore.
- **Ciphertext store**: `PrivateFileVaultStore` — `filesDir/tower_vault.bin`, one record
  per line `<key>=<base64(iv)>:<base64(ciphertext)>`, written via temp-file + rename.
  Fresh IV per put; plaintext never touches disk.
- **Metadata separation**: `tower_profile.json` holds the profile only. Tests pin that
  the metadata file never contains the secret and that exports never contain it.
- **Emulator/device evidence**: `KeystoreVaultInstrumentedTest` — round-trip, no-plaintext
  on disk, reopen survival, truthful availability report (GATE-04).
- **JVM/tests**: `InMemorySecretVault` — the interface is platform-free; no Android
  dependency in unit tests.

## Pairing flow

1. Owner enters the pairing code or token in the Studio (password field).
2. "Save and connect" / "Pair device" stores it via `setSecret` / `setPairingCode`
   (vault-encrypted), keyed `tower/<profileId>/secret` | `/pairing` (VaultKeys).
3. Requests attach `Authorization: Bearer …` only in memory at call time.
4. "Clear sensitive values" removes both vault entries; profile metadata is preserved.
5. Export writes placeholders (`ACCESS_TOKEN_REQUIRED`, `PAIRING_SECRET_REQUIRED`);
   import refuses real secrets outright and never overwrites an active configuration
   without explicit confirmation.

## Prohibited stores (enforced by design + tests)

plain SharedPreferences · exported JSON · logs (LogRedactor) · screenshots · analytics ·
repository files · crash reports (allowBackup=false).
