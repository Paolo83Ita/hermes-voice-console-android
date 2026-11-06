# TOWER_ARCHIVE_PRODUCT_COMPLETION_REPORT.md

- Mission: GRAPH 4.9 — CP-G4.9-007 (Tower Archive product completion)
- Owner: Paolo Lombisani
- Authority: remote tip `eb660d5` (fresh read-back) + this node's implementation
- Written: 2026-11-07

## 1. Scope and authority

The Tower Archive stays a CLIENT with DETERMINISTIC MOCK data until Tower
Graph 1.7 owns the real server-side routes. Every row in the mock is seeded
deterministically (same rows every run), the UI labels the state SIMULATED,
no production endpoint is invented and no recovered historical data is
claimed. This report maps the briefing §10 feature table to the shipped
implementation at this node.

## 2. Feature matrix (briefing §10 → implementation)

### CONVERSATIONS — `ArchiveRepositories.ConversationRepository`, `MockTowerArchive`

| Feature | State | Evidence |
| --- | --- | --- |
| list | DONE (mock) | `listConversations(page,pageSize,…)` deterministic pages |
| pagination | DONE | `ArchivePage(items, nextCursor)`; cursor null = end |
| search | DONE | case-insensitive title/id match |
| filters | DONE | source (LOCAL/TOWER), projectId, missionId, from/to date |
| transcript | DONE | deterministic 4-line transcript per conversation |
| local cache | DONE | `cachedConversationIds()`, `removeLocalCache()` — LOCAL rows only, remote untouched (§15) |
| offline pin | DONE | `pinOffline(id, pinned)` + `pinnedConversationIds()` |
| continuation eligibility | DONE | mission link + mission's `continuationEligible` |
| related mission | DONE | `ArchiveConversation.missionId` + `locateMission` |
| related artifacts | DONE | artifact rows reference conversation ids; `locateConversation` |

### MISSIONS — `MissionRepository`

| Feature | State | Evidence |
| --- | --- | --- |
| ID/title/graph/phase/state | DONE | `ArchiveMission` full field set |
| checkpoint | DONE | `checkpoint` (deterministic CP labels) |
| blockers | DONE | `blockers` + `ownerAction` |
| receipts | DONE | `receiptIds` |
| continuation eligibility | DONE | `continuationEligible` field |
| continuation request | DONE | `requestContinuation` → typed `SimulatedAccepted`/`Refused` — the REAL Tower stays the authority (§16; cached phone state is never sufficient) |

### ARTIFACTS — `ArtifactRepository` + `ArtifactDownloadManager` (extended this node)

| Feature | State | Evidence |
| --- | --- | --- |
| metadata | DONE | id/name/kind/size/sha256/localState |
| MIME type | **ADDED (CP-G4.9-007)** | honest per-kind MIME map (`mimeFor`) |
| size | DONE | bytes from the deterministic seed |
| SHA-256 | DONE | deterministic seed hash; `verifyHash()` typed |
| provenance | **ADDED** | per-row SIMULATED provenance line + conversation/mission locate |
| preview | **ADDED** | `previewText` for text-bearing kinds, always visibly SIMULATED; binary kinds expose NO fabricated preview |
| download | DONE | `download()` → typed `Simulated` (no bytes move) |
| pause | **ADDED** | typed `ArtifactTransferState`; refused unless DOWNLOADING |
| resume | **ADDED** | PAUSED → simulated completion; refused otherwise |
| verify | DONE | `verifyHash(artifactId)` |
| open | TYPED-BLOCKED | no real bytes on device in the mock; enabling open/share would fabricate content — honest refusal (SIMULATED state shown instead) |
| share | TYPED-BLOCKED | same honesty rule as open |
| pin offline | DONE | conversation-level pin (§15) |
| remove local copy | DONE | `removeLocalCopy` → METADATA_ONLY, remote untouched, transfer state reset |
| re-download | **ADDED** | `reDownload` = removeLocalCopy + download (fresh SIMULATED cycle) |

## 3. UI surface (`ui/screens/ArchiveScreen.kt`)

- Tabs Conversations / Missions / Artifacts.
- Artifacts: metadata + MIME + transfer state + provenance + preview +
  Download / Pause / Resume / Re-download / Remove local copy / Verify hash —
  all physically clickable, all operating on the typed mock lifecycle.
- The SIMULATED label remains mandatory on the screen (no claim of real
  recovered data).

## 4. Tests

`MockTowerArchiveTest` extended this node (335/335 unit suite GREEN):
- `artifacts carry mime type provenance and deterministic text previews` —
  JSON→application/json + preview present; PDF→application/pdf + NO preview;
- `transfer lifecycle pauses resumes re-downloads with typed states` —
  IDLE → download (Simulated) → re-download → cancel → IDLE → resume refused →
  remove-local → METADATA_ONLY + IDLE.

## 5. What this does NOT claim

- No real Tower routes, no real recovery of historical data, no real bytes.
- The client seam (`TowerArchiveClient`) is the contract boundary where the
  Tower Graph 1.7 implementation will be injected; see
  ANDROID_GRAPH4_TOWER_GRAPH1_7_COMPATIBILITY_MATRIX.md.
