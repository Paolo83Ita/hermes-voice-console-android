package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * GRAPH 3 — Monumental Oracle Tower token system (WAVE 1).
 * Materials, energy roles and text colors of the authorized visual direction.
 * Source of truth: docs/MONUMENTAL_ORACLE_TOWER_VISUAL_SYSTEM.md and the approved
 * preview pack (design/monumental/preview/). Supersedes V0.1 minimal direction for
 * visual identity only; functional semantics are untouched.
 */
@Immutable
data class MonumentalColors(
    // obsidian structure
    val obsidianDeep: Color,
    val obsidian: Color,
    val obsidianPanel: Color,
    val obsidianRaised: Color,
    val obsidianSheen: Color,
    // carved stone
    val stoneDeep: Color,
    val stone: Color,
    val stoneMid: Color,
    val stoneEdge: Color,
    val stoneLit: Color,
    // aged bronze + gold
    val bronzeDeep: Color,
    val bronze: Color,
    val bronzeEdge: Color,
    val bronzeLight: Color,
    val gold: Color,
    val goldBright: Color,
    // marble (selective accents only)
    val marble: Color,
    // energy roles
    val cyanCore: Color,
    val cyan: Color,
    val cyanDeep: Color,
    val cyanAbyss: Color,
    val towerBlue: Color,
    val towerBlueDeep: Color,
    val localGold: Color,
    val localCyan: Color,
    val amber: Color,
    val redBronze: Color,
    val redBronzeDeep: Color,
    // text
    val ink: Color,
    val inkSecondary: Color,
    val inkDim: Color,
)

/** The single authorized dark identity (mission §9; OLED-first). */
val MonumentalObsidian = MonumentalColors(
    obsidianDeep = Color(0xFF04060A),
    obsidian = Color(0xFF080B12),
    obsidianPanel = Color(0xFF0D1119),
    obsidianRaised = Color(0xFF131824),
    obsidianSheen = Color(0xFF16202E),
    stoneDeep = Color(0xFF191611),
    stone = Color(0xFF221D15),
    stoneMid = Color(0xFF2C261B),
    stoneEdge = Color(0xFF3A3225),
    stoneLit = Color(0xFF4A4130),
    bronzeDeep = Color(0xFF3E2F18),
    bronze = Color(0xFF6B5127),
    bronzeEdge = Color(0xFF8A6A38),
    bronzeLight = Color(0xFFB98C4A),
    gold = Color(0xFFD9B36A),
    goldBright = Color(0xFFF0D9A0),
    marble = Color(0xFFEFE7D8),
    cyanCore = Color(0xFF35E0FF),
    cyan = Color(0xFF1FC2E8),
    cyanDeep = Color(0xFF0E7E9C),
    cyanAbyss = Color(0xFF0A3E52),
    towerBlue = Color(0xFF4C8DFF),
    towerBlueDeep = Color(0xFF2C5BD9),
    localGold = Color(0xFFE8C87B),
    localCyan = Color(0xFF7CE7D8),
    amber = Color(0xFFE8A33D),
    redBronze = Color(0xFFC4553B),
    redBronzeDeep = Color(0xFF7E3222),
    ink = Color(0xFFEDE6D8),
    inkSecondary = Color(0xFFA89F8D),
    inkDim = Color(0xFF6E675C),
)

val LocalMonumentalColors = staticCompositionLocalOf { MonumentalObsidian }

val monumentalColors: MonumentalColors
    @Composable get() = LocalMonumentalColors.current
