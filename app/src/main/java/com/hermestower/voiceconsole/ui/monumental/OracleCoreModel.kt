package com.hermestower.voiceconsole.ui.monumental

import com.hermestower.voiceconsole.core.state.ConsoleState
import kotlin.math.cos
import kotlin.math.sin

/**
 * GRAPH 3 WAVE 2 — pure, unit-testable model of the Oracle Core (no Compose).
 * Maps the 15 truthful console states to layer-driving parameters. The renderer
 * (OracleCore.kt) draws exactly what this model says; reduced motion renders the
 * same parameters at the sample frame. Mirror of the approved preview table
 * (docs/ORACLE_CORE_STATE_SPEC.md §2).
 */
data class CoreVisualParams(
    val portalAlpha: Float,        // L6 portal luminosity budget
    val conduitAlpha: Float,       // L5 energy conduits
    val bronzeBudget: Float,       // L3/L4 material luminance (AMOLED)
    val ringADegrees: Float,       // mechanical ring A sample/rotor phase
    val ringBDegrees: Float,
    val messenger: MessengerMode,
    val dim: Float,                // global state dim
    val expansionWave: Boolean = false,   // WAKE_DETECTED
    val convergeWaves: Boolean = false,   // LISTENING
    val glyphSequenceUpTo: Int = 0,       // TRANSCRIBING sockets lit (0..12)
    val localCorePlate: Boolean = false,  // UNDERSTANDING_LOCAL seal
    val towerLink: Boolean = false,       // transport path visible
    val secondTrail: Boolean = false,     // TOWER_PROCESSING multi-packets
    val amplitudeGlow: Boolean = false,   // SPEAKING reacts to real amplitude
    val queueSeal: Boolean = false,       // REQUEST_QUEUED (real count shown separately)
    val halfShutters: Boolean = false,    // DISCONNECTED portal partially sealed
    val shutters: Boolean = false,        // PRIVACY_MUTED gates fully closed
    val privacyRing: Boolean = false,
    val brokenPath: Boolean = false,
    val dashedConduits: Boolean = false,
    val fracture: Boolean = false,        // ERROR
    val maintenanceSeal: Boolean = false, // UPDATE_AVAILABLE
)

enum class MessengerMode { NONE, LOCAL_ORBIT, DEPARTURE, ARRIVAL, BROKEN, DEGRADED }

object OracleCoreModel {

    fun params(state: ConsoleState): CoreVisualParams = when (state) {
        ConsoleState.DORMANT -> CoreVisualParams(0.10f, 0.06f, 0.30f, 0f, 0f, MessengerMode.NONE, 0.20f)
        ConsoleState.IDLE -> CoreVisualParams(0.26f, 0.24f, 0.55f, 16f, -9f, MessengerMode.NONE, 0.50f)
        ConsoleState.WAKE_DETECTED -> CoreVisualParams(
            0.85f, 0.55f, 0.90f, 6f, -4f, MessengerMode.LOCAL_ORBIT, 0.85f, expansionWave = true,
        )
        ConsoleState.LISTENING -> CoreVisualParams(
            0.72f, 0.50f, 0.85f, 9f, -6f, MessengerMode.LOCAL_ORBIT, 0.85f, convergeWaves = true,
        )
        ConsoleState.TRANSCRIBING -> CoreVisualParams(
            0.55f, 0.40f, 0.75f, 12f, -8f, MessengerMode.NONE, 0.75f, glyphSequenceUpTo = 5,
        )
        ConsoleState.UNDERSTANDING_LOCAL -> CoreVisualParams(
            0.62f, 0.45f, 0.85f, 28f, -14f, MessengerMode.LOCAL_ORBIT, 0.85f, localCorePlate = true,
        )
        ConsoleState.CONTACTING_TOWER -> CoreVisualParams(
            0.60f, 0.55f, 0.85f, 20f, -12f, MessengerMode.DEPARTURE, 0.85f, towerLink = true,
        )
        ConsoleState.TOWER_PROCESSING -> CoreVisualParams(
            0.80f, 0.70f, 0.95f, 63f, -41f, MessengerMode.DEPARTURE, 0.95f,
            towerLink = true, secondTrail = true,
        )
        ConsoleState.SPEAKING -> CoreVisualParams(
            0.92f, 0.55f, 0.90f, 12f, -8f, MessengerMode.ARRIVAL, 0.95f, amplitudeGlow = true,
        )
        ConsoleState.REQUEST_QUEUED -> CoreVisualParams(
            0.42f, 0.30f, 0.70f, 8f, -5f, MessengerMode.NONE, 0.70f, queueSeal = true,
        )
        ConsoleState.DISCONNECTED -> CoreVisualParams(
            0.20f, 0.16f, 0.50f, 6f, -3f, MessengerMode.BROKEN, 0.50f,
            halfShutters = true, brokenPath = true,
        )
        ConsoleState.DEGRADED -> CoreVisualParams(
            0.45f, 0.30f, 0.72f, 14f, -9f, MessengerMode.DEGRADED, 0.70f, dashedConduits = true,
        )
        ConsoleState.ERROR -> CoreVisualParams(
            0.38f, 0.25f, 0.65f, 10f, -6f, MessengerMode.NONE, 0.65f, fracture = true,
        )
        ConsoleState.PRIVACY_MUTED -> CoreVisualParams(
            0.14f, 0.05f, 0.42f, 0f, 0f, MessengerMode.NONE, 0.40f,
            shutters = true, privacyRing = true,
        )
        ConsoleState.UPDATE_AVAILABLE -> CoreVisualParams(
            0.32f, 0.24f, 0.62f, 8f, -5f, MessengerMode.NONE, 0.60f, maintenanceSeal = true,
        )
    }

    /** Continuous rotation phase (degrees) for the mechanical rings at time t (ms). */
    fun ringAPhase(state: ConsoleState, tMs: Long, reducedMotion: Boolean, quality: VisualQuality): Float {
        if (reducedMotion || quality == VisualQuality.LOW) return params(state).ringADegrees
        val secondsPerRev = when (state) {
            ConsoleState.TOWER_PROCESSING -> 8f
            ConsoleState.UNDERSTANDING_LOCAL -> 12f
            else -> 24f
        }
        return params(state).ringADegrees + (tMs % 60_000L) / 1000f * (360f / secondsPerRev)
    }

    fun ringBPhase(state: ConsoleState, tMs: Long, reducedMotion: Boolean, quality: VisualQuality): Float {
        if (reducedMotion || quality == VisualQuality.LOW) return params(state).ringBDegrees
        val secondsPerRev = when (state) {
            ConsoleState.TOWER_PROCESSING -> 14f
            else -> 40f
        }
        return params(state).ringBDegrees - (tMs % 60_000L) / 1000f * (360f / secondsPerRev)
    }

    /** Breathing multiplier for the portal (IDLE and resting states breathe slowly). */
    fun portalBreath(state: ConsoleState, tMs: Long, reducedMotion: Boolean): Float {
        if (reducedMotion) return 1f
        val phase = (tMs % 4_800L) / 4_800f * 2f * Math.PI.toFloat()
        val depth = when (state) {
            ConsoleState.IDLE -> 0.18f
            ConsoleState.DORMANT, ConsoleState.PRIVACY_MUTED, ConsoleState.DISCONNECTED -> 0.08f
            else -> 0.10f
        }
        return 1f + depth * sin(phase)
    }

    /** Converging listening wave radius fraction (0..1 across the three waves). */
    fun convergeFraction(tMs: Long, wave: Int, reducedMotion: Boolean): Float {
        if (reducedMotion) return 0.35f + wave * 0.3f
        val period = 1_800f
        val off = wave * period / 3f
        return ((tMs % 1_800L) + off) % period / period
    }

    fun polar(cx: Float, cy: Float, r: Float, deg: Float): Pair<Float, Float> {
        val a = Math.toRadians(deg.toDouble())
        return Pair(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
    }
}
