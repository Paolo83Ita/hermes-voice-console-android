package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R

/**
 * GRAPH 3 WAVE 4 — shared architectural chrome. Replaces the generic Material
 * expression of chips/pills/cards/buttons while preserving behavior, touch
 * targets (≥48 dp) and TalkBack semantics.
 */

/** Carved-stone / bronze-edged obsidian panel (replaces SectionCard visuals). */
@Composable
fun ArchitecturalPanel(
    title: String,
    accent: Color,
    colors: MonumentalColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .background(
                Brush.linearGradient(
                    listOf(colors.obsidianRaised, colors.obsidianPanel, Color(0xFF0A0D14)),
                ),
                RoundedCornerShape(10.dp),
            )
            .border(1.5.dp, accent, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◤ ", color = accent.copy(alpha = 0.9f), style = MonumentalType.sealLabel)
            Text(
                title,
                style = MonumentalType.sealLabel,
                color = accent,
                modifier = Modifier.weight(1f),
            )
            Text(" ◥", color = accent.copy(alpha = 0.9f), style = MonumentalType.sealLabel)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** Engraved status seal (replaces StatusPill). */
@Composable
fun StatusSeal(
    label: String,
    sublabel: String,
    accent: Color,
    active: Boolean,
    colors: MonumentalColors,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val bg = if (active) colors.stoneDeep else colors.obsidianPanel
    val border = if (active) accent else colors.bronzeDeep
    val glow = if (active) accent.copy(alpha = 0.28f) else Color.Transparent
    Column(
        modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.5.dp, border, RoundedCornerShape(6.dp))
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(glow, CircleShape)
                .border(1.dp, if (active) accent else colors.inkDim, CircleShape),
        )
        Text(label, style = MonumentalType.sealLabel, color = if (active) accent else colors.inkDim)
        Text(sublabel, style = MonumentalType.sealSub, color = colors.inkDim)
    }
}

/** Command frieze mode seal (replaces ModeChip on the console). */
@Composable
fun ModeSeal(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    colors: MonumentalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (selected) colors.gold else colors.bronzeEdge
    // semantics {} is not composable — resolve the localized a11y string here.
    val modeCd = stringResource(R.string.chrome_mode_cd_fmt, label) +
        if (selected) stringResource(R.string.chrome_selected_suffix) else ""
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) colors.stoneDeep else colors.obsidianPanel,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) accent else colors.bronzeDeep),
        modifier = modifier.semantics {
            contentDescription = modeCd
        },
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MonumentalType.sealLabel, color = if (selected) colors.gold else colors.inkSecondary)
            Text(stringResource(R.string.chrome_mode_label), style = MonumentalType.sealSub, color = colors.inkDim)
        }
    }
}

/**
 * Circular engraved command medallion (replaces bordered IconButtons).
 * Guaranteed ≥48 dp touch target; [size] is the visual diameter (56/72/88 dp).
 */
@Composable
fun ControlMedallion(
    label: String,
    icon: ImageVector,
    iconDescription: String,
    accent: Color,
    colors: MonumentalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    active: Boolean = false,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(size)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.obsidianRaised, colors.obsidianPanel, Color(0xFF070A10)),
                        ),
                        CircleShape,
                    )
                    .border(
                        width = if (active) 2.5.dp else 1.5.dp,
                        color = if (active) accent else colors.bronzeEdge,
                        shape = CircleShape,
                    )
                    .semantics { contentDescription = iconDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(size / 2.4f))
            }
            Text(label, style = MonumentalType.medallionLabel, color = colors.inkSecondary)
        }
    }
}

/** Amber suspended-scroll queue seal (REQUEST_QUEUED, real count). */
@Composable
fun QueueSeal(count: Int, colors: MonumentalColors, modifier: Modifier = Modifier) {
    if (count <= 0) return
    // semantics {} is not composable — resolve the localized plural here.
    val queuedCd = pluralStringResource(R.plurals.chrome_queued_cd, count, count)
    Column(
        modifier
            .background(colors.stoneDeep, RoundedCornerShape(8.dp))
            .border(2.dp, colors.amber, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics {
                contentDescription = queuedCd
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.chrome_queued_fmt, count), style = MonumentalType.sealLabel, color = colors.amber)
        Text(stringResource(R.string.chrome_scroll_suspended), style = MonumentalType.sealSub, color = colors.inkDim)
    }
}

/** Red-bronze privacy gate note (PRIVACY_MUTED — unmistakable mic state). */
@Composable
fun PrivacyGateNote(colors: MonumentalColors, modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.chrome_privacy_gate),
        style = MonumentalType.sealLabel,
        color = colors.redBronze,
        modifier = modifier.semantics {
            contentDescription = stringResource(R.string.chrome_privacy_cd)
        },
    )
}

/** Frieze base rule (double gold line under the command frieze). */
@Composable
fun FriezeBase(colors: MonumentalColors, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, colors.bronzeEdge, Color.Transparent),
                ),
            ),
    )
}
