package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Token layer (decision D-010): system families now, bundled OFL typefaces later.
 * Serif carries the Word (brand, captions); sans carries the body; mono carries status.
 */
object HermesType {

    val serif: FontFamily = FontFamily.Serif
    val sans: FontFamily = FontFamily.Default
    val mono: FontFamily = FontFamily.Monospace

    val brand = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 4.6.sp,
    )

    val screenTitle = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 1.4.sp,
    )

    val sectionLabel = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.8.sp,
    )

    val stateChip = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.4.sp,
    )

    val body = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val bodySecondary = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    )

    val caption = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Normal,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )

    val pill = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.2.sp,
    )
}

val HermesTypography = Typography(
    displaySmall = HermesType.brand,
    titleLarge = HermesType.screenTitle,
    titleMedium = HermesType.screenTitle,
    bodyLarge = HermesType.body,
    bodyMedium = HermesType.bodySecondary,
    labelSmall = HermesType.pill,
)
