# TOWER CONNECTION PROFILE SCHEMA — V0.2

Typed profile: `tower/TowerConnectionProfile.kt` (kotlinx-serialization). Secrets are NOT
fields of this class — they live only in the SecretVault keyed by profileId.

| Field | Type / default | Notes |
| --- | --- | --- |
| profileId | string | stable id; vault key namespace |
| profileName | string | required, non-blank |
| transportMode | enum name | HTTPS · SECURE_WEBSOCKET · HTTPS_PLUS_SECURE_WEBSOCKET · TAILSCALE_PRIVATE · MOCK_SIMULATOR |
| baseUrl | string | scheme-validated (https/wss; http allowed only for Tailscale); host required; optional port 1–65535; no whitespace |
| restBasePath / webSocketPath | "/tower/api" / "/tower/ws" | must start with "/" |
| protocolVersion | "hermes.console.v0.1" | required |
| userReference / deviceAlias | string | Owner-facing labels |
| preferredLanguage | "it-IT" | |
| requestedMode | "TOWER_FIRST" | default operating mode |
| connectAutomatically / useTailscale | bool | |
| requestTimeoutMs | 10000 | validated 1_000..120_000 |
| streamingTimeoutMs | 60000 | validated 5_000..600_000 |
| maximumRetryCount | 2 | validated 0..5 |
| queuedRequestPolicy | QUEUE_NON_CONSEQUENTIAL | consequential requests are never silently queued |
| consequentialConfirmationRequired | true | |
| certificatePolicy | SYSTEM | or PINNED |
| certificateFingerprintReference | "" | required 64-hex when PINNED |
| diagnosticsVerbosity | REDACTED_SUMMARY | |
| localTowerRoutingThreshold | 0.55 | mirrors the router's low-confidence threshold |
| createdAt / updatedAt | ISO-8601 | |
| lastValidationAt / lastConnectionAt / lastConnectionResult | ISO-8601 / enum name | truthful read-back |

## Configuration states (ConnectionConfigState)

EMPTY · DRAFT · INVALID · VALIDATED · PAIRING_REQUIRED · PAIRED · CONNECTION_TEST_PENDING ·
CONNECTED · DISCONNECTED · DEGRADED · AUTHENTICATION_FAILED · PROTOCOL_MISMATCH ·
CERTIFICATE_ERROR · TIMEOUT · ROLLBACK_AVAILABLE.

## Validation (TowerProfileValidator — deterministic, unit-pinned)

Scheme/host/port/path/whitespace rules; protocol version presence; timeout and retry
ranges; token required exactly when the method needs a server; fingerprint syntax when
PINNED; Mock mode bypasses production authentication requirements; Tailscale mode accepts
only `*.ts.net` or `100.x.y.z` addresses. VALIDATION_PASSED never implies CONNECTED.

## Export format

`ExportedProfile{ exportedFormatVersion: "hermes.connection.profile.v1", profile,
pairingSecret: "PAIRING_SECRET_REQUIRED", accessToken: "ACCESS_TOKEN_REQUIRED" }` —
placeholder fields are REQUIRED so any JSON writer always serializes them. Import refuses
any file that carries a non-placeholder secret, and never silently overwrites an active
configuration (Conflict → explicit confirm → previous configuration preserved for restore).
