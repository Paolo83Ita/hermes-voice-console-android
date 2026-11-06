package com.hermestower.voiceconsole.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * GRAPH 4.10 CP-G4.10-008 (§20 RESPONSIVE MONUMENTAL HEADER) — the ONE screen
 * header authority for the compact-phone repair (Xiaomi 11 Lite 5G NE).
 *
 * Rules:
 *  - COMPACT (available width < 380 dp): reduced title font (20 sp), reduced
 *    letter spacing, maximum two title lines, compact subtitle, NO fixed
 *    height (the header grows only as much as its text needs);
 *  - font scale 1.30 or greater: the monumental tracking is REMOVED (normal
 *    tracking), vertical growth is allowed and the next panel is never overlapped;
 *  - the menu button is preserved and the title measures the width remaining
 *    after the menu icon;
 *  - statusBarsPadding applied.
 */
@Composable
fun ResponsiveScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onMenu: (() -> Unit)? = null,
    menuContentDescription: String? = null,
) {
    val c = hermesColors
    BoxWithConstraints(modifier.fillMaxWidth().statusBarsPadding()) {
        val widthClass = consoleWindowWidthClass(maxWidth.value)
        val fontScale = LocalDensity.current.fontScale
        val largeFont = fontScale >= 1.30f
        val compact = widthClass == ConsoleWindowWidthClass.COMPACT
        val titleSize = when {
            largeFont -> 22.sp // vertical growth with readable tracking
            compact -> 20.sp   // reduced title font at COMPACT
            widthClass == ConsoleWindowWidthClass.MEDIUM -> 24.sp
            else -> 28.sp
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onMenu != null) {
                IconButton(onClick = onMenu) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = menuContentDescription,
                        tint = c.bronze,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    title,
                    style = HermesType.screenTitle.copy(
                        fontSize = titleSize,
                        letterSpacing = if (largeFont) 0.sp else (if (compact) 0.6.sp else 1.2.sp),
                        lineHeight = titleSize * 1.15f,
                    ),
                    color = c.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = HermesType.bodySecondary.copy(
                            fontSize = if (compact || largeFont) 12.sp else 13.sp,
                        ),
                        color = c.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
