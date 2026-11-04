package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.locale.AppLanguage
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.selftest.SelfTestMode
import com.hermestower.voiceconsole.selftest.SelfTestState
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

/**
 * GRAPH 4 directive §6 — SIMPLE ONBOARDING (first launch only).
 *
 * Six steps: language → Tower connect (QR or simulation) → recommended voice
 * pack → optional local LLM (default "Not now") → voice test → finish. The
 * flow is chrome-free (no drawer, no NavHost) and born-localized in the five
 * product locales. Ordinary onboarding NEVER surfaces SHA-256, ABI,
 * quantization, protocol paths, TLS fingerprints, raw endpoints or debug
 * internals (directive §5) — those live in the advanced surfaces behind
 * Settings.
 *
 * The "scan the QR code" choice defers the physical scan to the Tower Gate
 * scanner right after onboarding (the scanner route lives in the NavHost);
 * the choice is honest and labeled. The voice-pack install runs the REAL
 * pack installer and reports its events truthfully — onboarding is not the
 * enforcement point, Models and Voice is.
 */
object Onboarding {
    private const val PREFS = "onboarding"
    private const val KEY = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }

    /** E2E/first-launch control: clears the done flag (debug/test usage only). */
    fun resetForTest(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}

@Composable
fun OnboardingScreen(graph: AppGraph, onFinish: (pendingRoute: String?) -> Unit) {
    val c = hermesColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(0) }
    var towerChoiceQr by rememberSaveable { mutableStateOf(false) }
    var openModelsAfter by rememberSaveable { mutableStateOf(false) }
    var packEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var packRunning by remember { mutableStateOf(false) }
    val selfTestResults by graph.selfTestCenter.results.collectAsState()
    val selfTestRunning by graph.selfTestCenter.running.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.ob_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 2.dp))
        Text(
            stringResource(R.string.ob_step_of_fmt, step + 1, 6),
            style = HermesType.sectionLabel, color = c.bronze,
        )

        when (step) {
            // ---- STEP 1: language ----
            0 -> {
                SectionCard(title = stringResource(R.string.ob_s1_title)) {
                    Text(stringResource(R.string.ob_s1_body), style = HermesType.body, color = c.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FilterChip(
                            selected = AppLanguage.stored(context) == AppLanguage.FOLLOW_SYSTEM,
                            onClick = {
                                AppLanguage.apply(context, AppLanguage.FOLLOW_SYSTEM)
                                (context as? Activity)?.recreate()
                            },
                            label = { Text(stringResource(R.string.ob_s1_follow_system)) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("it" to "Italiano", "en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch")
                            .forEach { (code, label) ->
                                FilterChip(
                                    selected = AppLanguage.stored(context) == code,
                                    onClick = {
                                        AppLanguage.apply(context, code)
                                        (context as? Activity)?.recreate()
                                    },
                                    label = { Text(label) },
                                )
                            }
                    }
                }
                OnboardingContinue(onContinue = { step = 1 })
            }

            // ---- STEP 2: Tower connect ----
            1 -> {
                SectionCard(title = stringResource(R.string.ob_s2_title)) {
                    Text(stringResource(R.string.ob_s2_body), style = HermesType.body, color = c.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { towerChoiceQr = true; step = 2 }) {
                            Text(stringResource(R.string.ob_s2_scan_qr))
                        }
                        OutlinedButton(onClick = { towerChoiceQr = false; step = 2 }) {
                            Text(stringResource(R.string.ob_s2_simulation))
                        }
                    }
                    Text(
                        if (towerChoiceQr) stringResource(R.string.ob_s2_chosen_qr)
                        else stringResource(R.string.ob_s2_chosen_sim),
                        style = HermesType.bodySecondary, color = c.inkSecondary,
                    )
                }
            }

            // ---- STEP 3: recommended voice pack ----
            2 -> {
                val resources = remember { graph.deviceResourceReader.read() }
                val plan = remember { graph.modelPackInstaller.plan(VoiceModelProfile.HYBRID_BALANCED, resources, graph.modelCatalog) }
                val summary = remember { graph.modelPackInstaller.summarize(plan, resources) }
                SectionCard(title = stringResource(R.string.ob_s3_title)) {
                    Text(stringResource(R.string.ob_s3_body), style = HermesType.body, color = c.ink)
                    Text(
                        stringResource(
                            R.string.ob_s3_summary_fmt,
                            plan.includedItems.size,
                            formatBytes(summary.totalDownloadBytes),
                        ),
                        style = HermesType.body, color = c.ink,
                    )
                    plan.includedItems.forEach { item ->
                        Text(
                            "• ${item.entry?.displayName ?: item.componentType}",
                            style = HermesType.bodySecondary, color = c.inkSecondary,
                        )
                    }
                    Text(stringResource(R.string.ob_s3_no_account_needed), style = HermesType.bodySecondary, color = c.inkSecondary)
                    if (plan.includedItems.isNotEmpty()) {
                        Button(
                            enabled = !packRunning,
                            onClick = {
                                packRunning = true
                                val res = graph.deviceResourceReader.read()
                                scope.launch {
                                    val result = graph.modelPackInstaller.installPack(plan, res) { event ->
                                        packEvents = packEvents + event
                                    }
                                    packEvents = packEvents + if (result.success) {
                                        context.getString(R.string.ob_s3_install_ok)
                                    } else {
                                        context.getString(R.string.ob_s3_install_later_note)
                                    }
                                    packRunning = false
                                }
                            },
                        ) { Text(stringResource(R.string.ob_s3_install)) }
                    }
                    packEvents.takeLast(3).forEach {
                        Text(it, style = HermesType.bodySecondary, color = c.energy)
                    }
                }
                OnboardingContinue(onContinue = { step = 3 })
            }

            // ---- STEP 4: optional local LLM ----
            3 -> {
                SectionCard(title = stringResource(R.string.ob_s4_title)) {
                    Text(stringResource(R.string.ob_s4_body), style = HermesType.body, color = c.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { openModelsAfter = false; step = 4 }) {
                            Text(stringResource(R.string.ob_s4_not_now))
                        }
                        OutlinedButton(onClick = { openModelsAfter = true; step = 4 }) {
                            Text(stringResource(R.string.ob_s4_choose_model))
                        }
                    }
                }
            }

            // ---- STEP 5: voice test ----
            4 -> {
                SectionCard(title = stringResource(R.string.ob_s5_title)) {
                    Text(stringResource(R.string.ob_s5_body), style = HermesType.body, color = c.ink)
                    Button(
                        enabled = !selfTestRunning,
                        onClick = { graph.selfTestCenter.run(SelfTestMode.VOICE_FOUNDATION_ONLY) },
                    ) { Text(stringResource(R.string.ob_s5_run)) }
                    val done = selfTestResults.values.filter { it.state != SelfTestState.NOT_RUN && it.state != SelfTestState.RUNNING }
                    if (done.isNotEmpty()) {
                        val passed = done.count { it.state == SelfTestState.PASS }
                        Text(
                            stringResource(R.string.ob_s5_result_fmt, passed, done.size),
                            style = HermesType.body, color = c.ink,
                        )
                        Text(stringResource(R.string.ob_s5_honest_note), style = HermesType.bodySecondary, color = c.inkSecondary)
                    }
                }
                OnboardingContinue(onContinue = { step = 5 })
            }

            // ---- STEP 6: finish ----
            else -> {
                SectionCard(title = stringResource(R.string.ob_s6_title)) {
                    Text(stringResource(R.string.ob_s6_body), style = HermesType.body, color = c.ink)
                }
                Button(onClick = {
                    onFinish(if (openModelsAfter) com.hermestower.voiceconsole.ui.AppRoutes.MODEL_MANAGER else null)
                }) { Text(stringResource(R.string.ob_s6_finish)) }
            }
        }
    }
}

@Composable
private fun OnboardingContinue(onContinue: () -> Unit) {
    TextButton(onClick = onContinue) { Text(stringResource(R.string.ob_continue)) }
}
