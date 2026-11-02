package com.hermestower.voiceconsole.ui.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * GRAPH 4 NODE 0 — dedicated frame-timing state controller (CP-G4-002).
 *
 * Replaces the `produceState(0L, …) { while (true) { withFrameNanos { value = … } } }`
 * pattern in OracleCore/TowerCore that Android Lint could not statically verify
 * (`ProduceStateDoesNotAssignValue`): the state assignment lived inside a nested
 * frame-callback lambda, invisible to lint's producer analysis. Runtime behavior is
 * preserved exactly while the mechanism is now lifecycle-safe by construction:
 *
 *  - `LaunchedEffect(running)` — Compose guarantees the effect coroutine is cancelled
 *    when the composable leaves composition and when `running` flips, so no orphan
 *    frame loop can survive disposal (animation work STOPS off-composition);
 *  - `mutableLongStateOf` — allocation-free Long state, one invalidation per frame;
 *  - the frame-callback lambda is hoisted out of the loop — zero per-frame allocation;
 *  - `running == false` (reduced motion, LOW quality) resets to 0 ms, producing the
 *    same static frame as before (OLED idle policy unchanged);
 *  - the clock is deliberately NOT keyed on console state: state changes update the
 *    animation parameters through the draw path while time stays monotonic, so
 *    transitions no longer reset the animation phase mid-motion (Graph 3's
 *    produceState keyed on `state` restarted the phase at every state change — an
 *    invisible defect this controller removes; frozen/reduced states are derived
 *    inside the state models, which the existing model tests pin).
 */
@Composable
fun rememberAnimationFrameTimeMs(running: Boolean): Long {
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        pumpAnimationFrameTime(running) { frameMs -> clock.longValue = frameMs }
    }
    return clock.longValue
}

/**
 * Frame pump extracted from the composable so the loop logic is unit-testable on
 * the JVM without a full UI composition. Cancelling the calling coroutine stops all
 * frame work; [running] == false emits exactly one reset to 0 ms and completes.
 */
internal suspend fun pumpAnimationFrameTime(
    running: Boolean,
    emit: (Long) -> Unit,
) {
    if (!running) {
        emit(0L)
        return
    }
    val startNanos = withFrameNanos { it }
    // Hoisted callback: no allocation inside the hot loop.
    val tick: (Long) -> Unit = { now -> emit((now - startNanos) / 1_000_000L) }
    while (true) {
        withFrameNanos(tick)
    }
}
