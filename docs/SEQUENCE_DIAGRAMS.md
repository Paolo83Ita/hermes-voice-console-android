# SEQUENCE DIAGRAMS (Mermaid) — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Note | V0.1 simulates STT/wake with text injection; the sequence structure is the one the real engines will drive from V0.3. |

## 1. LOCAL — local-only resolution

```mermaid
sequenceDiagram
    autonumber
    actor O as Owner
    participant C as Console (driver)
    participant R as IntentRouter
    participant L as LocalConversationEngine
    participant T as TextToSpeechEngine
    participant S as ConsoleStateStore

    O->>C: utterance (V0.1: injected text)
    C->>S: TRANSCRIBING (final transcript + confidence)
    C->>R: route(text, mode=LOCAL, connectivity=OK)
    R-->>C: HANDLE_LOCAL (intent, reason)
    C->>S: UNDERSTANDING_LOCAL
    C->>L: respond(text)
    L-->>C: local response (canned in V0.1)
    C->>T: speak(response)
    C->>S: SPEAKING (captions on, amplitude)
    T-->>O: spoken audio (LOCAL-labeled)
    O->>C: barge-in (optional)
    C->>T: interrupt
    C->>S: IDLE
```

## 2. TOWER — full forwarding with streaming

```mermaid
sequenceDiagram
    autonumber
    actor O as Owner
    participant C as Console (driver)
    participant R as IntentRouter
    participant B as TowerBridge (mock)
    participant S as ConsoleStateStore
    participant T as TextToSpeechEngine

    O->>C: utterance (mode=TOWER)
    C->>S: TRANSCRIBING
    C->>R: route(text, mode=TOWER)
    R-->>C: ROUTE_TOWER
    C->>S: CONTACTING_TOWER
    C->>B: sendRequest(REQUEST frame)
    B-->>S: ACK -> TOWER_PROCESSING
    B-->>S: PROGRESS events
    B-->>C: DELTA stream
    C->>S: captions buffer (streaming text)
    B-->>C: FINAL(finalText, receiptRef, state=COMPLETED)
    C->>T: speak(finalText)
    C->>S: SPEAKING
    T-->>O: spoken audio (TOWER-labeled)
    C->>S: IDLE (turn DELIVERED, receipt ref kept)
    O->>C: stop / barge-in at any point -> cancel(requestId)
```

## 3. HYBRID — router decides per turn

```mermaid
sequenceDiagram
    autonumber
    actor O as Owner
    participant C as Console (driver)
    participant R as IntentRouter
    participant L as LocalConversationEngine
    participant B as TowerBridge (mock)
    participant T as TextToSpeechEngine

    O->>C: "good day" (simple)
    C->>R: route(mode=HYBRID, confidence=0.93)
    R-->>C: HANDLE_LOCAL (whitelist: greeting)
    C->>L: respond
    C->>T: speak local answer (LOCAL-labeled)
    O->>C: "prepare tomorrow mission plan" (consequential)
    C->>R: route(mode=HYBRID, confidence=0.9)
    R-->>C: ROUTE_TOWER (consequential pattern: mission)
    C->>B: REQUEST (localRouting hint attached)
    B-->>C: ACK / PROGRESS / DELTA / FINAL
    C->>T: speak Tower answer (TOWER-labeled)
```

## 4. DISCONNECTED_SAFE — disclosure, queue, flush

```mermaid
sequenceDiagram
    autonumber
    actor O as Owner
    participant C as Console (driver)
    participant B as TowerBridge (mock)
    participant R as IntentRouter
    participant S as ConsoleStateStore
    participant T as TextToSpeechEngine

    Note over B: fault injection: DISCONNECT_AT_ACK
    O->>C: "status summary" (mode=TOWER)
    C->>S: CONTACTING_TOWER
    C->>B: REQUEST
    B-->>C: ERROR(classification=TRANSPORT, retryable=true)
    C->>S: DISCONNECTED (effective mode -> DISCONNECTED_SAFE)
    C->>R: route(same text, effectiveMode=DISCONNECTED_SAFE)
    R-->>C: QUEUE_FOR_TOWER (non-consequential)
    C->>S: REQUEST_QUEUED (queue count +1)
    C->>S: IDLE (queued badge visible)
    O->>C: "local help" while offline
    C->>R: route -> HANDLE_LOCAL
    C->>T: speak safe local answer
    Note over B: fault cleared, connection restored
    B-->>C: connection CONNECTED
    C->>S: DISCONNECTED -> REQUEST_QUEUED -> CONTACTING_TOWER (flush)
    C->>B: queued REQUEST
    B-->>C: ACK/PROGRESS/DELTA/FINAL
    C->>S: turn DELIVERED, IDLE
```

## 5. Failure and cancellation overlay (applies to all modes)

```mermaid
sequenceDiagram
    autonumber
    actor O as Owner
    participant C as Console (driver)
    participant B as TowerBridge
    participant S as ConsoleStateStore

    O->>C: privacy mute during any active state
    C->>B: cancel(requestId)
    C->>S: PRIVACY_MUTED (pipeline job cancelled, TTS interrupted)
    O->>C: unmute
    C->>S: IDLE
    B-->>C: ERROR(TOWER_INTERNAL, retryable=false)
    C->>S: ERROR (turn FAILED with reason - retry disabled for consequential)
    O->>C: retry (allowed only if non-consequential + transport failure)
    C->>S: REQUEST_QUEUED -> CONTACTING_TOWER
```
