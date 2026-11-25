package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCalculationsTest {
    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int = 10, minute: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun completedWorkout(id: String, startedAt: Long, endedAt: Long? = null): Workout = Workout(
        id = id,
        name = "Workout $id",
        routineId = null,
        startedAt = startedAt,
        endedAt = endedAt ?: (startedAt + 3_600_000L),
        notes = "",
        status = WorkoutStatus.COMPLETED,
        createdAt = startedAt,
        updatedAt = startedAt
    )

    @Test
    fun `computeWeeklyStreak returns zeros when there are no workouts`() {
        val now = millisAt(2026, 1, 7)
        val result = computeWeeklyStreak(emptyList(), now, DayOfWeek.MONDAY, zone)
        assertEquals(0, result.consecutiveWeeks)
        assertEquals(0, result.workoutsThisWeek)
    }

    @Test
    fun `computeWeeklyStreak counts consecutive weeks across year boundary`() {
        // 2025-12-22 (Mon), 2025-12-31 (Wed, week of 2025-12-29), 2026-01-06 (Tue, week of 2026-01-05)
        val workouts =
            listOf(
                millisAt(2025, 12, 22),
                millisAt(2025, 12, 31),
                millisAt(2026, 1, 2), // same week as Dec 31
                millisAt(2026, 1, 6)
            )
        val now = millisAt(2026, 1, 8)
        val result = computeWeeklyStreak(workouts, now, DayOfWeek.MONDAY, zone)

        assertEquals(3, result.consecutiveWeeks)
        assertEquals(1, result.workoutsThisWeek)
    }

    @Test
    fun `computeWeeklyStreak stops when an empty week breaks the chain`() {
        val workouts =
            listOf(
                millisAt(2026, 1, 5), // week of Jan 5
                // week of Jan 12 is empty!
                millisAt(2026, 1, 19), // week of Jan 19
                millisAt(2026, 1, 26) // week of Jan 26
            )
        val now = millisAt(2026, 1, 28)
        val result = computeWeeklyStreak(workouts, now, DayOfWeek.MONDAY, zone)

        assertEquals(2, result.consecutiveWeeks)
        assertEquals(1, result.workoutsThisWeek)
    }

    @Test
    fun `computeWeeklyStreak keeps previous streak alive when current week has no workouts yet`() {
        val workouts =
            listOf(
                millisAt(2026, 1, 5),
                millisAt(2026, 1, 12)
            )
        // Today is Monday Jan 19, no workout yet this week
        val now = millisAt(2026, 1, 19)
        val result = computeWeeklyStreak(workouts, now, DayOfWeek.MONDAY, zone)

        assertEquals(2, result.consecutiveWeeks)
        assertEquals(0, result.workoutsThisWeek)
    }

    @Test
    fun `computeWeeklyStreak resets to zero when both current and previous week are empty`() {
        val workouts = listOf(millisAt(2026, 1, 5))
        val now = millisAt(2026, 1, 20)
        val result = computeWeeklyStreak(workouts, now, DayOfWeek.MONDAY, zone)

        assertEquals(0, result.consecutiveWeeks)
        assertEquals(0, result.workoutsThisWeek)
    }

    @Test
    fun `computeWeeklyStreak respects Monday vs Sunday firstDayOfWeek`() {
        // 2025-12-28 is Sunday; 2025-12-29 is Monday
        val workouts =
            listOf(
                millisAt(2025, 12, 28),
                millisAt(2025, 12, 29)
            )
        val now = millisAt(2025, 12, 30)

        val mondayResult = computeWeeklyStreak(workouts, now, DayOfWeek.MONDAY, zone)
        assertEquals(2, mondayResult.consecutiveWeeks)
        assertEquals(1, mondayResult.workoutsThisWeek)

        val sundayResult = computeWeeklyStreak(workouts, now, DayOfWeek.SUNDAY, zone)
        assertEquals(1, sundayResult.consecutiveWeeks)
        assertEquals(2, sundayResult.workoutsThisWeek)
    }

    @Test
    fun `groupWorkoutsByWeek groups completed workouts newest first and respects firstDayOfWeek`() {
        val w1 = completedWorkout("w1", millisAt(2025, 12, 28, 18)) // Sun
        val w2 = completedWorkout("w2", millisAt(2025, 12, 29, 9)) // Mon
        val w3 = completedWorkout("w3", millisAt(2026, 1, 3, 11)) // Sat
        val inProgress =
            completedWorkout("w4", millisAt(2026, 1, 4, 12)).copy(status = WorkoutStatus.IN_PROGRESS)

        val mondayGroups = groupWorkoutsByWeek(listOf(w1, w2, w3, inProgress), DayOfWeek.MONDAY, zone)
        assertEquals(2, mondayGroups.size)
        // Newest week first: 2025-12-29 .. 2026-01-04
        assertEquals(LocalDate.of(2025, 12, 29), mondayGroups[0].weekStart)
        assertEquals(LocalDate.of(2026, 1, 4), mondayGroups[0].weekEnd)
        assertEquals(listOf("w3", "w2"), mondayGroups[0].workouts.map { it.id })
        // Older week: 2025-12-22 .. 2025-12-28
        assertEquals(LocalDate.of(2025, 12, 22), mondayGroups[1].weekStart)
        assertEquals(listOf("w1"), mondayGroups[1].workouts.map { it.id })

        val sundayGroups = groupWorkoutsByWeek(listOf(w1, w2, w3, inProgress), DayOfWeek.SUNDAY, zone)
        assertEquals(1, sundayGroups.size)
        assertEquals(LocalDate.of(2025, 12, 28), sundayGroups[0].weekStart)
        assertEquals(LocalDate.of(2026, 1, 3), sundayGroups[0].weekEnd)
        assertEquals(listOf("w3", "w2", "w1"), sundayGroups[0].workouts.map { it.id })
    }

    @Test
    fun `createWorkoutFromCompletedWorkout and createRoutineFromWorkout preserve structure`() {
        val source =
            WorkoutWithExercises(
                workout =
                    completedWorkout("w-src", 1_000L, 4_600_000L).copy(
                        name = "Push A",
                        notes = "Good session"
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise =
                                WorkoutExercise(
                                    id = "we-1",
                                    workoutId = "w-src",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 120,
                                    notes = "Pause",
                                    createdAt = 1_000L,
                                    updatedAt = 1_000L
                                ),
                            sets =
                                listOf(
                                    WorkoutSet(
                                        id = "ws-1",
                                        workoutExerciseId = "we-1",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = 100.0,
                                        reps = 5,
                                        rpe = 8.5,
                                        durationS = null,
                                        distanceM = null,
                                        completed = true,
                                        completedAt = 2_000L,
                                        createdAt = 1_000L,
                                        updatedAt = 2_000L
                                    )
                                )
                        )
                    )
            )

        val repeated = createWorkoutFromCompletedWorkout(source, nowMillis = 9_000L)
        assertEquals("Push A", repeated.workout.workout.name)
        assertEquals(WorkoutStatus.IN_PROGRESS, repeated.workout.workout.status)
        assertNull(repeated.workout.workout.endedAt)
        val repeatedSet = repeated.workout.exercises.single().sets.single()
        assertFalse(repeatedSet.completed)
        assertNull(repeatedSet.weightKg)
        assertEquals(100.0, repeated.targetPlaceholdersBySetId[repeatedSet.id]?.targetWeightKg ?: 0.0, 0.001)
        assertEquals(5, repeated.targetPlaceholdersBySetId[repeatedSet.id]?.targetRepsMin)

        val routine = createRoutineFromWorkout(source, routineName = "Push Template", nowMillis = 9_000L)
        assertEquals("Push Template", routine.routine.name)
        assertEquals("Good session", routine.routine.notes)
        val routineSet = routine.exercises.single().sets.single()
        assertTrue(routineSet.targetWeightKg == 100.0)
        assertEquals(5, routineSet.targetRepsMin)
        assertEquals(5, routineSet.targetRepsMax)
    }
}
