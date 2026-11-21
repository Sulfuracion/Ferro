package com.davidguijo.ferro.core.domain

import java.time.Clock
import java.util.Locale

/** Estado persistible del temporizador de descanso basado en marca de tiempo absoluta. */
data class RestTimerState(
    val workoutId: String,
    val exerciseId: String?,
    val startedAtMillis: Long,
    val endAtMillis: Long,
    val totalDurationSeconds: Int
)

/** Cálculos puros del temporizador de descanso con [Clock] inyectado (nunca por ticks). */
object RestTimer {
    fun start(workoutId: String, exerciseId: String?, durationSeconds: Int, clock: Clock): RestTimerState? {
        if (durationSeconds <= 0) return null
        val now = clock.millis()
        return RestTimerState(
            workoutId = workoutId,
            exerciseId = exerciseId,
            startedAtMillis = now,
            endAtMillis = now + durationSeconds * 1_000L,
            totalDurationSeconds = durationSeconds
        )
    }

    fun adjust(state: RestTimerState, deltaSeconds: Int, clock: Clock): RestTimerState? {
        val now = clock.millis()
        val newEnd = state.endAtMillis + deltaSeconds * 1_000L
        if (newEnd <= now) return null
        val prelim = state.copy(endAtMillis = newEnd)
        val remainingSec = remainingSeconds(prelim, clock)
        return prelim.copy(totalDurationSeconds = maxOf(state.totalDurationSeconds, remainingSec))
    }

    fun remainingMillis(state: RestTimerState, clock: Clock): Long =
        (state.endAtMillis - clock.millis()).coerceAtLeast(0L)

    fun remainingSeconds(state: RestTimerState, clock: Clock): Int {
        val rem = remainingMillis(state, clock)
        return if (rem <= 0L) 0 else ((rem + 999L) / 1_000L).toInt()
    }

    fun progress(state: RestTimerState, clock: Clock): Float {
        val totalMs = state.totalDurationSeconds.coerceAtLeast(1) * 1_000L
        return (remainingMillis(state, clock).toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    fun isFinished(state: RestTimerState, clock: Clock): Boolean = clock.millis() >= state.endAtMillis

    fun formatRemaining(remainingSeconds: Int): String {
        val clamped = remainingSeconds.coerceAtLeast(0)
        val minutes = clamped / 60
        val seconds = clamped % 60
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
