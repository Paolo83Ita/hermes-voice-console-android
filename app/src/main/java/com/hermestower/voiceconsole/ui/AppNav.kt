package com.hermestower.voiceconsole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.ModeChip
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
import com.hermestower.voiceconsole.ui.theme.HermesType
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

/** Navigation shell (FR-30..FR-37): drawer over all eight surfaces, console is home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(graph: AppGraph) {
    val c = hermesColors
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AppRoutes.CONSOLE
    val current = destinations.firstOrNull { it.route == currentRoute } ?: destinations.first()
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("HERMES", style = HermesType.brand, color = c.bronze, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        "VOICE CONSOLE — V0.2",
                        style = HermesType.sectionLabel,
                        color = c.inkSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    MeanderBand(colors = c, modifier = Modifier.padding(vertical = 10.dp))
                    destinations.forEach { d ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                d.icon,
                                contentDescription = null,
                                tint = if (currentRoute == d.route) c.energy else c.inkSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.size(12.dp))
                            ModeChip(
                                label = d.label,
                                selected = currentRoute == d.route,
                                enabled = true,
                                onClick = {
                                    navController.navigate(d.route) { launchSingleTop = true }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                    Text(
                        "Every state shown is the real state of the console.",
                        style = HermesType.bodySecondary,
                        color = c.inkSecondary,
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = c.background,
            topBar = {
                TopAppBar(
                    title = { Text(current.label, style = HermesType.screenTitle, color = c.ink) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation drawer", tint = c.bronze)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background),
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppRoutes.CONSOLE,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(AppRoutes.CONSOLE) { ConsoleScreen(graph, onOpenHistory = { navController.navigate(AppRoutes.HISTORY) { launchSingleTop = true } }) }
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
