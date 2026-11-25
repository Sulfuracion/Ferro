package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCalculationsTest {
    @Test
    fun `isBackupReminderDue activates only when over 30 days or never backed up with completed workouts`() {
        val now = 100L * BackupCalculations.MILLIS_PER_DAY
        val exact30DaysAgo = now - 30L * BackupCalculations.MILLIS_PER_DAY
        val over30DaysAgo = exact30DaysAgo - 1L

        // Exactamente 30 días -> aún no vence (> 30 días)
        assertFalse(
            BackupCalculations.isBackupReminderDue(
                lastBackupAtMillis = exact30DaysAgo,
                nowMillis = now,
                hasCompletedWorkouts = true
            )
        )

        // Más de 30 días -> aviso activo
        assertTrue(
            BackupCalculations.isBackupReminderDue(
                lastBackupAtMillis = over30DaysAgo,
                nowMillis = now,
                hasCompletedWorkouts = false
            )
        )
        assertEquals(30, BackupCalculations.daysSinceLastBackup(exact30DaysAgo, now))
        assertEquals(31, BackupCalculations.daysSinceLastBackup(now - 31L * BackupCalculations.MILLIS_PER_DAY, now))

        // Nunca exportado: solo avisa si hay algún entreno completado
        assertFalse(
            BackupCalculations.isBackupReminderDue(
                lastBackupAtMillis = null,
                nowMillis = now,
                hasCompletedWorkouts = false
            )
        )
        assertTrue(
            BackupCalculations.isBackupReminderDue(
                lastBackupAtMillis = null,
                nowMillis = now,
                hasCompletedWorkouts = true
            )
        )
        assertNull(BackupCalculations.daysSinceLastBackup(null, now))
    }

    @Test
    fun `buildSeriesCsv formats completed sets in selected unit and escapes RFC 4180 fields`() {
        val bench =
            Exercise(
                id = "ex-bench",
                nameEs = "Press banca",
                nameEn = "Bench press",
                primaryMuscle = Muscle.CHEST,
                secondaryMuscles = listOf(Muscle.TRICEPS),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "",
                instructionsEn = "",
                isCustom = false,
                archived = false,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        val run =
            Exercise(
                id = "ex-run",
                nameEs = "Carrera",
                nameEn = "Running",
                primaryMuscle = Muscle.CARDIO,
                secondaryMuscles = emptyList(),
                equipment = Equipment.CARDIO_MACHINE,
                trackingType = TrackingType.DISTANCE_DURATION,
                instructionsEs = "",
                instructionsEn = "",
                isCustom = false,
                archived = false,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )

        val startedAt = Instant.parse("2026-11-20T17:30:00Z").toEpochMilli()
        val completedWorkout =
            Workout(
                id = "w-1",
                name = "Torso, \"A\"",
                routineId = null,
                startedAt = startedAt,
                endedAt = startedAt + 3_600_000L,
                notes = "Fuerte",
                status = WorkoutStatus.COMPLETED,
                createdAt = startedAt,
                updatedAt = startedAt + 3_600_000L
            )
        val discardedWorkout =
            completedWorkout.copy(
                id = "w-discarded",
                status = WorkoutStatus.DISCARDED
            )

        val weBench =
            WorkoutExercise(
                id = "we-1",
                workoutId = "w-1",
                exerciseId = "ex-bench",
                position = 0,
                supersetGroup = null,
                restSeconds = 90,
                notes = "Pausa, 1s",
                createdAt = startedAt,
                updatedAt = startedAt
            )
        val weRun =
            WorkoutExercise(
                id = "we-2",
                workoutId = "w-1",
                exerciseId = "ex-run",
                position = 1,
                supersetGroup = null,
                restSeconds = null,
                notes = "",
                createdAt = startedAt,
                updatedAt = startedAt
            )

        val sets =
            listOf(
                WorkoutSet(
                    id = "ws-1",
                    workoutExerciseId = "we-1",
                    position = 0,
                    setType = SetType.WARMUP,
                    weightKg = 60.0,
                    reps = 8,
                    rpe = 6.5,
                    durationS = null,
                    distanceM = null,
                    completed = true,
                    completedAt = startedAt + 60_000L,
                    createdAt = startedAt,
                    updatedAt = startedAt
                ),
                // Serie no completada -> no debe exportarse al CSV
                WorkoutSet(
                    id = "ws-incomplete",
                    workoutExerciseId = "we-1",
                    position = 1,
                    setType = SetType.NORMAL,
                    weightKg = 100.0,
                    reps = 5,
                    rpe = null,
                    durationS = null,
                    distanceM = null,
                    completed = false,
                    completedAt = null,
                    createdAt = startedAt,
                    updatedAt = startedAt
                ),
                WorkoutSet(
                    id = "ws-2",
                    workoutExerciseId = "we-2",
                    position = 0,
                    setType = SetType.NORMAL,
                    weightKg = null,
                    reps = null,
                    rpe = null,
                    durationS = 600,
                    distanceM = 2000.0,
                    completed = true,
                    completedAt = startedAt + 600_000L,
                    createdAt = startedAt,
                    updatedAt = startedAt
                )
            )

        val csvKg =
            BackupCalculations.buildSeriesCsv(
                workouts = listOf(completedWorkout, discardedWorkout),
                workoutExercises = listOf(weBench, weRun),
                workoutSets = sets,
                exercisesById = mapOf(bench.id to bench, run.id to run),
                weightUnit = WeightUnit.KG,
                zoneId = ZoneOffset.UTC,
                locale = Locale.US,
                useEnglishExerciseNames = false
            )

        val linesKg = csvKg.trim().lines()
        assertEquals(3, linesKg.size)
        assertEquals("date,workout,exercise,set_type,weight_kg,reps,rpe,duration_s,distance_m,notes", linesKg[0])
        assertEquals(
            "2026-11-20,\"Torso, \"\"A\"\"\",Press banca,WARMUP,60,8,6.5,,,\"Fuerte | Pausa, 1s\"",
            linesKg[1]
        )
        assertEquals(
            "2026-11-20,\"Torso, \"\"A\"\"\",Carrera,NORMAL,,,,600,2000,Fuerte",
            linesKg[2]
        )

        val csvLb =
            BackupCalculations.buildSeriesCsv(
                workouts = listOf(completedWorkout),
                workoutExercises = listOf(weBench),
                workoutSets = listOf(sets.first().copy(weightKg = Units.lbToKg(135.0))),
                exercisesById = mapOf(bench.id to bench),
                weightUnit = WeightUnit.LB,
                zoneId = ZoneOffset.UTC,
                locale = Locale.US,
                useEnglishExerciseNames = true
            )
        val linesLb = csvLb.trim().lines()
        assertEquals("date,workout,exercise,set_type,weight_lb,reps,rpe,duration_s,distance_m,notes", linesLb[0])
        assertEquals(
            "2026-11-20,\"Torso, \"\"A\"\"\",Bench press,WARMUP,135,8,6.5,,,\"Fuerte | Pausa, 1s\"",
            linesLb[1]
        )
    }
}
