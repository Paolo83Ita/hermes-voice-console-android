package com.hermestower.voiceconsole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.screens.AboutSafetyScreen
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
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination(AppRoutes.CONSOLE, "Voice Console", Icons.Outlined.Waves),
    Destination(AppRoutes.HISTORY, "Conversation History", Icons.Outlined.History),
    Destination(AppRoutes.CONNECTION, "Connection", Icons.Outlined.Link),
    Destination(AppRoutes.TOWER_STUDIO, "Hermes Tower Connection", Icons.Outlined.Router),
    Destination(AppRoutes.VOICE, "Voice & Audio", Icons.Outlined.Mic),
    Destination(AppRoutes.MODEL_MANAGER, "Voice Models", Icons.Outlined.Memory),
    Destination(AppRoutes.INTELLIGENCE, "Local Intelligence", Icons.Outlined.Psychology),
    Destination(AppRoutes.APPEARANCE, "Appearance", Icons.Outlined.Palette),
    Destination(AppRoutes.DIAGNOSTICS, "Diagnostics", Icons.Outlined.BugReport),
    Destination(AppRoutes.SELF_TEST, "Self-Test Center", Icons.Outlined.FactCheck),
    Destination(AppRoutes.ABOUT, "About & Safety", Icons.Outlined.Info),
)

private val chamberGroups: List<Pair<String, List<String>>> = listOf(
    "SANCTUM" to listOf(AppRoutes.CONSOLE, AppRoutes.HISTORY),
    "TOWER & MODELS" to listOf(AppRoutes.CONNECTION, AppRoutes.TOWER_STUDIO, AppRoutes.MODEL_MANAGER, AppRoutes.INTELLIGENCE),
    "MECHANISM" to listOf(AppRoutes.VOICE, AppRoutes.SELF_TEST, AppRoutes.DIAGNOSTICS, AppRoutes.APPEARANCE, AppRoutes.ABOUT),
)

private fun subtitleFor(route: String): String = when (route) {
    AppRoutes.CONSOLE -> "ORACLE CORE"
    AppRoutes.HISTORY -> "ARCHIVE CHAMBER"
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
            ModalDrawerSheet(drawerContainerColor = m.obsidian) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Single distinct string: an exact-text "HERMES" node here would
                    // duplicate the console frieze node (Graph 2 E2E finding, run
                    // 41341134806 — always-composed drawer sheet breaks onNodeWithText).
                    Text("HERMES TOWER", style = MonumentalType.friezeTitle, color = m.gold, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        "NAVIGATION CHAMBER",
                        style = MonumentalType.friezeSubtitle,
                        color = m.inkSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    chamberGroups.forEach { (groupName, routes) ->
                        Text(
                            groupName,
                            style = MonumentalType.sealSub,
                            color = m.gold,
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 6.dp)
                                .fillMaxWidth(),
                        )
                        routes.forEach { route ->
                            val d = destinations.first { it.route == route }
                            val selected = currentRoute == route
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(
                                        if (selected) m.obsidianRaised else m.obsidianPanel,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .border(
                                        1.5.dp,
                                        if (selected) m.gold else m.bronzeDeep,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { navController.navigate(d.route) { launchSingleTop = true } }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    d.icon,
                                    contentDescription = null,
                                    tint = if (selected) m.cyanCore else m.inkSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.size(12.dp))
                                Text(
                                    d.label,
                                    style = MonumentalType.sealLabel,
                                    color = if (selected) m.ink else m.inkSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Text("CURRENT", style = MonumentalType.sealSub, color = m.gold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                    Text(
                        "Every state shown is the real state of the console.",
                        style = MonumentalType.bodySecondary,
                        color = m.inkSecondary,
                    )
                    Text(
                        "VOICE ORACLE CONSOLE — V0.3 · MONUMENTAL REFOUNDATION",
                        style = MonumentalType.sealSub,
                        color = m.inkDim,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    )
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
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation drawer", tint = m.bronzeLight)
                        }
                        Column {
                            Text(current.label, style = MonumentalType.friezeTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified), color = m.ink)
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
