# MODEL DOWNLOAD AND ROLLBACK — behavior contract

## Download lifecycle (states enforced by the installation registry)

```
DISCOVERED → DOWNLOAD_APPROVAL_REQUIRED → DOWNLOADING ⇄ DOWNLOAD_PAUSED
           → DOWNLOADED → HASH_VERIFICATION_PENDING → HASH_VERIFIED
           → INSTALLING → INSTALLED → QUALIFICATION_PENDING → QUALIFIED
           → ACTIVE      (or ROLLBACK_AVAILABLE → REMOVED)
```

Failure paths end in FAILED (typed, with the reason in the registry record and the UI).

## Guarantees

1. **No silent download.** Nothing downloads at first launch. The recommendation flow
   displays every component (size, installed size, RAM estimate, source, license,
   qualification state) and requires explicit confirmation before any download begins.
2. **Storage preflight** — `free ≥ downloadSize + 256 MB reserve`, checked before the
   first byte; failure is typed, never a partial download.
3. **Resumable** — partial files live in `filesDir/models/tmp/<identity>.part`; resume
   issues `Range: bytes=<size>-`; the server's 206 answer is appended atomically.
4. **Pause/cancel are quiescent** — pause and cancel join the writer (bounded 3 s) before
   the partial is read or deleted, so a resume never appends over moving bytes.
5. **No partial activation** — only the temp file exists until hash verification passes;
   installation is a single ATOMIC_MOVE into `filesDir/models/<location>/`.
6. **No duplicate download** — if an installed file already hashes to the manifest value,
   the download is skipped entirely (`verifiedExisting`).
7. **Cleanup** — failed or cancelled temp files are deleted; the partial is deleted
   immediately on hash mismatch.
8. **Truthful progress** — byte counts and a fraction derived from real HTTP content
   length; no fabricated speed or ETA.

## Hash verification

`ModelSha256` streams SHA-256 over the file (64 KB buffer). Verification compares against
the manifest `sha256`; mismatch ⇒ file deleted, state FAILED, typed evidence
(`Hash mismatch: expected…actual…`).

## Rollback

- `snapshotFor` copies the current installed file to `<componentDir>/<identity>.pkg.prev`
  (identity-based, so it survives version changes) and marks `ROLLBACK_AVAILABLE`.
- `rollback` re-hashes the backup against `previousSha256` before restoring — a tampered
  backup is refused. The restored version is re-marked QUALIFIED; rollback slot cleared.
- `removalCleanup` deletes the installed file, the backup and the registry record.

## Activation

Activation runs the qualification runner first (presence, size, hash, manifest
completeness); only QUALIFIED components can become ACTIVE. `readBack()` returns the
active configuration (identity, type, version, sha256, activatedAt) — this is the
read-back the Self-Test Center and the Model Manager UI display.

## Real large models

Never bundled, never downloaded by Graph 2. Physical-device qualification plan:
`XIAOMI_MODEL_QUALIFICATION_PLAN.md`.
