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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.hermestower.voiceconsole.ui.screens.SelfTestScreen
import com.hermestower.voiceconsole.ui.screens.TowerStudioScreen
import com.hermestower.voiceconsole.ui.screens.VoiceAudioScreen
import com.hermestower.voiceconsole.ui.nav.DrawerWidthPolicy
import com.hermestower.voiceconsole.ui.theme.hermesColors
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
)

/** GRAPH 4 G4-C07 (Owner delta §11): primary then secondary destinations.
 *  CP-G4-011 (§14): Tower Archive joins PRIMARY. */
private val primaryDestinations = listOf(
    AppRoutes.CONSOLE, AppRoutes.ARCHIVE, AppRoutes.TOWER_STUDIO,
    AppRoutes.MODEL_MANAGER, AppRoutes.SELF_TEST,
)
private val secondaryDestinations = listOf(
    AppRoutes.HISTORY, AppRoutes.CONNECTION, AppRoutes.VOICE,
    AppRoutes.INTELLIGENCE, AppRoutes.APPEARANCE, AppRoutes.DIAGNOSTICS, AppRoutes.ABOUT,
)

private fun subtitleFor(route: String): String = when (route) {
    AppRoutes.CONSOLE -> "ORACLE CORE"
    AppRoutes.HISTORY -> "CONSOLE MEMORY"
    AppRoutes.ARCHIVE -> "ARCHIVE CHAMBER"
    AppRoutes.CONNECTION -> "TOWER GATE"
    AppRoutes.TOWER_STUDIO -> "TOWER GATE · TRANSPORT CHAMBER"
    AppRoutes.VOICE -> "RESONANCE CHAMBER"
    AppRoutes.MODEL_MANAGER -> "ORACLE LIBRARY"
    AppRoutes.INTELLIGENCE -> "LOCAL CORE CHAMBER"
    AppRoutes.APPEARANCE -> "ARCHITECTURAL FORGE"
    AppRoutes.DIAGNOSTICS -> "MECHANISM INSPECTION"
    AppRoutes.SELF_TEST -> "TRIAL CHAMBER"
    AppRoutes.ABOUT -> "FOUNDATION CHAMBER"
    else -> ""
}

/**
 * Navigation shell (FR-30..FR-37) — GRAPH 3 temple navigation chamber.
 * Functional titles are preserved verbatim (searchable, TalkBack-friendly);
 * architectural names are subtitles. Console is home; the console supplies its
 * own command frieze, other screens carry the compact bronze top bar.
 */
@Composable
fun AppNav(graph: AppGraph) {
    val m = monumentalColors
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoutes.CONSOLE
    val current = destinations.firstOrNull { it.route == currentRoute } ?: destinations.first()
    val scope = rememberCoroutineScope()

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

                        // ---- PRIMARY destinations ----
                        Text(stringResource(com.hermestower.voiceconsole.R.string.nav_primary), style = MonumentalType.sealSub, color = m.gold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        primaryDestinations.forEach { route -> DrawerRow(route, currentRoute, m, compact = true) { navController.navigate(route) { launchSingleTop = true } } }
                        HorizontalDivider(color = m.bronzeDeep, modifier = Modifier.padding(top = 8.dp))

                        // ---- SECONDARY destinations ----
                        Text(stringResource(com.hermestower.voiceconsole.R.string.nav_secondary), style = MonumentalType.sealSub, color = m.inkSecondary, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                        secondaryDestinations.forEach { route -> DrawerRow(route, currentRoute, m, compact = true) { navController.navigate(route) { launchSingleTop = true } } }
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
                .background(m.obsidianDeep),
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
                            Text(subtitleFor(currentRoute), style = MonumentalType.sealSub, color = m.gold)
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
                composable(AppRoutes.TOWER_STUDIO) { TowerStudioScreen(graph) }
                composable(AppRoutes.VOICE) { VoiceAudioScreen(graph) }
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
            Text("CURRENT", style = MonumentalType.sealSub, color = m.gold)
        }
    }
}
