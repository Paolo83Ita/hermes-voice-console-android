package com.hermestower.voiceconsole.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * GRAPH 3 WAVE 4 — the V0.1 chrome API (StatusPill / ModeChip / SectionCard /
 * TruthBadge) re-expressed in the Monumental material language. Signatures are
 * preserved so every Graph 2 screen converges visually without functional change.
 */

/** Engraved status seal (was: status pill with truth dot). */
@Composable
fun StatusPill(
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    description: String = label,
) {
    val c = hermesColors // GRAPH 4.13 §6: semantic surfaces/text (theme-aware)
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(6.dp),
        color = c.surfaceInteractive,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, dotColor.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape),
            )
            Text(label, style = MonumentalType.sealLabel, color = c.textSecondary)
        }
    }
}

/** Mode seal; selected carries the lit identity (was: mode chip).
 *
 * GRAPH 4.13 §5 disabled contract: a disabled chip is recognizably disabled
 * with ≥2 cues (color + fill + border) while its label stays readable (≥3.0).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = hermesColors
    val bg = when {
        !enabled -> c.surfaceDisabled
        selected -> c.surfaceSelected
        else -> c.surfaceInteractive
    }
    val fg = when {
        !enabled -> c.textDisabled
        selected -> c.textOnSelected
        else -> c.textSecondary
    }
    val border = when {
        !enabled -> c.outlineDisabled
        selected -> c.outlineFocus
        else -> c.outlineSecondary
    }
    androidx.compose.material3.Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, border),
    ) {
        Text(
            label,
            style = MonumentalType.sealLabel,
            color = fg,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
        )
    }
}

/** Architectural panel (was: generic section card). Identity-edged surface.
 *
 * GRAPH 4.13 §6: surfaces/text come from the semantic authority so the panel
 * follows the active theme (obsidian identity in dark, marble in light).
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = hermesColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(listOf(c.surfaceRaised, c.surfaceInteractive, c.surfaceBase)),
                    RoundedCornerShape(10.dp),
                )
                .border(1.5.dp, c.outlinePrimary, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // UI_POLISH_PASS: the title column takes the REMAINING width so a
                // long localized title can never collide with (or push) the trailing
                // element on narrow screens.
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), style = MonumentalType.sealLabel, color = c.gold)
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(width = 34.dp, height = 2.dp)
                            .background(c.outlinePrimary),
                    )
                }
                if (trailing != null) trailing()
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/**
 * UI_POLISH_PASS (OWNER PHYSICAL FEEDBACK 2): the wrapping badge row. Status/
 * truth badges were placed in fixed horizontal Rows that CANNOT wrap — long
 * Italian/German labels overflowed narrow screens (the Owner's "incorrect text
 * layout" finding). BadgeFlowRow wraps to the next line instead. Signature:
 * content is one badge per call site slot, identical semantics, purely additive.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        content()
    }
}

/** Engraved truth badge (INACTIVE IN V0.1, SIMULATED, ...). */
@Composable
fun TruthBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Text(
            text,
            style = MonumentalType.sealSub,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// =====================================================================
// GRAPH 4.13 §5 — DISABLED-STATE CONTRACT + §6 semantic control colors.
// Disabled controls are recognizably disabled with ≥2 cues (label color,
// fill, border) while labels stay readable (≥3.0). Enabled controls never
// render informative text in disabled colors. Wire these defaults at every
// Material control call site instead of the library defaults (whose 38%
// alpha disabled labels measure 1.4–2.1:1 on obsidian — measured FAIL).
// =====================================================================

/** Filled action button colors (primary action = the only filled control). */
@Composable
fun hermesButtonColors() = ButtonDefaults.buttonColors(
    containerColor = hermesColors.bronze,
    contentColor = hermesColors.textOnAccent,
    disabledContainerColor = hermesColors.surfaceDisabled,
    disabledContentColor = hermesColors.textDisabled,
)

/** Outlined/secondary button colors (cues: outline + fill + label color). */
@Composable
fun hermesOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = hermesColors.textPrimary,
    disabledContentColor = hermesColors.textDisabled,
)

/** Filter chip colors (cues: label color + fill + border via chip border param). */
@Composable
fun hermesFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = hermesColors.surfaceInteractive,
    labelColor = hermesColors.textSecondary,
    selectedContainerColor = hermesColors.surfaceSelected,
    selectedLabelColor = hermesColors.textOnSelected,
    disabledContainerColor = hermesColors.surfaceDisabled,
    disabledLabelColor = hermesColors.textDisabled,
)

/** Switch colors (cues: track fill + thumb; readable state row label outside). */
@Composable
fun hermesSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = hermesColors.bronze,
    checkedThumbColor = hermesColors.textOnAccent,
    uncheckedTrackColor = hermesColors.surfaceInteractive,
    uncheckedThumbColor = hermesColors.iconSecondary,
    uncheckedBorderColor = hermesColors.outlinePrimary,
    disabledCheckedTrackColor = hermesColors.surfaceDisabled,
    disabledUncheckedTrackColor = hermesColors.surfaceDisabled,
    disabledCheckedThumbColor = hermesColors.textDisabled,
    disabledUncheckedThumbColor = hermesColors.textDisabled,
    disabledUncheckedBorderColor = hermesColors.outlineDisabled,
)

/** Text field colors (composer, studio inputs) on semantic surfaces. */
@Composable
fun hermesTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = hermesColors.textPrimary,
    unfocusedTextColor = hermesColors.textPrimary,
    focusedContainerColor = hermesColors.surfaceInteractive,
    unfocusedContainerColor = hermesColors.surfaceInteractive,
    focusedBorderColor = hermesColors.outlineFocus,
    unfocusedBorderColor = hermesColors.outlinePrimary,
    disabledTextColor = hermesColors.textDisabled,
    disabledContainerColor = hermesColors.surfaceDisabled,
    disabledBorderColor = hermesColors.outlineDisabled,
    cursorColor = hermesColors.textInfo,
    focusedLabelColor = hermesColors.textSecondary,
    unfocusedLabelColor = hermesColors.textTertiaryReadable,
)

/** Dense text-field variant for compact rows. */
@Composable
fun hermesDenseTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = hermesColors.textPrimary,
    unfocusedTextColor = hermesColors.textPrimary,
    focusedContainerColor = hermesColors.surfaceInteractive,
    unfocusedContainerColor = hermesColors.surfaceInteractive,
    focusedIndicatorColor = hermesColors.outlineFocus,
    unfocusedIndicatorColor = hermesColors.outlinePrimary,
    disabledTextColor = hermesColors.textDisabled,
    disabledContainerColor = hermesColors.surfaceDisabled,
    disabledIndicatorColor = hermesColors.outlineDisabled,
    cursorColor = hermesColors.textInfo,
)

// ---- GRAPH 4.13 control wrappers (§5 contract wired in one place) ----

/** Filled button — the only filled control per card/screen section. */
@Composable
fun HermesFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = hermesButtonColors(),
    content = content,
)

/** Secondary outline button. */
@Composable
fun HermesOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = hermesOutlinedButtonColors(),
    border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) hermesColors.outlinePrimary else hermesColors.outlineDisabled),
    content = content,
)

/** Filter chip with §5 cues (label + fill + border) on semantic surfaces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = hermesColors
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        colors = hermesFilterChipColors(),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = c.outlinePrimary,
            selectedBorderColor = c.outlineFocus,
            disabledBorderColor = c.outlineDisabled,
            disabledSelectedBorderColor = c.outlineDisabled,
        ),
    )
}

/** Switch with §5 cues (track fill + thumb + border). */
@Composable
fun HermesSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = androidx.compose.material3.Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    colors = hermesSwitchColors(),
)

/** Outlined text field on semantic surfaces. */
@Composable
fun HermesOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) = androidx.compose.material3.OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    placeholder = placeholder,
    enabled = enabled,
    singleLine = singleLine,
    visualTransformation = visualTransformation,
    colors = hermesTextFieldColors(),
)
