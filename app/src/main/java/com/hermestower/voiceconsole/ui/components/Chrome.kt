package com.hermestower.voiceconsole.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
    val m = monumentalColors
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(6.dp),
        color = m.obsidianPanel,
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
            Text(label, style = MonumentalType.sealLabel, color = m.inkSecondary)
        }
    }
}

/** Mode seal; selected carries the lit gold identity (was: mode chip). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = monumentalColors
    val bg = if (selected) m.stoneDeep else m.obsidianPanel
    val fg = if (selected) m.gold else m.inkSecondary
    val border = if (selected) m.gold else m.bronzeDeep
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

/** Architectural panel (was: generic section card). Bronze-edged obsidian. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = monumentalColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(listOf(m.obsidianRaised, m.obsidianPanel, Color(0xFF0A0D14))),
                    RoundedCornerShape(10.dp),
                )
                .border(1.5.dp, m.bronzeEdge, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(title.uppercase(), style = MonumentalType.sealLabel, color = m.gold)
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(width = 34.dp, height = 2.dp)
                            .background(m.bronzeEdge),
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

/** Engraved truth badge (INACTIVE IN V0.1, SIMULATED, ...). */
@Composable
fun TruthBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val m = monumentalColors
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
