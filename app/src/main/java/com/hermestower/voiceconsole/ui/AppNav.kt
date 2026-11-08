package com.hermestower.voiceconsole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.screens.AboutSafetyScreen
import com.hermestower.voiceconsole.ui.screens.ArchiveScreen
import com.hermestower.voiceconsole.ui.screens.AppearanceScreen
import com.hermestower.voiceconsole.ui.screens.ConsoleScreen
import com.hermestower.voiceconsole.ui.screens.ConnectionScreen
import com.hermestower.voiceconsole.ui.screens.DiagnosticsScreen
import com.hermestower.voiceconsole.ui.screens.HistoryScreen
import com.hermestower.voiceconsole.ui.screens.LocalIntelligenceScreen
import com.hermestower.voiceconsole.ui.screens.ModelManagerScreen
import com.hermestower.voiceconsole.ui.screens.OnboardingScreen
import com.hermestower.voiceconsole.ui.screens.Onboarding
import com.hermestower.voiceconsole.ui.screens.SelfTestScreen
import com.hermestower.voiceconsole.ui.screens.QrScannerScreen
import com.hermestower.voiceconsole.ui.screens.SettingsHubScreen
import com.hermestower.voiceconsole.ui.screens.TowerStudioScreen
import com.hermestower.voiceconsole.ui.screens.VoiceAudioScreen
import com.hermestower.voiceconsole.ui.nav.DrawerWidthPolicy
import com.hermestower.voiceconsole.ui.theme.hermesColors
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import java.util.Locale

private data class Destination(val route: String, val labelRes: Int, val icon: ImageVector)

private val destinations = listOf(
    Destination(AppRoutes.CONSOLE, com.hermestower.voiceconsole.R.string.dest_console, Icons.Outlined.Waves),
    Destination(AppRoutes.HISTORY, com.hermestower.voiceconsole.R.string.dest_history, Icons.Outlined.History),
    Destination(AppRoutes.ARCHIVE, com.hermestower.voiceconsole.R.string.dest_archive, Icons.Outlined.History),
    Destination(AppRoutes.CONNECTION, com.hermestower.voiceconsole.R.string.dest_connection, Icons.Outlined.Link),
    Destination(AppRoutes.TOWER_STUDIO, com.hermestower.voiceconsole.R.string.dest_connect, Icons.Outlined.Router),
    Destination(AppRoutes.VOICE, com.hermestower.voiceconsole.R.string.dest_voice_audio, Icons.Outlined.Mic),
    Destination(AppRoutes.MODEL_MANAGER, com.hermestower.voiceconsole.R.string.dest_models, Icons.Outlined.Memory),
    Destination(AppRoutes.INTELLIGENCE, com.hermestower.voiceconsole.R.string.dest_intelligence, Icons.Outlined.Psychology),
    Destination(AppRoutes.APPEARANCE, com.hermestower.voiceconsole.R.string.dest_appearance, Icons.Outlined.Palette),
    Destination(AppRoutes.DIAGNOSTICS, com.hermestower.voiceconsole.R.string.dest_diagnostics, Icons.Outlined.BugReport),
    Destination(AppRoutes.SELF_TEST, com.hermestower.voiceconsole.R.string.dest_selftest, Icons.Outlined.FactCheck),
    Destination(AppRoutes.ABOUT, com.hermestower.voiceconsole.R.string.dest_about, Icons.Outlined.Info),
    Destination(AppRoutes.SETTINGS, com.hermestower.voiceconsole.R.string.dest_settings, Icons.Outlined.Settings),
)

/** CP-G4-017 (directive §5): the drawer exposes ONLY the five primary
 *  destinations. Every technical destination is reached through the Settings
 *  hub (GENERAL / VOICE / TOWER CONNECTION / MODELS / DISPLAY / DIAGNOSTICS),
 *  which keeps the ordinary product feeling like a dedicated Hermes console
 *  rather than a developer control panel. The compact side-panel policy
 *  (G4-C07: 60% preferred, 2/3 hard max, 248–360dp) is unchanged. */
private val primaryDestinations = listOf(
    AppRoutes.CONSOLE, AppRoutes.ARCHIVE, AppRoutes.TOWER_STUDIO,
    AppRoutes.MODEL_MANAGER, AppRoutes.SETTINGS,
)

private fun subtitleFor(route: String): Int = when (route) {
    AppRoutes.CONSOLE -> com.hermestower.voiceconsole.R.string.subtitle_console
    AppRoutes.HISTORY -> com.hermestower.voiceconsole.R.string.subtitle_history
    AppRoutes.ARCHIVE -> com.hermestower.voiceconsole.R.string.subtitle_archive
    AppRoutes.CONNECTION -> com.hermestower.voiceconsole.R.string.subtitle_connection
    AppRoutes.TOWER_STUDIO -> com.hermestower.voiceconsole.R.string.subtitle_tower_studio
    AppRoutes.VOICE -> com.hermestower.voiceconsole.R.string.subtitle_voice
    AppRoutes.MODEL_MANAGER -> com.hermestower.voiceconsole.R.string.subtitle_model_manager
    AppRoutes.INTELLIGENCE -> com.hermestower.voiceconsole.R.string.subtitle_intelligence
    AppRoutes.APPEARANCE -> com.hermestower.voiceconsole.R.string.subtitle_appearance
    AppRoutes.DIAGNOSTICS -> com.hermestower.voiceconsole.R.string.subtitle_diagnostics
    AppRoutes.SELF_TEST -> com.hermestower.voiceconsole.R.string.subtitle_self_test
    AppRoutes.ABOUT -> com.hermestower.voiceconsole.R.string.subtitle_about
    AppRoutes.SETTINGS -> com.hermestower.voiceconsole.R.string.subtitle_settings
    else -> com.hermestower.voiceconsole.R.string.subtitle_console
}

/**
 * Navigation shell (FR-30..FR-37) — GRAPH 3 temple navigation chamber.
 * Functional titles are preserved verbatim (searchable, TalkBack-friendly);
 * architectural names are subtitles. Console is home; the console supplies its
 * own command frieze, other screens carry the compact bronze top bar.
 */
@Composable
fun AppNav(graph: AppGraph) {
    // GRAPH 4 directive §6: first-launch onboarding gate — chrome-free wizard
    // (no drawer, no NavHost) until the Owner finishes or the flag is preset
    // (the E2E harness presets it so the documented console anchors hold).
    val appContext = androidx.compose.ui.platform.LocalContext.current
    var onboardingDone by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(Onboarding.isDone(appContext))
    }
    // GRAPH 4.12 §5: the ONE-SHOT navigation token — a second onboarding
    // completion callback can never navigate again (the proven crash class:
    // `NavBackStackEntry ... INITIALIZED → DESTROYED` under a racing
    // navigation + recreation window). A pending route that survives a locale
    // recreation is consumed from COMMITTED storage after RESUMED.
    val oneShotNavigationToken = remember {
        java.util.concurrent.atomic.AtomicBoolean(false)
    }
    var pendingRoute by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    if (!onboardingDone) {
        OnboardingScreen(graph) { route ->
            val result = com.hermestower.voiceconsole.onboarding.OnboardingCompletion.execute(
                oneShotNavigationToken = oneShotNavigationToken,
                validate = { null }, // the six-step wizard holds its own required state
                persistOnboardingCompleted = { Onboarding.markDone(appContext) },
                persistLanguage = {
                    // Language is persisted at selection time (AppLanguage.apply);
                    // the session flag is cleared on completion.
                    com.hermestower.voiceconsole.onboarding.OnboardingCompletion.clearSessionFlags(appContext)
                },
                persistConnectionMode = {
                    graph.keyValueStore.putString(
                        com.hermestower.voiceconsole.onboarding.OnboardingCompletion.CONNECTION_MODE_KEY,
                        "persisted-by-onboarding-steps",
                    )
                },
                persistVoiceMode = {
                    graph.keyValueStore.putString(
                        com.hermestower.voiceconsole.onboarding.OnboardingCompletion.VOICE_MODE_KEY,
                        "persisted-by-onboarding-steps",
                    )
                },
                cancelOnboardingCoroutines = { /* the composition-scoped scope dies with the gate flip */ },
                stopOnboardingCollectors = { /* collectAsState collectors die with the composition */ },
                localeRecreationPending = {
                    com.hermestower.voiceconsole.onboarding.OnboardingCompletion
                        .isLocaleChangedThisSession(appContext)
                },
                navigation = { deferredRoute -> pendingRoute = deferredRoute },
                pendingRoute = route,
                storePendingRoute = { r ->
                    navLog("onboarding completion: storePendingRoute=$r")
                    com.hermestower.voiceconsole.onboarding.OnboardingCompletion.setPendingRoute(appContext, r)
                },
                clearPendingRoute = {
                    com.hermestower.voiceconsole.onboarding.OnboardingCompletion.clearPendingRoute(appContext)
                },
            )
            // Flip the gate regardless of the one-shot outcome: a duplicate
            // callback may not navigate, but the wizard must still close.
            onboardingDone = true
        }
        return
    }
    val m = monumentalColors
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoutes.CONSOLE
    val current = destinations.firstOrNull { it.route == currentRoute } ?: destinations.first()
    val scope = rememberCoroutineScope()

    // Directive §6: honor the onboarding choices (QR scan / Models) right after
    // the wizard closes, then clear the pending route.
    // GRAPH 4.12 §5: the navigation runs ONLY after the host is RESUMED and
    // EXACTLY ONCE. A route that survived a locale recreation is read from
    // committed storage (the in-memory pendingRoute dies with the old
    // composition). `lifecycle.withResumed` suspends here until RESUMED, so a
    // recreation in flight can never catch a half-initialized back-stack entry.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute
            ?: com.hermestower.voiceconsole.onboarding.OnboardingCompletion.consumePendingRoute(appContext)
            ?: return@LaunchedEffect
        pendingRoute = null
        navLog("pendingRoute effect: consumed route=$route host=${lifecycleOwner.lifecycle.currentState}")
        // §5: run the navigation only while RESUMED — a recreate in flight can
        // never catch a half-initialized back-stack entry.
        lifecycleOwner.lifecycle.withResumed {
            navLog("pendingRoute effect: RESUMED gate passed, navigate($route)")
            navController.navigate(route) { launchSingleTop = true }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // G4-C07 (OWNER-DELTA-02): compact width = hard 2/3 maximum, 60%
            // preferred, 248–360dp practical bounds. The remaining screen stays
            // visibly present; outside tap and back gesture dismiss as before.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val drawerWidth = DrawerWidthPolicy.drawerWidthDp(this@BoxWithConstraints.maxWidth.value)
                ModalDrawerSheet(
                    drawerContainerColor = m.obsidian,
                    modifier = Modifier.fillMaxWidth(drawerWidth / this@BoxWithConstraints.maxWidth.value),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                    ) {
                        // ---- HEADER: sigil, name, connection state, close ----
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("HERMES TOWER", style = MonumentalType.friezeTitle, color = m.gold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(com.hermestower.voiceconsole.R.string.nav_close_drawer), tint = m.bronzeLight)
                            }
                        }
                        Text(
                            "NAVIGATION CHAMBER",
                            style = MonumentalType.friezeSubtitle,
                            color = m.inkSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        val connection by graph.store.connection.collectAsStateWithLifecycle()
                        Row(
                            Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        when (connection.status) {
                                            com.hermestower.voiceconsole.core.state.ConnectionStatus.BridgeStatus.CONNECTED -> m.cyanCore
                                            com.hermestower.voiceconsole.core.state.ConnectionStatus.BridgeStatus.CONNECTING -> m.amber
                                            else -> m.redBronze
                                        },
                                        androidx.compose.foundation.shape.CircleShape,
                                    ),
                            )
                            Text(
                                "  " + stringResource(com.hermestower.voiceconsole.R.string.nav_bridge_state, connection.status.name.lowercase()),
                                style = MonumentalType.sealSub,
                                color = m.inkSecondary,
                            )
                        }
                        HorizontalDivider(color = m.bronzeDeep, modifier = Modifier.padding(top = 10.dp))

                        // ---- PRIMARY destinations (the only drawer section; CP-G4-017) ----
                        Text(stringResource(com.hermestower.voiceconsole.R.string.nav_primary), style = MonumentalType.sealSub, color = m.gold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        // CP-G4-017: selecting a destination closes the drawer
                        // (Material modal-drawer behavior; also keeps the real-frame
                        // CI path physically sound — the hub is never behind a scrim).
                        primaryDestinations.forEach { route -> DrawerRow(route, currentRoute, m, compact = true) { navLog("drawer row navigate route=$route"); scope.launch { drawerState.close() }; navController.navigate(route) { launchSingleTop = true } } }
                        HorizontalDivider(color = m.bronzeDeep, modifier = Modifier.padding(top = 8.dp))

                        // ---- FOOTER: version, mode, language (§11) ----
                        val requestedMode by graph.store.requestedMode.collectAsStateWithLifecycle()
                        Text(
                            "v${com.hermestower.voiceconsole.app.AppGraph.APP_VERSION} · mode ${requestedMode.label} · lang ${Locale.getDefault().displayLanguage}",
                            style = MonumentalType.sealSub,
                            color = m.inkDim,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                        )
                        Text(
                            stringResource(com.hermestower.voiceconsole.R.string.nav_truth_note),
                            style = MonumentalType.bodySecondary,
                            color = m.inkSecondary,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(m.obsidianDeep)
                // CP-G4-012a (Owner field report): with edge-to-edge enabled the
                // content drew UNDER the system status bar (clock/signal) and the
                // gesture bar. safeDrawing keeps the full-bleed obsidian background
                // (applied above) while clearing status bar, cutout, navigation bar
                // and IME — the keyboard-open chat-focused layout (§18) comes free.
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // compact architectural top bar (console draws its own frieze instead)
            if (currentRoute != AppRoutes.CONSOLE) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A0E16), m.obsidianDeep))),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(com.hermestower.voiceconsole.R.string.nav_open_drawer), tint = m.bronzeLight)
                        }
                        Column {
                            Text(stringResource(current.labelRes), style = MonumentalType.friezeTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified), color = m.ink)
                            Text(
                                stringResource(subtitleFor(currentRoute)),
                                style = MonumentalType.sealSub,
                                color = m.gold,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(androidx.compose.ui.graphics.Color.Transparent, m.bronzeEdge, androidx.compose.ui.graphics.Color.Transparent),
                                ),
                            ),
                    )
                }
            }
            NavHost(
                navController = navController,
                startDestination = AppRoutes.CONSOLE,
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                composable(AppRoutes.CONSOLE) {
                    ConsoleScreen(
                        graph,
                        onOpenHistory = { navController.navigate(AppRoutes.HISTORY) { launchSingleTop = true } },
                        onOpenNavigation = { scope.launch { drawerState.open() } },
                    )
                }
                composable(AppRoutes.HISTORY) { HistoryScreen(graph) }
                composable(AppRoutes.ARCHIVE) { ArchiveScreen(graph.towerArchive) }
                composable(AppRoutes.CONNECTION) { ConnectionScreen(graph, onOpenStudio = { navController.navigate(AppRoutes.TOWER_STUDIO) { launchSingleTop = true } }) }
                composable(AppRoutes.TOWER_STUDIO) { TowerStudioScreen(graph, onOpenScanner = { navController.navigate(AppRoutes.QR_SCANNER) { launchSingleTop = true } }) }
                composable(AppRoutes.QR_SCANNER) { QrScannerScreen(graph, onBack = { navController.popBackStack() }) }
                composable(AppRoutes.VOICE) { VoiceAudioScreen(graph) }
                composable(AppRoutes.SETTINGS) { SettingsHubScreen(onOpen = { route -> navLog("hub row navigate route=$route"); navController.navigate(route) { launchSingleTop = true } }) }
                composable(AppRoutes.MODEL_MANAGER) { ModelManagerScreen(graph) }
                composable(AppRoutes.INTELLIGENCE) { LocalIntelligenceScreen(graph) }
                composable(AppRoutes.APPEARANCE) { AppearanceScreen(graph) }
                composable(AppRoutes.DIAGNOSTICS) { DiagnosticsScreen(graph, onOpenStudio = { navController.navigate(AppRoutes.TOWER_STUDIO) { launchSingleTop = true } }, onOpenSelfTest = { navController.navigate(AppRoutes.SELF_TEST) { launchSingleTop = true } }) }
                composable(AppRoutes.SELF_TEST) { SelfTestScreen(graph) }
                composable(AppRoutes.ABOUT) { AboutSafetyScreen(graph) }
            }
        }
    }
}

@Composable
private fun DrawerRow(
    route: String,
    currentRoute: String,
    m: com.hermestower.voiceconsole.ui.monumental.MonumentalColors,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val d = destinations.first { it.route == route }
    val selected = currentRoute == route
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .defaultMinSize(minHeight = 48.dp) // 48dp interaction target (§11)
            .background(
                if (selected) m.obsidianRaised else m.obsidianPanel,
                RoundedCornerShape(8.dp),
            )
            .border(1.5.dp, if (selected) m.gold else m.bronzeDeep, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            d.icon,
            contentDescription = null,
            tint = if (selected) m.cyanCore else m.inkSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            stringResource(d.labelRes),
            style = MonumentalType.sealLabel,
            color = if (selected) m.ink else m.inkSecondary,
            maxLines = 2, // two-line layout for long labels (de/fr) (§11)
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.nav_current_chip),
                style = MonumentalType.sealSub,
                color = m.gold,
            )
        }
    }
}

/**
 * R-45b diagnostics (debug-only, additive): typed navigation provenance for the
 * destroy-time `NavBackStackEntry INITIALIZED → DESTROYED` crash class. Every
 * app-side pusher of a NavController.navigate() call logs its route here, so
 * the next fleet sample carries the pusher evidence in the logcat artifact.
 * Zero behavior change; the tag is HermesNav.
 */
internal fun navLog(message: String) {
    if (com.hermestower.voiceconsole.BuildConfig.DEBUG) {
        android.util.Log.i("HermesNav", message)
    }
}
