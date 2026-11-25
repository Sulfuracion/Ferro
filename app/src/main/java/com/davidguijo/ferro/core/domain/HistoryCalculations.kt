package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Grupo semanal de sesiones completadas ordenadas de más reciente a más antigua. */
data class WeekWorkoutGroup(val weekStart: LocalDate, val weekEnd: LocalDate, val workouts: List<Workout>)

/** Resumen de la racha semanal y entrenos completados en la semana actual. */
data class WeeklyStreakSummary(val consecutiveWeeks: Int, val workoutsThisWeek: Int)

/** Devuelve la fecha de inicio de la semana a la que pertenece [epochMillis] según [firstDayOfWeek]. */
fun weekStartFor(epochMillis: Long, firstDayOfWeek: DayOfWeek, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
}

/** Devuelve el último día (inclusive) de la semana que comienza en [weekStart]. */
fun weekEndFor(weekStart: LocalDate): LocalDate = weekStart.plusDays(6)

/** Agrupa sesiones completadas por semana, ordenadas de la más reciente a la más antigua. */
fun groupWorkoutsByWeek(
    workouts: List<Workout>,
    firstDayOfWeek: DayOfWeek,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<WeekWorkoutGroup> {
    val completedSorted =
        workouts
            .filter { it.status == WorkoutStatus.COMPLETED }
            .sortedByDescending { it.startedAt }
    if (completedSorted.isEmpty()) return emptyList()

    val grouped = linkedMapOf<LocalDate, MutableList<Workout>>()
    for (workout in completedSorted) {
        val weekStart = weekStartFor(workout.startedAt, firstDayOfWeek, zoneId)
        grouped.getOrPut(weekStart) { mutableListOf() }.add(workout)
    }

    return grouped.entries.map { (weekStart, items) ->
        WeekWorkoutGroup(
            weekStart = weekStart,
            weekEnd = weekEndFor(weekStart),
            workouts = items
        )
    }
}

/**
 * Calcula la racha de semanas consecutivas con al menos un entreno completado y los entrenos
 * de la semana actual. Si la semana actual aún no tiene entrenos, la racha se mantiene viva si la
 * semana inmediatamente anterior sí tuvo al menos uno.
 */
fun computeWeeklyStreak(
    completedWorkoutStartMillis: List<Long>,
    nowMillis: Long,
    firstDayOfWeek: DayOfWeek,
    zoneId: ZoneId = ZoneId.systemDefault()
): WeeklyStreakSummary {
    val currentWeekStart = weekStartFor(nowMillis, firstDayOfWeek, zoneId)
    if (completedWorkoutStartMillis.isEmpty()) {
        return WeeklyStreakSummary(consecutiveWeeks = 0, workoutsThisWeek = 0)
    }

    val countsByWeek =
        completedWorkoutStartMillis
            .groupingBy { weekStartFor(it, firstDayOfWeek, zoneId) }
            .eachCount()

    val workoutsThisWeek = countsByWeek[currentWeekStart] ?: 0
    var cursor =
        if (workoutsThisWeek > 0) {
            currentWeekStart
        } else {
            currentWeekStart.minusWeeks(1)
        }

    var streak = 0
    while ((countsByWeek[cursor] ?: 0) > 0) {
        streak++
        cursor = cursor.minusWeeks(1)
    }

    return WeeklyStreakSummary(
        consecutiveWeeks = streak,
        workoutsThisWeek = workoutsThisWeek
    )
}
