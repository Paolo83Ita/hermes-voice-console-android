package com.hermestower.voiceconsole.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * GRAPH 4.10 CP-G4.10-009 (§22 RESPONSIVE ACTION GROUP) — the ONE action-row
 * authority for Connection Studio, Model Sanctum, Voice and Audio and the
 * Self-Test Center.
 *
 * §21/§22 rules (unit-pinned at the layout level, Owner-repair P0-08):
 *  - COMPACT: primary full-width; secondary/management stacked (one per row);
 *    destructive stacked below a divider;
 *  - MEDIUM: primary full-width; up to two short secondary actions per row;
 *    management actions wrap only when measured text fits;
 *  - LARGE: max three actions per row;
 *  - minimum 48 dp height; no fixed narrow width; max two lines; centered
 *    text; no icon overlap; everything scroll-reachable.
 */
data class ResponsiveAction(
    val key: String,
    val label: String,
    val onClick: () -> Unit,
    val filled: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun ResponsiveActionGroup(
    widthClass: ConsoleWindowWidthClass,
    primary: ResponsiveAction? = null,
    secondary: List<ResponsiveAction> = emptyList(),
    management: List<ResponsiveAction> = emptyList(),
    destructive: List<ResponsiveAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val c = hermesColors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        primary?.let { p ->
            Button(
                onClick = p.onClick,
                enabled = p.enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    p.label,
                    style = HermesType.body,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ActionRows(widthClass, secondary)
        if (management.isNotEmpty()) {
            ActionRows(widthClass, management)
        }
        if (destructive.isNotEmpty()) {
            HorizontalDivider(color = c.bronze, modifier = Modifier.padding(vertical = 2.dp))
            ActionRows(widthClass, destructive)
        }
    }
}

@Composable
private fun ActionRows(widthClass: ConsoleWindowWidthClass, actions: List<ResponsiveAction>) {
    if (actions.isEmpty()) return
    val perRow = widthClass.maxActionsPerRow()
    val c = hermesColors
    actions.chunked(perRow).forEach { rowActions ->
        if (perRow == 1) {
            rowActions.forEach { a ->
                TextButton(
                    onClick = a.onClick,
                    enabled = a.enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        a.label,
                        style = HermesType.bodySecondary,
                        color = c.inkSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowActions.forEach { a ->
                    TextButton(
                        onClick = a.onClick,
                        enabled = a.enabled,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            a.label,
                            style = HermesType.bodySecondary,
                            color = c.inkSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
