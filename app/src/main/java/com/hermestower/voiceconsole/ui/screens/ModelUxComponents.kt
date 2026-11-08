package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.models.CompatVerdict
import com.hermestower.voiceconsole.models.CompatVerdictKind
import com.hermestower.voiceconsole.models.ModelPrimaryAction
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.HermesColors
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * GRAPH 4.13 §14-§23 — MODEL LIBRARY UX COMPONENTS (Block 2 rebuild).
 *
 * Presentation only: the model authority (catalog, registry, receipts,
 * download lifecycle, capability state machine, profiles, rollback) is reused
 * unchanged; no second registry exists. Every text consumes the §6 semantic
 * tokens; raw enums never render here (StateLabels + typed resolvers only).
 */

/** Spacing scale §23: 4 micro / 8 compact / 12 ordinary / 16 section / 24 major. */
object ModelUxSpacing {
    const val MICRO = 4
    const val COMPACT = 8
    const val ORDINARY = 12
    const val SECTION = 16
    const val MAJOR = 24
}

// ---------------- §14 configuration card rows ----------------

@Composable
private fun ConfigRow(label: String, value: String, stacked: Boolean) {
    if (stacked) {
        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(label, style = MonumentalType.sealSub, color = hermesColors.textTertiaryReadable)
            Text(value, style = HermesType.body, color = hermesColors.textPrimary)
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = HermesType.bodySecondary, color = hermesColors.textSecondary, modifier = Modifier.weight(0.42f))
            Text(value, style = HermesType.body, color = hermesColors.textPrimary, modifier = Modifier.weight(0.58f))
        }
    }
}

/**
 * §14 — the compact, readable recommended-configuration card. Two-column
 * key-value rows where width permits, stacked rows on compact widths; no
 * large empty lower region; the hardware identifier never shows (§25).
 */
@Composable
fun RecommendedConfigurationCard(
    deviceClass: String,
    androidVersion: String,
    architecture: String,
    availableRam: String,
    freeStorage: String,
    speechEngine: String,
    network: String,
    currentProfile: String,
    recommendedProfile: String,
) {
    val width = LocalConfiguration.current.screenWidthDp
    val stacked = width < 600
    val c = hermesColors
    SectionCard(title = stringResource(R.string.mm_section_a)) {
        ConfigRow(stringResource(R.string.mm_cfg_deviceclass), deviceClass, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_android), androidVersion, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_arch), architecture, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_ram), availableRam, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_storage), freeStorage, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_engine), speechEngine, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_network), network, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_profile_current), currentProfile, stacked)
        ConfigRow(stringResource(R.string.mm_cfg_profile_recommended), recommendedProfile, stacked)
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .heightIn(min = 2.dp)
                .background(c.outlineSecondary),
        )
    }
}

// ---------------- §15/§16 profile selector ----------------

data class ProfileCardData(
    val profile: VoiceModelProfile,
    val title: String,
    val body: String,
    val requirements: String,
    val selected: Boolean,
    val enabled: Boolean,
    /** §16: honest unavailability text + "Why unavailable" — never dim-only. */
    val unavailableExplanation: String? = null,
)

@Composable
private fun ProfileCardItem(data: ProfileCardData, onSelect: () -> Unit) {
    val c = hermesColors
    val bg = when {
        !data.enabled -> c.surfaceDisabled
        data.selected -> c.surfaceSelected
        else -> c.surfaceInteractive
    }
    val border = when {
        !data.enabled -> c.outlineDisabled
        data.selected -> c.outlineFocus
        else -> c.outlinePrimary
    }
    val titleColor = when {
        !data.enabled -> c.textDisabled
        data.selected -> c.textOnSelected
        else -> c.textPrimary
    }
    val bodyColor = if (!data.enabled) c.textDisabled else c.textSecondary
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(Brush.linearGradient(listOf(bg, bg)), RoundedCornerShape(10.dp))
            .border(BorderStroke(2.dp, border), RoundedCornerShape(10.dp))
            .clickable(enabled = data.enabled, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(ModelUxSpacing.MICRO.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.title, style = HermesType.sectionLabel, color = titleColor, modifier = Modifier.weight(1f, fill = false))
            if (data.selected) {
                Text(stringResource(R.string.mm_profile_selected), style = MonumentalType.sealSub, color = c.textOnSelected)
            }
        }
        Text(data.body, style = HermesType.bodySecondary, color = bodyColor)
        Text(
            stringResource(R.string.mm_profile_requirements_fmt, data.requirements),
            style = MonumentalType.sealSub,
            color = if (!data.enabled) c.textDisabled else c.textTertiaryReadable,
        )
        if (!data.enabled && data.unavailableExplanation != null) {
            Text(data.unavailableExplanation, style = HermesType.bodySecondary, color = c.textWarning)
        }
    }
}

/**
 * §15 — the three model profiles NEVER share one row on compact devices:
 * 360–411 dp renders vertical segmented cards (one full-width row per
 * profile); 600 dp+ allows up to three columns. No horizontal scrolling,
 * no letter-column wrapping ("Local experimental" stays a normal label).
 */
@Composable
fun ProfileSelector(
    cards: List<ProfileCardData>,
    onSelect: (VoiceModelProfile) -> Unit,
) {
    val width = LocalConfiguration.current.screenWidthDp
    val columnsAllowed = width >= 600 && cards.size >= 3
    if (columnsAllowed) {
        Column(verticalArrangement = Arrangement.spacedBy(ModelUxSpacing.COMPACT.dp)) {
            cards.chunked(3).forEach { rowCards ->
                Row(horizontalArrangement = Arrangement.spacedBy(ModelUxSpacing.COMPACT.dp)) {
                    rowCards.forEach { card ->
                        Box(Modifier.weight(1f)) { ProfileCardItem(card) { onSelect(card.profile) } }
                    }
                    repeat(3 - rowCards.size) { Box(Modifier.weight(1f)) {} }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ModelUxSpacing.COMPACT.dp)) {
            cards.forEach { card ->
                ProfileCardItem(card) { onSelect(card.profile) }
            }
        }
    }
}

// ---------------- §17 model card ----------------

/** §19 — the typed compatibility line, plain-language localized. */
@Composable
fun compatVerdictLabel(verdict: CompatVerdict): String = when (verdict.kind) {
    CompatVerdictKind.COMPATIBLE -> stringResource(R.string.mm_compat_ok)
    CompatVerdictKind.IN_USE -> stringResource(R.string.mm_compat_ok)
    CompatVerdictKind.INACTIVE_READY -> stringResource(R.string.mm_compat_inactive)
    CompatVerdictKind.NOT_DOWNLOADED -> stringResource(R.string.mm_compat_not_downloaded)
    CompatVerdictKind.DOWNLOAD_BLOCKED -> stringResource(R.string.mm_compat_download_needed)
    CompatVerdictKind.RUNTIME_UNAVAILABLE -> stringResource(R.string.mm_compat_runtime_unavailable)
    CompatVerdictKind.VERIFICATION_REQUIRED -> stringResource(R.string.mm_compat_verification_required)
    CompatVerdictKind.INSTALL_READY -> stringResource(R.string.mm_compat_ok)
    CompatVerdictKind.QUALIFICATION_PENDING -> stringResource(R.string.mm_compat_qualification_pending)
    CompatVerdictKind.QUARANTINED -> stringResource(R.string.mm_action_review_issue)
    CompatVerdictKind.FAILED -> stringResource(R.string.mm_action_repair)
    CompatVerdictKind.INCOMPATIBLE -> stringResource(R.string.mm_compat_incompatible_fmt, verdict.detail ?: "—")
    CompatVerdictKind.INSUFFICIENT_MEMORY -> stringResource(R.string.mm_compat_insufficient_memory)
}

/** §21 — the single localized label for a card's primary action. */
@Composable
fun primaryActionLabel(action: ModelPrimaryAction): String = when (action) {
    ModelPrimaryAction.DOWNLOAD -> stringResource(R.string.mm_download)
    ModelPrimaryAction.VERIFY -> stringResource(R.string.mm_verify)
    ModelPrimaryAction.INSTALL -> stringResource(R.string.mm_install)
    ModelPrimaryAction.CHECK_RUNTIME -> stringResource(R.string.mm_action_check_runtime)
    ModelPrimaryAction.RUN_FUNCTIONAL_TEST -> stringResource(R.string.mm_action_run_functional_test)
    ModelPrimaryAction.ACTIVATE -> stringResource(R.string.mm_activate)
    ModelPrimaryAction.TEST -> stringResource(R.string.mm_test)
    ModelPrimaryAction.REPAIR -> stringResource(R.string.mm_action_repair)
    ModelPrimaryAction.REVIEW_ISSUE -> stringResource(R.string.mm_action_review_issue)
    ModelPrimaryAction.DETAILS_ONLY -> stringResource(R.string.mm_action_open_details)
}

/** Compatibility row color: readable status semantics (§4 critical status ≥4.5). */
@Composable
fun compatVerdictColor(verdict: CompatVerdict, c: HermesColors) = when (verdict.kind) {
    CompatVerdictKind.COMPATIBLE, CompatVerdictKind.IN_USE, CompatVerdictKind.INSTALL_READY -> c.textSuccess
    CompatVerdictKind.QUARANTINED, CompatVerdictKind.FAILED, CompatVerdictKind.INCOMPATIBLE,
    CompatVerdictKind.INSUFFICIENT_MEMORY, CompatVerdictKind.RUNTIME_UNAVAILABLE,
    -> c.textError
    else -> c.textWarning
}

/** §23 — status pill with icon glyph + localized text (never a raw enum). */
@Composable
fun ModelStatePill(rawState: String) {
    val c = hermesColors
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(6.dp),
        color = c.surfaceInteractive,
        border = BorderStroke(1.dp, c.outlinePrimary),
    ) {
        Text(
            com.hermestower.voiceconsole.locale.StateLabels.label(rawState, java.util.Locale.getDefault().toLanguageTag()),
            style = MonumentalType.sealSub,
            color = c.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
