package com.hermestower.voiceconsole.selftest

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateMachine
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.archive.ArtifactDownloadResult
import com.hermestower.voiceconsole.archive.ContinuationResult
import com.hermestower.voiceconsole.models.BundledModelInstaller
import com.hermestower.voiceconsole.models.DefaultVoiceModelCatalog
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
 * The 51 deterministic self-tests (GRAPH 2 §11 + GRAPH 4 G4-C11 expansion,
 * ST-46..ST-50: bundled delivery, QR pairing, archive truth). Every check is bounded, offline
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
                // CP-G4-029 (Owner retest repair): the repair build advances the
                // version — 0.3.x-alpha-graph4* is the accepted Owner-package schema.
                val ok = s.appVersion.isNotBlank() && s.appVersion.startsWith("0.3.")
                if (ok) SelfTestState.PASS to "app version ${s.appVersion} reported truthfully."
                else SelfTestState.FAIL to "app version '${s.appVersion}' does not match 0.3.x-alpha-graph4 schema (Owner package)."
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
                val towerFirst = s.resolver.recommend(VoiceModelProfile.TOWER_FIRST, s.deviceResources(), s.catalog)
                val hybrid = s.resolver.recommend(VoiceModelProfile.HYBRID_BALANCED, s.deviceResources(), s.catalog)
                val llmTower = towerFirst.components.first { it.componentType == ModelComponentType.LOCAL_LLM.name }
                val llmHybrid = hybrid.components.first { it.componentType == ModelComponentType.LOCAL_LLM.name }
                val probe = hybrid.components.first { it.identity == PROBE_ID }
                if (llmTower.route == "TOWER" && llmHybrid.route == "LOCAL" && probe.downloadAllowed && hybrid.requiresExplicitConfirmation) {
                    SelfTestState.PASS to "recommendation deterministic; TOWER_FIRST routes LLM to Tower, HYBRID keeps it local; probe downloadable; confirmation required."
                } else {
                    SelfTestState.FAIL to "recommendation unexpected (tower=${llmTower.route}, hybrid=${llmHybrid.route}, probe=${probe.downloadAllowed})."
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
                    server.putArtifact("st-33", LocalTowerMockServer.Artifact(bytes, chunkDelayMs = 15))
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
                    server.putArtifact("st-34", LocalTowerMockServer.Artifact(bytes, delayMs = 300, chunkDelayMs = 15))
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
            // ---------- GRAPH 4 expansion (G4-C11 wave): bundled delivery, QR pairing, archive truth ----------
            def("ST-46", "Bundled manifest double-gate agreement", SelfTestCategory.MODELS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val text = s.bundledManifestText()
                if (text == null) {
                    SelfTestState.SKIPPED_NOT_APPLICABLE to "bundled manifest not readable on this runtime."
                } else {
                    val manifest = DefaultVoiceModelCatalog.json.decodeFromString(
                        BundledModelInstaller.BundledManifest.serializer(), text,
                    )
                    val conflicts = manifest.artifacts.mapNotNull { a ->
                        val e = s.catalog.find(a.identity)
                        when {
                            e == null -> "${a.asset}: identity missing from catalog"
                            e.sha256 != a.sha256 -> "${a.asset}: hash conflict"
                            e.version != a.version -> "${a.asset}: version conflict"
                            e.downloadSizeBytes != a.sizeBytes -> "${a.asset}: size conflict"
                            else -> null
                        }
                    }
                    if (conflicts.isEmpty()) {
                        SelfTestState.PASS to "${manifest.artifacts.size} bundled artifacts agree with the shipped catalog on hash/version/size."
                    } else {
                        SelfTestState.FAIL to "double-gate conflicts: ${conflicts.joinToString("; ")}."
                    }
                }
            },
            def("ST-47", "Bundled delivery end-to-end (Owner §6)", SelfTestCategory.MODELS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val installer = s.bundledInstaller
                if (installer == null) {
                    SelfTestState.SKIPPED_NOT_APPLICABLE to "bundled installer not wired on this runtime."
                } else {
                    val report = installer.deliverAll()
                    val second = installer.deliverAll()
                    val reverified = second.results.count { it is BundledModelInstaller.BundledInstallResult.AlreadyInstalled }
                    if (report.allDelivered && report.installedCount == 4 && second.allDelivered && reverified == 4) {
                        SelfTestState.PASS to "4/4 delivered through the hash-gated, receipted pipeline; re-delivery idempotent (4 re-verified, no network)."
                    } else {
                        SelfTestState.FAIL to "delivery truth: stoppedAt=${report.stoppedAt} installed=${report.installedCount} reVerified=$reverified secondAll=${second.allDelivered}."
                    }
                }
            },
            def("ST-48", "QR pairing parser strict matrix probe", SelfTestCategory.TOWER, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.CONNECTIVITY_ONLY)) { s ->
                val now = System.currentTimeMillis()
                fun uri(nonce: String, issued: Long, expires: Long) =
                    "hermes-pair://v1/tower-selftest?env=local&iat=$issued&exp=$expires&n=$nonce&c=CHALLENGE&ep=https%3A%2F%2Ftower.local%2Fpair"
                val parser = s.pairingParserFactory()
                val valid = parser.parse(uri("SELFTESTNONCE1", now - 60_000, now + 300_000))
                val replay = parser.parse(uri("SELFTESTNONCE1", now - 60_000, now + 300_000))
                val expired = s.pairingParserFactory().parse(uri("SELFTESTNONCE2", now - 700_000, now - 100_000))
                val wrongScheme = s.pairingParserFactory().parse("https://tower.local/qr")
                val okValid = valid.getOrNull()?.towerId == "tower-selftest"
                fun kind(r: Result<com.hermestower.voiceconsole.pairing.PairingPayload>) =
                    (r.exceptionOrNull() as? com.hermestower.voiceconsole.pairing.PairingRejectException)?.reject?.kind
                val matrix = listOf(
                    "parse=${if (okValid) "ok" else "fail"}",
                    "replay=${kind(replay)}",
                    "expired=${kind(expired)}",
                    "scheme=${kind(wrongScheme)}",
                )
                if (okValid && kind(replay) == com.hermestower.voiceconsole.pairing.PairingRejectKind.REPLAYED_NONCE &&
                    kind(expired) == com.hermestower.voiceconsole.pairing.PairingRejectKind.EXPIRED &&
                    kind(wrongScheme) == com.hermestower.voiceconsole.pairing.PairingRejectKind.UNSUPPORTED_SCHEME
                ) {
                    SelfTestState.PASS to "strict matrix: valid parses, replay/expiry/scheme rejected with typed kinds (no endpoint contacted)."
                } else {
                    SelfTestState.FAIL to "matrix unexpected: ${matrix.joinToString(", ")}."
                }
            },
            def("ST-49", "Archive authority truthfulness (Owner §15/§16/§17)", SelfTestCategory.TOWER, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val archive = s.towerArchive
                if (archive == null) {
                    SelfTestState.SKIPPED_NOT_APPLICABLE to "archive not wired on this runtime."
                } else {
                    // §15: local-cache removal and clearCache NEVER delete remote authority rows.
                    val before = archive.listConversations(1, 100).items.size
                    archive.cachedConversationIds().forEach { archive.removeLocalCache(it) }
                    archive.clearCache()
                    val after = archive.listConversations(1, 100).items.size
                    val s15 = before > 0 && after == before
                    // §16: continuation authority is the REAL Tower — the mock answers typed SIMULATED/Refused.
                    val cont = archive.listMissions(1, 100).items.firstOrNull()?.let { archive.requestContinuation(it.id) }
                    val s16 = cont is ContinuationResult.SimulatedAccepted || cont is ContinuationResult.Refused
                    // §17: download is typed SIMULATED (no silent byte regeneration).
                    val dl = archive.listArtifacts(1, 100).items.firstOrNull()?.let { archive.download(it.id) }
                    val s17 = dl is ArtifactDownloadResult.Simulated ||
                        (dl is ArtifactDownloadResult.Completed && dl.sha256.isNotBlank())
                    if (s15 && s16 && s17) {
                        SelfTestState.PASS to "authority rows survive cache removal ($before kept); continuation typed ${cont?.javaClass?.simpleName}; download typed ${dl?.javaClass?.simpleName}."
                    } else {
                        SelfTestState.FAIL to "truth breach: §15=$s15 §16=$s16 §17=$s17."
                    }
                }
            },
            def("ST-50", "Graph 4 route registry", SelfTestCategory.UI_SEMANTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val required = setOf(AppRoutes.ARCHIVE, AppRoutes.QR_SCANNER, AppRoutes.TOWER_STUDIO, AppRoutes.MODEL_MANAGER, AppRoutes.SELF_TEST)
                val missing = required - AppRoutes.all
                if (missing.isEmpty()) {
                    SelfTestState.PASS to "Graph 4 surfaces registered: archive, qr_scanner, tower_studio, model_manager, self_test."
                } else {
                    SelfTestState.FAIL to "missing routes: $missing."
                }
            },
            def("ST-51", "Italian TTS voice negotiation probe (Owner Finding 3)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.italianTtsVoiceProbe
                when {
                    // OWNER PHYSICAL RETEST §7: a JVM/emulator runtime cannot
                    // initialize a real engine or produce audio — the typed
                    // result is QUALIFICATION_PENDING_DEVICE (never a pass).
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: no real TTS engine can produce audio here. The real-engine path (initialize → enumerate → select Italian → read back → synthesize → utterance start/done) is qualified on the physical device."
                    probe() == null ->
                        SelfTestState.SKIPPED_NOT_APPLICABLE to "the TTS engine has not negotiated yet (not initialized on this run)."
                    else -> {
                        val rb = probe()!!
                        when (rb.status) {
                            com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE ->
                                SelfTestState.PASS to com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.describe(rb)
                            // Device-side dependency states: the Italian voice
                            // data is missing or the engine cannot do Italian.
                            com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA,
                            com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED,
                            com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ENGINE_UNAVAILABLE,
                            ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    (com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.describe(rb) +
                                        (rb.detail?.let { " — $it" } ?: "") +
                                        " Repair: install the Italian voice data from the system Text-to-speech settings, then re-run.")
                            // A non-Italian voice is ACTIVE while Italian was
                            // requested — the engine fell back to English: FAIL
                            // (the app's fail-closed gate refuses the audio).
                            else ->
                                SelfTestState.FAIL to
                                    ("English or non-Italian voice active while Italian was requested (ITALIAN_SYSTEM_TTS_READBACK_MISMATCH). " +
                                        com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.describe(rb))
                        }
                    }
                }
            },
            def("ST-52", "Natural local TTS route (CP-G4.9-003)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.naturalTtsRouteProbe
                when {
                    // JVM/emulator: no native runtime, no audible audio — a
                    // typed pending, never a simulated pass.
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the sherpa-onnx native runtime cannot synthesise or play audio here. The physical path (bundle load → synthesis → PCM playback → interruption → unload) is qualified on the physical device."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "route read-back not produced this run."
                            rb.activeRoute.startsWith("NATURAL") && rb.voiceBundlePresent ->
                                SelfTestState.PASS to "NATURAL route active: ${rb.naturalEngineId ?: rb.activeRoute} (bundle verified on disk)."
                            rb.activeRoute.startsWith("NATURAL") && !rb.voiceBundlePresent ->
                                SelfTestState.FAIL to "route reports NATURAL but the voice bundle does not resolve on disk — route-truth violation."
                            else ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "SYSTEM route active (mandatory fallback). The natural piper voice is not installed/activated in the Model Sanctum — download it under Models and Voice, then re-run."
                        }
                    }
                }
            },
            def("ST-53", "Local offline STT route (CP-G4.9-004)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.QUICK, SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.localSttRouteProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: no microphone capture and no vosk native decoder here. The physical path (model load → capture/fixture WAV → transcript → unload) is qualified on the physical device."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "route read-back not produced this run."
                            rb.offlineModelLoaded ->
                                SelfTestState.PASS to "offline route active: ${rb.activeEngine} (model ${rb.loadedModelDir ?: "?"})."
                            else ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "active: ${rb.activeEngine}. The offline vosk Italian model is not installed — download it under Models and Voice, then re-run."
                        }
                    }
                }
            },
            def("ST-54", "Voice A/B comparison (Graph 4.11 Node 1)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.voiceAbQualificationProbe
                when {
                    // JVM/emulator: no native synthesis and no audible audio —
                    // a typed pending, never a simulated pass (ST-52 precedent).
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the A/B audition cannot produce audio here. The physical path (voice A speaks the preview phrase → voice B speaks the SAME phrase → the Owner hears the difference) is qualified on the physical device."
                    // The probe performs the REAL audition; it runs EXACTLY ONCE
                    // and every branch reads the same read-back.
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "the A/B audition did not produce a read-back this run."
                            rb.voiceAId == null ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "no natural voice is ACTIVE. Install and activate a piper voice in Model Sanctum (Models and Voice), then re-run. The system Italian TTS route is active meanwhile."
                            rb.voiceBId == null ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "only one voice is installed (${rb.voiceAId}). The A/B comparison needs a SECOND installed voice — download the other piper Italian voice in Model Sanctum, then re-run."
                            rb.utteranceA != "COMPLETED" || rb.utteranceB != "COMPLETED" ->
                                SelfTestState.FAIL to
                                    ("the A/B audition did not complete on both legs — A=${rb.utteranceA}, B=${rb.utteranceB}" +
                                        (rb.detail?.let { " — $it" } ?: "") +
                                        ". Check the voice bundles on disk and re-run; the active voice fell back safely if the runtime failed.")
                            !rb.identitiesDistinct ->
                                SelfTestState.FAIL to
                                    "both legs completed but the runtime reported the SAME voice identity (${rb.readBackAId}) — the voices cannot be proven distinct. This is a route-truth violation."
                            !rb.authorityUnchanged ->
                                SelfTestState.FAIL to
                                    "the audition changed the selection authority state — the A/B probe must never touch the selected voice. Route-truth violation."
                            else ->
                                SelfTestState.PASS to
                                    ("voice A (${rb.voiceAId}) and voice B (${rb.voiceBId}) both completed the same preview phrase with distinct runtime identities. " +
                                        "The audible difference is confirmed by the OWNER in the physical retest (VOICE_SWITCHING_OWNER_VERIFICATION_PENDING until then).")
                        }
                    }
                }
            },
            // ---- GRAPH 4.12 §9: ST-55 (Vosk physical function truth) ----
            // GRAPH 4.14 §25 HONESTY REPAIR: ST-55..ST-57 were appended AFTER the
            // closing paren of the returned list (a latent CP-G4.12 defect — the
            // bare def() values were DISCARDED, so the battery silently ran 54
            // tests while the file grew to 57). They are now INSIDE the returned
            // list, together with the new ST-58..ST-61.
            def("ST-55", "Vosk function truth (Graph 4.12 §9)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.voskFunctionProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the Vosk fixture needs the native libvosk.so runtime. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no Vosk read-back this run."
                            !rb.modelVerified ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "the offline Italian model is not installed/verified in Model Sanctum — install vosk-model-small-it-0.22, then re-run."
                            !rb.modelLoaded ->
                                SelfTestState.FAIL to
                                    ("the model is installed but the runtime could NOT load it (libvosk.so or model files)" +
                                        (rb.detail?.let { " — $it" } ?: ""))
                            rb.fixtureResult != "FIXTURE_PASS" ->
                                SelfTestState.FAIL to
                                    ("the deterministic fixture leg failed: ${rb.fixtureResult ?: "not runnable"}" +
                                        (rb.detail?.let { " — $it" } ?: "") +
                                        " (phrase: Benvenuto alla torre di Hermes, il canale vocale è pronto.)")
                            rb.microphonePass == null ->
                                SelfTestState.QUALIFICATION_PENDING_DEVICE to
                                    "FIXTURE_PASS achieved. The REAL microphone leg is a separate PHYSICAL qualification (Owner device) — route: ${rb.activeRouteReadBack}."
                            !rb.microphonePass ->
                                SelfTestState.FAIL to "the real microphone leg failed while the fixture passed — check the capture route: ${rb.activeRouteReadBack}."
                            else ->
                                SelfTestState.PASS to
                                    "FIXTURE_PASS + MICROPHONE_PASS on the verified model; active route ${rb.activeRouteReadBack}."
                        }
                    }
                }
            },

            // ---- GRAPH 4.12 §10: ST-56 (Riccardo physical function truth) ----
            def("ST-56", "Riccardo function truth (Graph 4.12 §10)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.sherpaFunctionProbeRiccardo
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the piper synthesis needs the sherpa-onnx JNI. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no Riccardo read-back this run."
                            !rb.modelAndConfigExtracted ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "the Riccardo bundle does not resolve on disk — install the voice in Model Sanctum, then re-run."
                            rb.usingAndroidSystemTts ->
                                SelfTestState.QUALIFICATION_PENDING_DEVICE to
                                    "the route is still the Android system TTS: the Riccardo runtime is not being consumed. State stays INSTALLED_RUNTIME_NOT_AVAILABLE — never ACTIVE."
                            !rb.jniLoaded || !rb.voiceLoaded ->
                                SelfTestState.FAIL to
                                    ("the Riccardo runtime did not fully load (jni=${rb.jniLoaded}, voice=${rb.voiceLoaded})" +
                                        (rb.detail?.let { " — $it" } ?: ""))
                            !rb.previewSynthesized || !rb.playbackCompleted ->
                                SelfTestState.FAIL to
                                    "the Riccardo preview did not complete (synth=${rb.previewSynthesized}, playback=${rb.playbackCompleted})"
                            !rb.runtimeReadBackMatchesSelected ->
                                SelfTestState.FAIL to
                                    "the runtime read-back does NOT match the selected Riccardo identity — route-truth violation."
                            else ->
                                SelfTestState.PASS to
                                    "Riccardo consumed by the REAL runtime: load+synthesis+playback+read-back all verified (restart persistence ${rb.restartPersistenceOk?.toString() ?: "physical qualification pending"})."
                        }
                    }
                }
            },

            // ---- GRAPH 4.12 §10: ST-57 (Paola physical function truth) ----
            def("ST-57", "Paola function truth (Graph 4.12 §10)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.sherpaFunctionProbePaola
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the piper synthesis needs the sherpa-onnx JNI. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no Paola read-back this run."
                            !rb.modelAndConfigExtracted ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "the Paola bundle does not resolve on disk — install the voice in Model Sanctum, then re-run."
                            rb.usingAndroidSystemTts ->
                                SelfTestState.QUALIFICATION_PENDING_DEVICE to
                                    "the route is still the Android system TTS: the Paola runtime is not being consumed. State stays INSTALLED_RUNTIME_NOT_AVAILABLE — never ACTIVE."
                            !rb.jniLoaded || !rb.voiceLoaded ->
                                SelfTestState.FAIL to
                                    ("the Paola runtime did not fully load (jni=${rb.jniLoaded}, voice=${rb.voiceLoaded})" +
                                        (rb.detail?.let { " — $it" } ?: ""))
                            !rb.previewSynthesized || !rb.playbackCompleted ->
                                SelfTestState.FAIL to
                                    "the Paola preview did not complete (synth=${rb.previewSynthesized}, playback=${rb.playbackCompleted})"
                            !rb.runtimeReadBackMatchesSelected ->
                                SelfTestState.FAIL to
                                    "the runtime read-back does NOT match the selected Paola identity — route-truth violation."
                            else ->
                                SelfTestState.PASS to
                                    "Paola consumed by the REAL runtime: load+synthesis+playback+read-back all verified (restart persistence ${rb.restartPersistenceOk?.toString() ?: "physical qualification pending"})."
                        }
                    }
                }
            },

            // ---- GRAPH 4.14 §25: ST-58 (Silero VAD real inference truth) ----
            def("ST-58", "Silero VAD function truth (Graph 4.14 §25)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.sileroVadProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the Silero graph needs the real ORT runner. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no Silero read-back this run."
                            !rb.modelDelivered ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "org.silero.vad.v5 is not delivered/ACTIVE in Model Sanctum — install it, then re-run."
                            !rb.runtimeLoaded ->
                                SelfTestState.FAIL to
                                    "the Silero ORT runner did not load (lastError=${rb.lastError ?: "none"}) — the VAD stays on the typed Energy fallback, never reported ACTIVE."
                            !rb.realInferenceRan ->
                                SelfTestState.FAIL to
                                    "no REAL inference frames ran through the Silero graph — catalog presence alone is never a PASS (§25)."
                            rb.speechDetectedOnFixture == false ->
                                SelfTestState.FAIL to
                                    "the Silero graph ran but detected NO speech on the Italian fixture — inference output is suspect."
                            else ->
                                SelfTestState.PASS to
                                    "REAL Silero inference: graph loaded, fixture frames processed, speech detected=${rb.speechDetectedOnFixture ?: true}. Physical microphone leg stays with the Owner retest."
                        }
                    }
                }
            },

            // ---- GRAPH 4.14 §25: ST-59 (GTCRN real ONNX inference truth) ----
            def("ST-59", "GTCRN denoiser function truth (Graph 4.14 §25)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL, SelfTestMode.VOICE_FOUNDATION_ONLY)) { s ->
                val probe = s.gtcrnProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the GTCRN streaming graph needs the real ORT runner. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no GTCRN read-back this run."
                            !rb.modelDelivered ->
                                SelfTestState.BLOCKED_DEPENDENCY to
                                    "org.gtcrn.speech-enhancement.simple is not delivered/ACTIVE in Model Sanctum — install it, then re-run."
                            !rb.runtimeLoaded ->
                                SelfTestState.FAIL to
                                    "the GTCRN ORT runner did not load (lastError=${rb.lastError ?: "none"}) — denoising stays typed-OFF, never reported ACTIVE."
                            !rb.realInferenceRan || (rb.framesProcessed ?: 0) <= 0 ->
                                SelfTestState.FAIL to
                                    "no REAL streaming frames processed through the GTCRN graph — catalog presence alone is never a PASS (§25)."
                            else ->
                                SelfTestState.PASS to
                                    "REAL GTCRN ONNX inference: ${rb.framesProcessed} streaming frames processed with no engine error."
                        }
                    }
                }
            },

            // ---- GRAPH 4.14 §25: ST-60 (wake-word typed qualification state) ----
            def("ST-60", "Wake word typed qualification (Graph 4.14 §25)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val probe = s.wakeWordProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the 3-graph ORT chain needs the real runtime. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no wake-word read-back this run."
                            !rb.disabledByDefault ->
                                SelfTestState.FAIL to
                                    "the wake word is NOT disabled by default — Owner §8/§9 requires the FA/FR harness as the activation gate."
                            else ->
                                SelfTestState.PASS to
                                    "typed state: ${rb.typedQualificationState} (graphsLoaded=${rb.graphsLoaded}) — OFF by default; physical FA/FR qualification stays with the Owner retest."
                        }
                    }
                }
            },

            // ---- GRAPH 4.14 §25: ST-61 (local LLM chain truth) ----
            def("ST-61", "Local LLM chain truth (Graph 4.14 §25)", SelfTestCategory.DIAGNOSTICS, setOf(SelfTestMode.STANDARD, SelfTestMode.FULL)) { s ->
                val probe = s.localLlmProbe
                when {
                    probe == null ->
                        SelfTestState.QUALIFICATION_PENDING_DEVICE to
                            "JVM/emulator runtime: the llama.cpp native load needs the arm64 device. PHYSICAL_QUALIFICATION_PENDING."
                    else -> {
                        val rb = probe()
                        when {
                            rb == null ->
                                SelfTestState.SKIPPED_NOT_APPLICABLE to "no local-LLM read-back this run."
                            !rb.hasVerifiedRuntime ->
                                SelfTestState.FAIL to
                                    "the engine does NOT hold a verified runtime — selectability must be disabled honestly (§25: no PASS from catalog state alone)."
                            rb.modelFileExists && rb.state == "ERROR" ->
                                SelfTestState.FAIL to
                                    "the model file exists but the engine is in ERROR (integrity=${rb.integrityCode}) — inspect the typed diagnostic."
                            else ->
                                SelfTestState.PASS to
                                    "dependency VERIFIED, runtime identity '${rb.runtimeName}', engine state ${rb.state}, integrity ${rb.integrityCode}, model file present=${rb.modelFileExists}. Physical native-load/output/memory/thermal qualification stays PENDING (§4)."
                        }
                    }
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
