package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * GRAPH 3 WAVE 1 — lapidary display typography (techno-Hellenic capitalis).
 * On-device display face is the platform serif with wide letterspacing until a
 * licensed display face is bundled (ASSET_PIPELINE_DECISION.md); body stays the
 * platform sans for readability. All identity text is also exposed to TalkBack
 * through contentDescription at usage sites.
 */
object MonumentalType {
    val serif: FontFamily = FontFamily.Serif
    val sans: FontFamily = FontFamily.Default

    /** HERMES TOWER frieze title. */
    val friezeTitle = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        letterSpacing = 7.sp,
    )

    /** VOICE ORACLE CONSOLE subtitle / architectural subtitles. */
    val friezeSubtitle = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 9.sp,
    )

    /** Engraved seal labels. */
    val sealLabel = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp,
    )

    /** Tiny seal sublabels. */
    val sealSub = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 2.2.sp,
    )

    /** Inscriptions on the core (ΗΕΡΜΗΣ ΟΡΚΕΛΟΣ, state label). */
    val inscription = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 4.sp,
    )

    val stateLabel = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 5.sp,
    )

    val body = TextStyle(fontFamily = sans, fontSize = 15.sp, letterSpacing = 0.2.sp)
    val bodySecondary = TextStyle(fontFamily = sans, fontSize = 13.5.sp, letterSpacing = 0.2.sp)
    val caption = TextStyle(fontFamily = sans, fontSize = 14.sp, letterSpacing = 0.4.sp)
    val medallionLabel = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.8.sp,
    )
}
