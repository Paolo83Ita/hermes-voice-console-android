package com.hermestower.voiceconsole.ui.nav

/**
 * GRAPH 4 G4-C07 (OWNER-DELTA-02): compact navigation drawer width policy.
 *
 * Binding Owner requirement: on a portrait phone the primary side menu must
 * never exceed two thirds of the available screen width.
 *
 *   drawerWidth = min(max(screenWidth × 0.60, 248dp), min(screenWidth × 0.6667, 360dp))
 *
 * When the screen is too narrow to satisfy both limits the priority is:
 * hard two-thirds maximum → accessible content (vertical scroll, two-line
 * labels) — never a full-screen drawer. Landscape/tablets get the compact
 * fixed 360dp cap (≤ two thirds of any screen wider than 540dp), leaving the
 * active content area visibly present. The function is pure so the whole
 * required width matrix (360/393/411dp, landscape, narrow screens) is
 * unit-testable without instrumentation.
 */
object DrawerWidthPolicy {
    const val TARGET_RATIO = 0.60f
    const val HARD_MAX_RATIO = 2f / 3f // exact two-thirds (Owner hard maximum)
    const val MIN_PRACTICAL_DP = 248f
    const val MAX_PRACTICAL_DP = 360f

    fun drawerWidthDp(screenWidthDp: Float): Float {
        val preferred = maxOf(screenWidthDp * TARGET_RATIO, MIN_PRACTICAL_DP)
        val cap = minOf(screenWidthDp * HARD_MAX_RATIO, MAX_PRACTICAL_DP)
        return if (preferred <= cap) preferred else cap
    }
}
