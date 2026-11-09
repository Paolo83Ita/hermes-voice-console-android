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
    // GRAPH 4.12 §19: the TEXT variant of the red-bronze identity — the seal
    // / border keep redBronze; ordinary small TEXT on dark panels uses this
    // raised variant (≥4.5:1 on every obsidian surface, PHYS-FAIL-02 repair).
    val redBronzeText: Color,
    // text
    val ink: Color,
    val inkSecondary: Color,
    // GRAPH 4.12 §18/§19: textMuted (formerly 0xFF6E675C at 3.17–3.63:1 —
    // a measured WCAG failure on every ordinary label that used it).
    // 0xFF8A8375 measures ≥4.72:1 on every obsidian surface.
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
    redBronzeText = Color(0xFFDE7A5E),
    ink = Color(0xFFEDE6D8),
    inkSecondary = Color(0xFFA89F8D),
    inkDim = Color(0xFF8A8375),
)

/**
 * GRAPH 4.14 §15/§17/§18 — the MARBLE Light monumental identity.
 *
 * FINDING-04/05 repair: the shell/chrome/stage are no longer hard-pinned to
 * obsidian. In Marble Light the STAGE is ivory/pale warm stone, panels are
 * light, chrome gradients are light, ink is dark — while the bronze/cyan
 * energy identity of the Tower is preserved (the tower artwork itself may
 * stay dark on a light stage, with a controlled local halo — §18).
 * Every text/icon token is chosen ≥4.5:1 (text) / ≥3:1 (icons) on the light
 * surfaces it hosts; measured pairs live in
 * MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.json.
 */
val MonumentalMarble = MonumentalColors(
    // stage surfaces: ivory / pale warm stone (light equivalents of the obsidian family)
    obsidianDeep = Color(0xFFF1EBDD),
    obsidian = Color(0xFFEAE3D2),
    obsidianPanel = Color(0xFFF8F4EA),
    obsidianRaised = Color(0xFFFFFBF1),
    obsidianSheen = Color(0xFFE3DBC7),
    // carved stone: light warm neutrals
    stoneDeep = Color(0xFFDCD2B8),
    stone = Color(0xFFE6DEC9),
    stoneMid = Color(0xFFEFE8D6),
    stoneEdge = Color(0xFFC4B897),
    stoneLit = Color(0xFF9C8E6C),
    // aged bronze + gold: deepened for light-surface contrast (text-capable)
    bronzeDeep = Color(0xFF6E5424),
    bronze = Color(0xFF7A5C2E),
    bronzeEdge = Color(0xFF947739),
    bronzeLight = Color(0xFF8F6B33),
    gold = Color(0xFF6E5420),
    goldBright = Color(0xFF54400F),
    marble = Color(0xFFEFE7D8),
    // energy roles: deepened cyan/amber/red for ≥3:1 icons and ≥4.5:1 text on ivory
    cyanCore = Color(0xFF0085A8),
    cyan = Color(0xFF0083A8),
    cyanDeep = Color(0xFF0B5E76),
    cyanAbyss = Color(0xFF124B5E),
    towerBlue = Color(0xFF2C5BD9),
    towerBlueDeep = Color(0xFF1E3FA8),
    localGold = Color(0xFF7A5C1E),
    localCyan = Color(0xFF0B6E62),
    amber = Color(0xFF7E4A00),
    redBronze = Color(0xFF9E3A22),
    redBronzeDeep = Color(0xFF6E2818),
    redBronzeText = Color(0xFF8F3420),
    // text: dark ink family on light surfaces
    ink = Color(0xFF241F14),
    inkSecondary = Color(0xFF554E3E),
    inkDim = Color(0xFF665E4C),
)

val LocalMonumentalColors = staticCompositionLocalOf { MonumentalObsidian }

val monumentalColors: MonumentalColors
    @Composable get() = LocalMonumentalColors.current
