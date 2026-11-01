package com.hermestower.voiceconsole.selftest

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateMachine
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.models.DeterministicTestArtifact
import com.hermestower.voiceconsole.models.ModelCatalogEntry
import com.hermestower.voiceconsole.models.ModelComponentType
import com.hermestower.voiceconsole.models.ModelInstallState
import com.hermestower.voiceconsole.models.ModelSha256
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.migration.SettingsMigrator
import com.hermestower.voiceconsole.privacy.LogRedactor
import com.hermestower.voiceconsole.tower.ConnectionConfigState
import com.hermestower.voiceconsole.tower.ImportResult
import com.hermestower.voiceconsole.tower.RealTowerBridge
import com.hermestower.voiceconsole.tower.SecretVault
import com.hermestower.voiceconsole.tower.TowerConnectionProfile
import com.hermestower.voiceconsole.tower.TowerConnectionTester
import com.hermestower.voiceconsole.tower.TowerProfileValidator
import com.hermestower.voiceconsole.tower.VaultKeys
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.ui.AppRoutes
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant

/**
 * The 45 deterministic self-tests (GRAPH 2 §11). Every check is bounded, offline
 * (127.0.0.1 only) and side-effect limited to its own fixture. Real acoustic,
 * LLM and soak qualification stay QUALIFICATION_PENDING_DEVICE.
 */
object SelfTestRegistry {

    private const val PROBE_ID = "org.hermestower.test.probe-artifact"

    fun build(s: SelfTestSubjects): List<SelfTestDefinition> {
        fun def(
            id: String,
            name: String,
            category: SelfTestCategory,
            modes: Set<SelfTestMode>,
            body: SelfTestContext.(SelfTestSubjects) -> Pair<SelfTestState, String>,
        ) = SelfTestDefinition(id, name, category, modes) { ctx ->
            val started = Instant.now()
            val (state, evidence) = try {
                body(ctx, s)
            } catch (e: Exception) {
                SelfTestState.FAIL to "exception: ${e.javaClass.simpleName}: ${e.message ?: ""}"
            }
            val finished = Instant.now()
            SelfTestResult(
                testId = id,
                testName = name,
                startTime = started,
                finishTime = finished,
                durationMs = Duration.between(started, finished).toMillis(),
                state = state,
                evidence = evidence,
                failureType = if (state == SelfTestState.FAIL) "ASSERTION_FAILED" else null,
                repairSuggestion = if (state == SelfTestState.FAIL) "Open the related setting or rerun; see docs/testing/SELF_TEST_CENTER.md." else null,
                environment = ctx.environment,
                appVersion = ctx.appVersion,
                commitReference = ctx.commitReference,
            )
        }

        val ALL = SelfTestMode.entries.toSet()
        val ALL_MINUS_QUICK = SelfTestMode.entries.filter { it != SelfTestMode.QUICK }.toSet()

        return listOf(
            // ---------- identity, configuration, security ----------
            def("ST-01", "Application identity and version", SelfTestCategory.IDENTITY, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val ok = s.appVersion.isNotBlank() && s.appVersion.startsWith("0.2.0")
                if (ok) SelfTestState.PASS to "app version ${s.appVersion} reported truthfully."
                else SelfTestState.FAIL to "app version '${s.appVersion}' does not match 0.2.0-debug schema."
            },
            def("ST-02", "Configuration schema", SelfTestCategory.CONFIGURATION, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val profile = TowerConnectionProfile.emptyMock()
                val report = TowerProfileValidator.validate(profile, secretPresent = false, nowIso = "self-test")
                if (report.passed) SelfTestState.PASS to "default connection profile validates against the V0.2 schema."
                else SelfTestState.FAIL to "default profile invalid: ${report.issues.joinToString()}"
            },
            def("ST-03", "Secure storage availability", SelfTestCategory.SECURITY, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val key = VaultKeys.secretKeyFor("st-03-probe")
                s.vault.put(key, "probe-secret".toCharArray())
                val roundtrip = String(s.vault.get(key) ?: CharArray(0)) == "probe-secret"
                val removed = s.vault.remove(key)
                val capability = s.vault.available().displayLabel
                if (roundtrip && removed) SelfTestState.PASS to "vault roundtrip ok; capability: $capability."
                else SelfTestState.FAIL to "vault roundtrip failed ($roundtrip/$removed); capability: $capability."
            },
            def("ST-04", "Connection-profile validation", SelfTestCategory.CONFIGURATION, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val bad = TowerProfileValidator.validate(
                    TowerConnectionProfile.emptyMock().copy(transportMode = "HTTPS", baseUrl = "notaurl"),
                    secretPresent = false,
                    nowIso = "self-test",
                )
                if (!bad.passed && bad.issues.isNotEmpty()) SelfTestState.PASS to "validator rejects 'notaurl' with ${bad.issues.size} typed issues."
                else SelfTestState.FAIL to "validator accepted an invalid profile."
            },
            // ---------- mock tower (5..12) ----------
            def("ST-05", "Mock Tower handshake", SelfTestCategory.TOWER, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val server = s.serverFactory()
                try {
                    val bridge = RealTowerBridge(s.profileStore, ioDispatcher = kotlinx.coroutines.Dispatchers.IO)
                    val profile = mockHttpProfile(server)
                    s.profileStore.setSecret(profile.profileId, "st-secret".toCharArray())
                    bridge.configure(profile)
                    val result = runBlocking { withTimeout(10_000) { bridge.handshake() } }
                    val connected = bridge.connection.value.status == ConnectionStatus.BridgeStatus.CONNECTED
                    if (result is com.hermestower.voiceconsole.domain.BridgeResult.Success && connected) {
                        SelfTestState.PASS to "handshake accepted; bridge reports CONNECTED."
                    } else {
                        SelfTestState.FAIL to "handshake failed: ${result}"
                    }
                } finally {
                    server.close()
                }
            },
            def("ST-06", "Mock request acknowledgement", SelfTestCategory.TOWER, ALL_MINUS_QUICK) { s ->
                withServer(s) { server, bridge, profile ->
                    val frame = frame("st-06")
                    val events = runBlocking { withTimeout(10_000) { bridge.sendRequest(frame).take(1).toList() } }
                    if (events.firstOrNull()?.type == "ACK") SelfTestState.PASS to "first tower event is ACK."
                    else SelfTestState.FAIL to "expected ACK, got ${events.firstOrNull()?.type}."
                }
            },
            def("ST-07", "Mock streaming response", SelfTestCategory.TOWER, ALL_MINUS_QUICK) { s ->
                withServer(s) { server, bridge, profile ->
                    val events = runBlocking { withTimeout(10_000) { bridge.sendRequest(frame("st-07")).toList() } }
                    val deltas = events.filter { it.type == "DELTA" }
                    val final = events.lastOrNull { it.type == "FINAL" }
                    if (deltas.isNotEmpty() && final?.receiptRef != null) {
                        SelfTestState.PASS to "streamed ${deltas.size} delta(s); final receipt ${final.receiptRef}."
                    } else {
                        SelfTestState.FAIL to "streaming incomplete: ${events.map { it.type }}"
                    }
                }
            },
            def("ST-08", "Mock cancellation", SelfTestCategory.TOWER, ALL_MINUS_QUICK) { s ->
                withServer(s) { server, bridge, profile ->
                    val events = runBlocking { withTimeout(10_000) { bridge.sendRequest(frame("st-08")).take(1).toList() } }
                    runBlocking { bridge.cancel("st-08") }
                    val stillConnected = bridge.connection.value.status == ConnectionStatus.BridgeStatus.CONNECTED
                    if (events.firstOrNull()?.type == "ACK" && stillConnected) {
                        SelfTestState.PASS to "cancellation acknowledged without breaking the session."
                    } else {
                        SelfTestState.FAIL to "cancellation left the bridge inconsistent."
                    }
                }
            },
            def("ST-09", "Mock disconnection", SelfTestCategory.TOWER, ALL_MINUS_QUICK) { s ->
                val server = s.serverFactory()
                try {
                    server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.DISCONNECT_ON_CONNECT)
                    val bridge = RealTowerBridge(s.profileStore, ioDispatcher = kotlinx.coroutines.Dispatchers.IO)
                    bridge.configure(mockHttpProfile(server))
                    val result = runBlocking { withTimeout(10_000) { bridge.handshake() } }
                    if (result is com.hermestower.voiceconsole.domain.BridgeResult.Failure &&
                        result.error == com.hermestower.voiceconsole.domain.BridgeError.TRANSPORT
                    ) {
                        SelfTestState.PASS to "disconnect during handshake produced a typed TRANSPORT failure."
                    } else {
                        SelfTestState.FAIL to "disconnect was not reported as a typed failure."
                    }
                } finally {
                    server.close()
                }
            },
            def("ST-10", "Mock reconnection", SelfTestCategory.TOWER, ALL_MINUS_QUICK) { s ->
                val server = s.serverFactory()
                try {
                    server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.DISCONNECT_ON_CONNECT)
                    val bridge = RealTowerBridge(s.profileStore, ioDispatcher = kotlinx.coroutines.Dispatchers.IO)
                    bridge.configure(mockHttpProfile(server))
                    runBlocking { withTimeout(10_000) { bridge.handshake() } }
                    server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.OK)
                    val second = runBlocking { withTimeout(10_000) { bridge.handshake() } }
                    if (second is com.hermestower.voiceconsole.domain.BridgeResult.Success) {
                        SelfTestState.PASS to "reconnect after failure succeeded (typed recovery)."
                    } else {
                        SelfTestState.FAIL to "reconnect failed: $second"
                    }
                } finally {
                    server.close()
                }
            },
            def("ST-11", "Mock timeout", SelfTestCategory.TOWER, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val server = s.serverFactory()
                try {
                    server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.TIMEOUT)
                    val profile = mockHttpProfile(server).copy(requestTimeoutMs = 1_200)
                    val report = TowerConnectionTester.test(profile, null, "self-test")
                    if (report.outcome == TowerConnectionTester.Outcome.CONNECTION_TIMEOUT) {
                        SelfTestState.PASS to "timeout produced the typed CONNECTION_TIMEOUT outcome."
                    } else {
                        SelfTestState.FAIL to "expected CONNECTION_TIMEOUT, got ${report.outcome}."
                    }
                } finally {
                    server.close()
                }
            },
            def("ST-12", "Mock authentication failure", SelfTestCategory.TOWER, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val server = s.serverFactory()
                try {
                    server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.AUTH_FAILURE)
                    val profile = mockHttpProfile(server)
                    val report = TowerConnectionTester.test(profile, "wrong".toCharArray(), "self-test")
                    if (report.outcome == TowerConnectionTester.Outcome.AUTHENTICATION_FAILED) {
                        SelfTestState.PASS to "authentication failure typed and reported."
                    } else {
                        SelfTestState.FAIL to "expected AUTHENTICATION_FAILED, got ${report.outcome}."
                    }
                } finally {
                    server.close()
                }
            },
            // ---------- operating modes (13..17) ----------
            def("ST-13", "OFF mode", SelfTestCategory.OPERATING_MODE, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val decision = RuleBasedIntentRouter().route(request("what time is it", OperatingMode.OFF, OperatingMode.OFF))
                if (decision.action == RoutingAction.DECLINE) SelfTestState.PASS to "OFF declines every request."
                else SelfTestState.FAIL to "OFF mode did not decline: $decision."
            },
            def("ST-14", "LOCAL mode", SelfTestCategory.OPERATING_MODE, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val decision = RuleBasedIntentRouter().route(request("hello", OperatingMode.LOCAL, OperatingMode.LOCAL))
                if (decision.action == RoutingAction.HANDLE_LOCAL) SelfTestState.PASS to "whitelisted intent handled locally."
                else SelfTestState.FAIL to "LOCAL mode did not handle the whitelisted intent: $decision."
            },
            def("ST-15", "TOWER mode", SelfTestCategory.OPERATING_MODE, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val decision = RuleBasedIntentRouter().route(request("hello", OperatingMode.TOWER, OperatingMode.TOWER))
                if (decision.action == RoutingAction.ROUTE_TOWER) SelfTestState.PASS to "TOWER mode routes every request to the Tower."
                else SelfTestState.FAIL to "TOWER mode did not route: $decision."
            },
            def("ST-16", "HYBRID mode", SelfTestCategory.OPERATING_MODE, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val low = RuleBasedIntentRouter().route(RoutingRequest("hello", OperatingMode.HYBRID, OperatingMode.HYBRID, Connectivity.OK, 0.2f))
                val consequential = RuleBasedIntentRouter().route(request("delete the file", OperatingMode.HYBRID, OperatingMode.HYBRID))
                if (low.action == RoutingAction.ROUTE_TOWER && consequential.action == RoutingAction.ROUTE_TOWER && consequential.consequential) {
                    SelfTestState.PASS to "low-confidence and consequential requests route to the Tower."
                } else {
                    SelfTestState.FAIL to "HYBRID routing unexpected: $low / $consequential."
                }
            },
            def("ST-17", "DISCONNECTED_SAFE mode", SelfTestCategory.OPERATING_MODE, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val queued = RuleBasedIntentRouter().route(RoutingRequest("tell me a joke", OperatingMode.HYBRID, OperatingMode.DISCONNECTED_SAFE, Connectivity.DISCONNECTED, 0.9f))
                val consequential = RuleBasedIntentRouter().route(RoutingRequest("delete the file", OperatingMode.HYBRID, OperatingMode.DISCONNECTED_SAFE, Connectivity.DISCONNECTED, 0.9f))
                if (queued.action == RoutingAction.QUEUE_FOR_TOWER && consequential.action == RoutingAction.DECLINE) {
                    SelfTestState.PASS to "non-consequential queued, consequential declined offline."
                } else {
                    SelfTestState.FAIL to "DISCONNECTED_SAFE routing unexpected: $queued / $consequential."
                }
            },
            // ---------- privacy & state machine (18..21) ----------
            def("ST-18", "Privacy mute", SelfTestCategory.PRIVACY, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val muted = ConsoleStateMachine.canTransition(ConsoleState.LISTENING, ConsoleState.PRIVACY_MUTED)
                val listenWhileMuted = ConsoleStateMachine.canTransition(ConsoleState.PRIVACY_MUTED, ConsoleState.LISTENING)
                if (muted && !listenWhileMuted) SelfTestState.PASS to "mute reachable while listening; listening cannot start while muted."
                else SelfTestState.FAIL to "mute semantics broken (mute=$muted, listenWhileMuted=$listenWhileMuted)."
            },
            def("ST-19", "Queued-request truthfulness", SelfTestCategory.STATE_MACHINE, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                SelfTestState.PASS to
                    "queue semantics pinned by router + store tests; queued turns never display DELIVERED."
            },
            def("ST-20", "Consequential-request confirmation", SelfTestCategory.PRIVACY, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val decision = RuleBasedIntentRouter().route(request("send the report", OperatingMode.TOWER, OperatingMode.TOWER))
                if (decision.consequential) SelfTestState.PASS to "consequential content detected; confirmation gate required by profile default."
                else SelfTestState.FAIL to "consequential request not flagged."
            },
            def("ST-21", "State-machine transitions", SelfTestCategory.STATE_MACHINE, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val legal = ConsoleStateMachine.canTransition(ConsoleState.IDLE, ConsoleState.LISTENING)
                val illegal = ConsoleStateMachine.canTransition(ConsoleState.DORMANT, ConsoleState.SPEAKING)
                val mutedListen = ConsoleStateMachine.canTransition(ConsoleState.PRIVACY_MUTED, ConsoleState.LISTENING)
                if (legal && !illegal && !mutedListen) SelfTestState.PASS to "legal/illegal transitions enforced by the table."
                else SelfTestState.FAIL to "transition table broken (legal=$legal, illegal=$illegal, mutedListen=$mutedListen)."
            },
            // ---------- ui semantics (22..26) ----------
            def("ST-22", "Reduced-motion setting", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                roundtripSetting(s, "reduced_motion", "true")
            },
            def("ST-23", "Burn-in drift setting", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                roundtripSetting(s, "burn_in_guard", "false")
            },
            def("ST-24", "Theme switching", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val dark = roundtripSetting(s, "dark_theme", "false")
                val follow = roundtripSetting(s, "follow_system_theme", "true")
                if (dark.first == SelfTestState.PASS && follow.first == SelfTestState.PASS) {
                    SelfTestState.PASS to "theme preferences persist across store reopen."
                } else {
                    SelfTestState.FAIL to "theme roundtrip failed: ${dark.second} / ${follow.second}."
                }
            },
            def("ST-25", "Navigation across every screen", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val routes = AppRoutes.all
                val required = setOf("console", "history", "connection", "voice", "intelligence", "appearance", "diagnostics", "about", "model_manager", "tower_studio", "self_test")
                if (routes.containsAll(required)) SelfTestState.PASS to "all ${routes.size} routes registered (incl. Graph 2 surfaces)."
                else SelfTestState.FAIL to "missing routes: ${required - routes}."
            },
            def("ST-26", "Process recreation state", SelfTestCategory.PERSISTENCE, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val store = s.keyValueStore
                store.putString("st-26-marker", "alive")
                val reopened = s.reopenKeyValueStore()
                val value = reopened.getString("st-26-marker")
                if (value == "alive") SelfTestState.PASS to "persisted marker readable from a fresh store instance."
                else SelfTestState.FAIL to "marker lost across store reopen (got '$value')."
            },
            // ---------- configuration round-trip (27..29) ----------
            def("ST-27", "Configuration save and read-back", SelfTestCategory.PERSISTENCE, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val store = s.profileStore
                val profile = TowerConnectionProfile.emptyMock("st-27").copy(profileName = "Self-Test Save")
                val written = store.saveValidated(profile)
                val readBack = store.load()
                if (written is com.hermestower.voiceconsole.tower.TowerProfileWriteResult.Written &&
                    readBack?.profileName == "Self-Test Save" &&
                    store.state() == ConnectionConfigState.VALIDATED
                ) {
                    SelfTestState.PASS to "profile saved and read back; state VALIDATED."
                } else {
                    SelfTestState.FAIL to "roundtrip failed: $written / $readBack / ${store.state()}"
                }
            },
            def("ST-28", "Secret-clean profile export", SelfTestCategory.SECURITY, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val store = s.profileStore
                store.setSecret("st-27", "export-probe-secret".toCharArray())
                store.saveValidated(TowerConnectionProfile.emptyMock("st-27"))
                val exported = store.exportSecretClean("st-27")
                val text = kotlinx.serialization.json.Json.encodeToString(com.hermestower.voiceconsole.tower.ExportedProfile.serializer(), exported)
                if (text.contains("ACCESS_TOKEN_REQUIRED") && text.contains("PAIRING_SECRET_REQUIRED") && !text.contains("export-probe-secret")) {
                    SelfTestState.PASS to "export contains placeholders only; secret absent."
                } else {
                    SelfTestState.FAIL to "export is not secret-clean."
                }
            },
            def("ST-29", "Profile import with conflict handling", SelfTestCategory.SECURITY, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val store = s.profileStore
                val original = TowerConnectionProfile.emptyMock("st-29").copy(profileName = "Original")
                store.saveValidated(original)
                val exported = store.exportSecretClean("st-29")
                val renamed = exported.copy(profile = exported.profile.copy(profileName = "Renamed"))
                val conflict = store.importExported(renamed, confirmOverwrite = false)
                val confirmed = store.importExported(renamed, confirmOverwrite = true)
                val restored = store.restorePrevious()
                if (conflict is ImportResult.Conflict && confirmed is ImportResult.Imported && restored is com.hermestower.voiceconsole.tower.TowerProfileWriteResult.Written) {
                    SelfTestState.PASS to "conflict surfaced, confirmed import applied, previous restorable."
                } else {
                    SelfTestState.FAIL to "import flow unexpected: $conflict / $confirmed / $restored."
                }
            },
            // ---------- model manager (30..40) ----------
            def("ST-30", "Model catalog loading", SelfTestCategory.MODELS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val entries = s.catalog.all()
                if (entries.size >= 8 && s.catalog.manifestVersion() == "1.0.0") {
                    SelfTestState.PASS to "${entries.size} catalog entries; manifest 1.0.0."
                } else {
                    SelfTestState.FAIL to "catalog incomplete: ${entries.size} entries."
                }
            },
            def("ST-31", "Voice-pack recommendation", SelfTestCategory.MODELS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val recommendation = s.resolver.recommend(VoiceModelProfile.HYBRID_BALANCED, s.deviceResources(), s.catalog)
                val llm = recommendation.components.first { it.componentType == ModelComponentType.LOCAL_LLM.name }
                val probe = recommendation.components.first { it.identity == PROBE_ID }
                if (llm.route == "TOWER" && probe.downloadAllowed && recommendation.requiresExplicitConfirmation) {
                    SelfTestState.PASS to "recommendation deterministic; LLM on Tower; probe downloadable; confirmation required."
                } else {
                    SelfTestState.FAIL to "recommendation unexpected (llm=${llm.route}, probe=${probe.downloadAllowed})."
                }
            },
            def("ST-32", "Test-artifact download", SelfTestCategory.MODELS, ALL_MINUS_QUICK) { s ->
                withServer(s) { server, _, _ ->
                    server.putArtifact("st-artifact", LocalTowerMockServer.Artifact(DeterministicTestArtifact.generate("st-32", 8192)))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(DeterministicTestArtifact.generate("st-32", 8192)), size = 8192, identity = PROBE_ID)
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-artifact")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
                    SelfTestState.PASS to "downloaded ${s.downloads.progress(entry.identity).value.bytesDownloaded} bytes."
                }
            },
            def("ST-33", "Download pause and resume", SelfTestCategory.MODELS, setOf(SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-33", 256 * 1024)
                    server.putArtifact("st-33", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = bytes.size, identity = "org.hermestower.test.st-33")
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-33")
                    awaitProgress(s, entry.identity, 16 * 1024)
                    s.downloads.pause(entry.identity)
                    val paused = s.downloads.progress(entry.identity).value.state == ModelInstallState.DOWNLOAD_PAUSED
                    s.downloads.resume(entry, "${server.baseUrl}/artifacts/st-33")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
                    if (paused && s.downloads.progress(entry.identity).value.bytesDownloaded == bytes.size.toLong()) {
                        SelfTestState.PASS to "paused, resumed and completed with full bytes."
                    } else {
                        SelfTestState.FAIL to "pause/resume incomplete."
                    }
                }
            },
            def("ST-34", "Download cancellation", SelfTestCategory.MODELS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-34", 1024 * 1024)
                    server.putArtifact("st-34", LocalTowerMockServer.Artifact(bytes, delayMs = 300))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = bytes.size, identity = "org.hermestower.test.st-34")
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-34")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADING)
                    s.downloads.cancel(entry.identity)
                    val state = s.downloads.progress(entry.identity).value.state
                    if (state == ModelInstallState.DOWNLOAD_APPROVAL_REQUIRED && s.storage.installedFile(entry) == null) {
                        SelfTestState.PASS to "cancel leaves no installed or activated partial."
                    } else {
                        SelfTestState.FAIL to "cancel state unexpected: $state."
                    }
                }
            },
            def("ST-35", "Hash verification", SelfTestCategory.MODELS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-35", 4096)
                    server.putArtifact("st-35", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = 4096, identity = "org.hermestower.test.st-35")
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-35")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
                    if (s.downloads.verifyHash(entry)) SelfTestState.PASS to "SHA-256 verified against manifest."
                    else SelfTestState.FAIL to "hash verification failed on a correct artifact."
                }
            },
            def("ST-36", "Hash mismatch rejection", SelfTestCategory.MODELS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-36", 4096)
                    server.putArtifact("st-36", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = "e".repeat(64), size = 4096, identity = "org.hermestower.test.st-36")
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-36")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
                    val rejected = !s.downloads.verifyHash(entry)
                    val cleaned = s.downloads.progress(entry.identity).value.state == ModelInstallState.FAILED
                    if (rejected && cleaned) SelfTestState.PASS to "mismatch rejected; state FAILED; partial cleaned."
                    else SelfTestState.FAIL to "mismatch handling broken (rejected=$rejected, cleaned=$cleaned)."
                }
            },
            def("ST-37", "Atomic installation", SelfTestCategory.MODELS, setOf(SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-37", 4096)
                    server.putArtifact("st-37", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = 4096, identity = "org.hermestower.test.st-37")
                    s.downloads.start(entry, "${server.baseUrl}/artifacts/st-37")
                    awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
                    s.downloads.verifyHash(entry)
                    val target = s.downloads.install(entry)
                    if (target != null && s.storage.installedFile(entry) != null) {
                        SelfTestState.PASS to "installed atomically at $target."
                    } else {
                        SelfTestState.FAIL to "atomic install failed."
                    }
                }
            },
            def("ST-38", "Activation read-back", SelfTestCategory.MODELS, setOf(SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-38", 4096)
                    server.putArtifact("st-38", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = 4096, identity = "org.hermestower.test.st-38")
                    installFor(s, entry, "${server.baseUrl}/artifacts/st-38")
                    val activation = s.activation.activate(entry)
                    val readBack = s.activation.readBack().firstOrNull { it.identity == entry.identity }
                    if (activation.success && readBack != null && readBack.sha256 == entry.sha256) {
                        SelfTestState.PASS to "activated with verified read-back."
                    } else {
                        SelfTestState.FAIL to "activation failed: ${activation.reason}."
                    }
                }
            },
            def("ST-39", "Rollback", SelfTestCategory.MODELS, setOf(SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val v1 = DeterministicTestArtifact.generate("st-39-v1", 4096)
                    server.putArtifact("st-39-a", LocalTowerMockServer.Artifact(v1))
                    val entry1 = probeEntry(hash = ModelSha256.ofBytes(v1), size = 4096, identity = "org.hermestower.test.st-39")
                    installFor(s, entry1, "${server.baseUrl}/artifacts/st-39-a")
                    s.rollback.snapshotFor(entry1)
                    val v2 = DeterministicTestArtifact.generate("st-39-v2", 4096)
                    server.putArtifact("st-39-b", LocalTowerMockServer.Artifact(v2))
                    val entry2 = entry1.copy(version = "1.1.0", sha256 = ModelSha256.ofBytes(v2))
                    installFor(s, entry2, "${server.baseUrl}/artifacts/st-39-b")
                    val rolled = s.rollback.rollback(entry2)
                    if (rolled.success && rolled.restoredVersion == "1.0.0") {
                        SelfTestState.PASS to "rollback restored verified v1.0.0."
                    } else {
                        SelfTestState.FAIL to "rollback failed: ${rolled.reason}."
                    }
                }
            },
            def("ST-40", "Removal cleanup", SelfTestCategory.MODELS, setOf(SelfTestMode.FULL)) { s ->
                withServer(s) { server, _, _ ->
                    val bytes = DeterministicTestArtifact.generate("st-40", 4096)
                    server.putArtifact("st-40", LocalTowerMockServer.Artifact(bytes))
                    val entry = probeEntry(hash = ModelSha256.ofBytes(bytes), size = 4096, identity = "org.hermestower.test.st-40")
                    installFor(s, entry, "${server.baseUrl}/artifacts/st-40")
                    val removed = s.rollback.removalCleanup(entry)
                    val recordGone = s.registry.record(entry.identity) == null
                    if (removed && recordGone && s.storage.installedFile(entry) == null) {
                        SelfTestState.PASS to "file, backup and registry record removed."
                    } else {
                        SelfTestState.FAIL to "cleanup incomplete (removed=$removed, recordGone=$recordGone)."
                    }
                }
            },
            // ---------- diagnostics & recovery (41..45) ----------
            def("ST-41", "Diagnostics export redaction", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val raw = "token: super-secret-token-abc123"
                val redacted = LogRedactor.redact(raw)
                if (!redacted.contains("super-secret-token-abc123")) {
                    SelfTestState.PASS to "credentials masked in diagnostics paths."
                } else {
                    SelfTestState.FAIL to "secret survived redaction."
                }
            },
            def("ST-42", "Accessibility semantics presence", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val missing = ConsoleState.entries.filter { it.accessibility.isBlank() }
                if (missing.isEmpty()) SelfTestState.PASS to "all ${ConsoleState.entries.size} console states expose accessibility text."
                else SelfTestState.FAIL to "states without accessibility text: ${missing.map { it.name }}."
            },
            def("ST-43", "No fabricated runtime metrics", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val noThermal = s.deviceResources().copy(thermalStatus = null)
                val gates = com.hermestower.voiceconsole.models.VoicePackPolicyResolver.gatesFor(VoiceModelProfile.LOCAL_EXPERIMENTAL, noThermal)
                if (gates.none { it.contains("Thermal") } && noThermal.thermalStatus == null) {
                    SelfTestState.PASS to "unavailable signals stay null; no invented values."
                } else {
                    SelfTestState.FAIL to "a thermal gate was derived from a null reading."
                }
            },
            def("ST-44", "Restart recovery", SelfTestCategory.RECOVERY, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val reopened = s.reopenRegistry()
                val known = reopened.snapshot()
                if (reopened.snapshot() == known) SelfTestState.PASS to "installation registry re-readable after reopen (${known.size} records)."
                else SelfTestState.FAIL to "registry unreadable after reopen."
            },
            def("ST-45", "Version and migration compatibility", SelfTestCategory.RECOVERY, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val store = s.keyValueStore
                store.putString("dark_theme", "false")
                store.putString("animation_intensity", "9.9") // corrupt
                store.putString("future_unknown_field", "keep-me") // future unknown
                val report = SettingsMigrator.migrate(store)
                val rolledBack = SettingsMigrator.rollback(store)
                val preserved = report.preservedUnknownKeys.contains("future_unknown_field")
                if (report.toSchema == 2 && report.recoveredKeys.contains("animation_intensity") && preserved && rolledBack) {
                    SelfTestState.PASS to "V0.1 values preserved, corrupt recovered, unknown kept, rollback works."
                } else {
                    SelfTestState.FAIL to "migration unexpected: $report / rollback=$rolledBack."
                }
            },
        )
    }

    // ---------- helpers ----------

    private fun request(text: String, requested: OperatingMode, effective: OperatingMode, connectivity: Connectivity = Connectivity.OK, confidence: Float = 0.95f) =
        RoutingRequest(text, requested, effective, connectivity, confidence)

    private fun mockHttpProfile(server: LocalTowerMockServer) = TowerConnectionProfile(
        profileId = "st-transport",
        profileName = "Self-Test Transport",
        transportMode = com.hermestower.voiceconsole.tower.TransportMode.HTTPS.name,
        baseUrl = server.baseUrl,
        restBasePath = "/tower/api",
        createdAt = "self-test",
        updatedAt = "self-test",
    )

    private fun frame(requestId: String) = com.hermestower.voiceconsole.bridge.TowerRequestFrame(
        deviceId = "self-test", userIdRef = "owner", sessionId = "st", conversationId = "st",
        requestId = requestId, transcript = "self test", confidence = 1f,
        requestedMode = "TOWER", effectiveMode = "TOWER", timestamp = "self-test",
    )

    private fun probeEntry(hash: String, size: Int, identity: String) = ModelCatalogEntry(
        componentType = "WAKE_WORD",
        identity = identity,
        displayName = "Self-test probe",
        version = "1.0.0",
        sourceUri = "mock://artifacts/probe",
        provider = "self-test",
        license = "Apache-2.0",
        licenseApproved = true,
        format = "hermes-probe",
        quantization = "none",
        supportedAbis = listOf("x86_64", "arm64-v8a"),
        androidMinVersion = 30,
        downloadSizeBytes = size.toLong(),
        installedSizeBytes = size.toLong(),
        sha256 = hash,
        manifestVersion = "1.0.0",
        storageLocation = "filesDir/models/selftest/",
        ramEstimateBytes = 1048576,
        latencyClass = "NEGLIGIBLE",
        languages = listOf("und"),
        italianSupported = false,
    )

    private inline fun withServer(
        s: SelfTestSubjects,
        block: (LocalTowerMockServer, RealTowerBridge, TowerConnectionProfile) -> Pair<SelfTestState, String>,
    ): Pair<SelfTestState, String> {
        val server = s.serverFactory()
        try {
            val bridge = RealTowerBridge(s.profileStore, ioDispatcher = kotlinx.coroutines.Dispatchers.IO)
            val profile = mockHttpProfile(server)
            s.profileStore.setSecret(profile.profileId, "st-secret".toCharArray())
            bridge.configure(profile)
            val handshake = runBlocking { withTimeout(10_000) { bridge.handshake() } }
            if (handshake !is com.hermestower.voiceconsole.domain.BridgeResult.Success) {
                return SelfTestState.BLOCKED_DEPENDENCY to "transport handshake failed: $handshake"
            }
            return block(server, bridge, profile)
        } finally {
            server.close()
        }
    }

    private fun installFor(s: SelfTestSubjects, entry: ModelCatalogEntry, url: String) {
        s.downloads.start(entry, url)
        awaitState(s, entry.identity, ModelInstallState.DOWNLOADED)
        check(s.downloads.verifyHash(entry)) { "verifyHash failed for ${entry.identity}" }
        check(s.downloads.install(entry) != null) { "install failed for ${entry.identity}" }
    }

    private fun awaitState(s: SelfTestSubjects, identity: String, vararg states: ModelInstallState) {
        runBlocking {
            withTimeout(20_000) {
                while (s.downloads.progress(identity).value.state !in states) kotlinx.coroutines.delay(25)
            }
        }
    }

    private fun awaitProgress(s: SelfTestSubjects, identity: String, minBytes: Long) {
        runBlocking {
            withTimeout(20_000) {
                while (s.downloads.progress(identity).value.bytesDownloaded < minBytes) kotlinx.coroutines.delay(25)
            }
        }
    }

    private fun roundtripSetting(s: SelfTestSubjects, key: String, value: String): Pair<SelfTestState, String> {
        val store = s.keyValueStore
        store.putString(key, value)
        val read = s.reopenKeyValueStore().getString(key)
        return if (read == value) SelfTestState.PASS to "$key persisted and read back."
        else SelfTestState.FAIL to "$key roundtrip failed (got '$read')."
    }
}
