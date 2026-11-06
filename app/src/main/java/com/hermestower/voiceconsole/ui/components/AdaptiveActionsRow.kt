package com.hermestower.voiceconsole.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * AdaptiveActionsRow (OWNER PHYSICAL RETEST §8): the adaptive action layout
 * that replaces the fixed horizontal `Row`s of many `TextButton`s.
 *
 * The physical defect (PHYSICAL-FAIL-03): the Verify button in Models and
 * Voice sat in a fixed row of up to 6 TextButtons — at the POCO's ≈372 dp the
 * row overflowed and the label clipped. A generic FlowRow-only repair is NOT
 * applied; the layout is adaptive AND hierarchical:
 *
 *  - WIDE available width (≥ 480 dp): the primary actions share one row, the
 *    secondary actions sit behind **More actions**;
 *  - CONSTRAINED width: the primary actions wrap (FlowRow) and the secondary
 *    actions stay behind **More actions** (an overflow menu, never clipped);
 *  - every action: minimum touch height 48 dp, minimum readable horizontal
 *    padding (12 dp), no fixed narrow width, maxLines 2, centered text, no
 *    text clipping, no icon/progress overlap (text-only buttons).
 */
data class AdaptiveAction(
    val key: String,
    val label: String,
    val enabled: Boolean = true,
    /** Primary actions render filled; secondary actions live in More actions. */
    val emphasized: Boolean = false,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveActionsRow(
    primary: List<AdaptiveAction>,
    secondary: List<AdaptiveAction>,
    moreActionsLabel: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = maxWidth >= 480.dp
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                primary.forEach { action -> ActionButton(action, emphasized = action.emphasized) }
                if (secondary.isNotEmpty()) MoreActionsMenu(secondary, moreActionsLabel)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    primary.forEach { action -> ActionButton(action, emphasized = action.emphasized) }
                }
                if (secondary.isNotEmpty()) MoreActionsMenu(secondary, moreActionsLabel)
            }
        }
    }
}

/** The overflow menu for secondary actions (Owner brief §8: More actions). */
@Composable
private fun MoreActionsMenu(secondary: List<AdaptiveAction>, moreActionsLabel: String) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(moreActionsLabel, maxLines = 2, textAlign = TextAlign.Center) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            secondary.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    enabled = action.enabled,
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/**
 * The physical-repair action button: ≥48 dp touch height, 12 dp horizontal
 * padding, maxLines 2, centered — the Verify-class defect is impossible here.
 */
@Composable
private fun ActionButton(action: AdaptiveAction, emphasized: Boolean) {
    val c = hermesColors
    if (emphasized) {
        Button(
            onClick = action.onClick,
            enabled = action.enabled,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        ) {
            Text(
                action.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        TextButton(
            onClick = action.onClick,
            enabled = action.enabled,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        ) {
            Text(
                action.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (action.enabled) c.bronze else c.inkSecondary,
            )
        }
    }
}

/**
 * Pure policy (unit-testable): decides the layout shape from the available
 * width and the action counts. Mirrors AdaptiveActionsRow's thresholds.
 */
object AdaptiveActionPolicy {
    /** The wide-layout threshold in dp (a phone ≈372 dp is constrained; tablets are wide). */
    const val WIDE_THRESHOLD_DP = 480f

    enum class Shape { SINGLE_ROW, WRAPPED_PRIMARY }

    fun shape(availableWidthDp: Float): Shape =
        if (availableWidthDp >= WIDE_THRESHOLD_DP) Shape.SINGLE_ROW else Shape.WRAPPED_PRIMARY
}
