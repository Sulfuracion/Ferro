package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseChartMetric
import com.davidguijo.ferro.core.domain.model.ExerciseRecordDetail
import com.davidguijo.ferro.core.domain.model.ExerciseSessionMetricRow
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.MuscleExerciseSetAggregate
import com.davidguijo.ferro.core.domain.model.MuscleLastTrainedAggregate
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.RecoveryStatus
import com.davidguijo.ferro.core.domain.model.RepMaxEntry
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSessionAggregate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculationsTest {
    private val utc = ZoneOffset.UTC

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        LocalDate.of(year, month, day).atTime(hour, 0).toInstant(utc).toEpochMilli()

    @Test
    fun `timeRangeStartMillis calcula rangos relativos y null para ALL`() {
        val now = epochMillis(2026, 11, 21, 15)

        assertEquals(
            LocalDate.of(2026, 10, 21).atStartOfDay(utc).toInstant().toEpochMilli(),
            ProgressCalculations.timeRangeStartMillis(ProgressTimeRange.ONE_MONTH, now, utc)
        )
        assertEquals(
            LocalDate.of(2026, 8, 21).atStartOfDay(utc).toInstant().toEpochMilli(),
            ProgressCalculations.timeRangeStartMillis(ProgressTimeRange.THREE_MONTHS, now, utc)
        )
        assertEquals(
            LocalDate.of(2025, 11, 21).atStartOfDay(utc).toInstant().toEpochMilli(),
            ProgressCalculations.timeRangeStartMillis(ProgressTimeRange.ONE_YEAR, now, utc)
        )
        assertNull(ProgressCalculations.timeRangeStartMillis(ProgressTimeRange.ALL, now, utc))
    }

    @Test
    fun `buildExerciseChartSeries filtra nulos o cero y ordena cronologicamente`() {
        val rows =
            listOf(
                ExerciseSessionMetricRow(
                    workoutId = "w2",
                    startedAt = epochMillis(2026, 11, 15),
                    estimated1RmKg = 120.0,
                    maxWeightKg = 100.0,
                    bestSetVolumeKg = 600.0,
                    sessionVolumeKg = 1_800.0
                ),
                ExerciseSessionMetricRow(
                    workoutId = "w1",
                    startedAt = epochMillis(2026, 11, 10),
                    estimated1RmKg = null,
                    maxWeightKg = 90.0,
                    bestSetVolumeKg = 0.0,
                    sessionVolumeKg = 1_400.0
                )
            )

        val est1Rm = ProgressCalculations.buildExerciseChartSeries(rows, ExerciseChartMetric.ESTIMATED_1RM)
        assertEquals(listOf("w2"), est1Rm.map { it.workoutId })
        assertEquals(120.0, est1Rm.first().valueKg, 1e-4)

        val maxWeight = ProgressCalculations.buildExerciseChartSeries(rows, ExerciseChartMetric.MAX_WEIGHT)
        assertEquals(listOf("w1", "w2"), maxWeight.map { it.workoutId })
        assertEquals(listOf(90.0, 100.0), maxWeight.map { it.valueKg })

        val bestSet = ProgressCalculations.buildExerciseChartSeries(rows, ExerciseChartMetric.BEST_SET_VOLUME)
        assertEquals(listOf("w2"), bestSet.map { it.workoutId })

        val sessionVol = ProgressCalculations.buildExerciseChartSeries(rows, ExerciseChartMetric.SESSION_VOLUME)
        assertEquals(listOf(1_400.0, 1_800.0), sessionVol.map { it.valueKg })
    }

    @Test
    fun `selectBestRecordsByType respeta menor es mejor en asistido y orden fijo`() {
        val records =
            listOf(
                recordDetail("r1", RecordType.MAX_WEIGHT, 30.0, 1_000L),
                recordDetail("r2", RecordType.MAX_WEIGHT, 15.0, 2_000L),
                recordDetail("r3", RecordType.MAX_REPS_AT_WEIGHT, 10.0, 3_000L),
                recordDetail("r4", RecordType.MAX_REPS_AT_WEIGHT, 14.0, 4_000L)
            )

        val assistedBest =
            ProgressCalculations.selectBestRecordsByType(records, TrackingType.ASSISTED_BODYWEIGHT)
        assertEquals(
            listOf(RecordType.MAX_WEIGHT, RecordType.MAX_REPS_AT_WEIGHT),
            assistedBest.map {
                it.record.recordType
            }
        )
        assertEquals(15.0, assistedBest[0].record.value, 1e-4)
        assertEquals(14.0, assistedBest[1].record.value, 1e-4)

        val weightedBest = ProgressCalculations.selectBestRecordsByType(records, TrackingType.WEIGHT_REPS)
        assertEquals(30.0, weightedBest[0].record.value, 1e-4)
    }

    @Test
    fun `buildRepMaxTable mantiene rango 1 a 12 y elige mayor peso por rep`() {
        val table =
            ProgressCalculations.buildRepMaxTable(
                listOf(
                    RepMaxEntry(reps = 0, weightKg = 100.0, achievedAt = 1_000L),
                    RepMaxEntry(reps = 13, weightKg = 70.0, achievedAt = 1_000L),
                    RepMaxEntry(reps = 5, weightKg = 95.0, achievedAt = 1_000L),
                    RepMaxEntry(reps = 5, weightKg = 100.0, achievedAt = 2_000L),
                    RepMaxEntry(reps = 1, weightKg = 120.0, achievedAt = 3_000L)
                )
            )

        assertEquals(setOf(1, 5), table.keys)
        assertEquals(120.0, table.getValue(1).weightKg, 1e-4)
        assertEquals(100.0, table.getValue(5).weightKg, 1e-4)
    }

    @Test
    fun `computeWeeklyVolumeSeries y computeProgressSummaryStats devuelven 12 semanas y duracion media`() {
        // Sábado 21 nov 2026 -> semana lunes 16 nov..domingo 22 nov
        val now = epochMillis(2026, 11, 21, 18)
        val sessions =
            listOf(
                WorkoutSessionAggregate(
                    workoutId = "w1",
                    startedAt = epochMillis(2026, 11, 17, 10),
                    endedAt = epochMillis(2026, 11, 17, 11),
                    volumeKg = 5_000.0
                ),
                WorkoutSessionAggregate(
                    workoutId = "w2",
                    startedAt = epochMillis(2026, 11, 19, 10),
                    endedAt = epochMillis(2026, 11, 19, 10) + 40 * 60_000L,
                    volumeKg = 4_000.0
                ),
                WorkoutSessionAggregate(
                    workoutId = "w3",
                    startedAt = epochMillis(2026, 11, 10, 10),
                    endedAt = epochMillis(2026, 11, 10, 10) + 50 * 60_000L,
                    volumeKg = 6_000.0
                )
            )

        val weekly =
            ProgressCalculations.computeWeeklyVolumeSeries(
                sessions = sessions,
                nowMillis = now,
                firstDayOfWeek = DayOfWeek.MONDAY,
                weeksCount = 12,
                zoneId = utc
            )
        assertEquals(12, weekly.size)
        assertEquals(LocalDate.of(2026, 11, 16), weekly.last().weekStart)
        assertEquals(9_000.0, weekly.last().volumeKg, 1e-4)
        assertEquals(2, weekly.last().workoutCount)
        assertEquals(6_000.0, weekly[10].volumeKg, 1e-4)
        assertEquals(1, weekly[10].workoutCount)
        assertTrue(weekly.take(10).all { it.volumeKg == 0.0 && it.workoutCount == 0 })

        val summary = ProgressCalculations.computeProgressSummaryStats(weekly, sessions)
        assertEquals(2, summary.workoutsThisWeek)
        assertEquals(3, summary.totalWorkoutsInWindow)
        assertEquals(0.25, summary.avgWorkoutsPerWeek, 1e-4)
        assertEquals(50, summary.avgDurationMinutes)
    }

    @Test
    fun `computeMuscleVolumeComparison pondera principal 1 y secundarios 0,5 frente a media 4 semanas`() {
        // Sábado 21 nov 2026 -> semana actual: 16 nov..22 nov
        val now = epochMillis(2026, 11, 21, 18)
        val rows =
            listOf(
                // Esta semana: 4 series press banca -> CHEST +4.0, TRICEPS +2.0, SHOULDERS +2.0
                MuscleExerciseSetAggregate(
                    workoutStartedAt = epochMillis(2026, 11, 18),
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
                    completedSetCount = 4
                ),
                // Hace 1 semana: 8 series press banca -> CHEST +8.0, TRICEPS +4.0, SHOULDERS +4.0
                MuscleExerciseSetAggregate(
                    workoutStartedAt = epochMillis(2026, 11, 11),
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
                    completedSetCount = 8
                ),
                // Hace 3 semanas: 4 series curl -> BICEPS +4.0
                MuscleExerciseSetAggregate(
                    workoutStartedAt = epochMillis(2026, 10, 28),
                    primaryMuscle = Muscle.BICEPS,
                    secondaryMuscles = emptyList(),
                    completedSetCount = 4
                ),
                // Hace 5 semanas (fuera de ventana de 4 anteriores): no debe contar
                MuscleExerciseSetAggregate(
                    workoutStartedAt = epochMillis(2026, 10, 14),
                    primaryMuscle = Muscle.QUADS,
                    secondaryMuscles = emptyList(),
                    completedSetCount = 10
                )
            )

        val comparison =
            ProgressCalculations.computeMuscleVolumeComparison(
                rows = rows,
                nowMillis = now,
                firstDayOfWeek = DayOfWeek.MONDAY,
                zoneId = utc
            )
        val byMuscle = comparison.associateBy { it.muscle }

        assertNull(byMuscle[Muscle.QUADS])
        assertEquals(4.0, byMuscle.getValue(Muscle.CHEST).thisWeekSets, 1e-4)
        assertEquals(2.0, byMuscle.getValue(Muscle.CHEST).previous4WeeksAvgSets, 1e-4)
        assertEquals(2.0, byMuscle.getValue(Muscle.TRICEPS).thisWeekSets, 1e-4)
        assertEquals(1.0, byMuscle.getValue(Muscle.TRICEPS).previous4WeeksAvgSets, 1e-4)
        assertEquals(0.0, byMuscle.getValue(Muscle.BICEPS).thisWeekSets, 1e-4)
        assertEquals(1.0, byMuscle.getValue(Muscle.BICEPS).previous4WeeksAvgSets, 1e-4)
    }

    @Test
    fun `computeMuscleRecovery clasifica reciente menor 48h, 48 a 72h y listo mayor 72h`() {
        val now = epochMillis(2026, 11, 21, 18)
        val hourMs = 3_600_000L

        val rows =
            listOf(
                // Hace 20 horas -> RECENT
                MuscleLastTrainedAggregate(
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.TRICEPS),
                    lastTrainedAt = now - 20 * hourMs
                ),
                // Hace 60 horas -> RECOVERING_48_72H
                MuscleLastTrainedAggregate(
                    primaryMuscle = Muscle.QUADS,
                    secondaryMuscles = listOf(Muscle.GLUTES),
                    lastTrainedAt = now - 60 * hourMs
                ),
                // Hace 96 horas -> READY
                MuscleLastTrainedAggregate(
                    primaryMuscle = Muscle.LATS,
                    secondaryMuscles = emptyList(),
                    lastTrainedAt = now - 96 * hourMs
                )
            )

        val recovery = ProgressCalculations.computeMuscleRecovery(rows, now, utc)
        val byMuscle = recovery.associateBy { it.muscle }

        assertEquals(RecoveryStatus.RECENT, byMuscle.getValue(Muscle.CHEST).status)
        assertEquals(RecoveryStatus.RECENT, byMuscle.getValue(Muscle.TRICEPS).status)
        assertEquals(RecoveryStatus.RECOVERING_48_72H, byMuscle.getValue(Muscle.QUADS).status)
        assertEquals(RecoveryStatus.RECOVERING_48_72H, byMuscle.getValue(Muscle.GLUTES).status)
        assertEquals(RecoveryStatus.READY, byMuscle.getValue(Muscle.LATS).status)
        assertEquals(4, byMuscle.getValue(Muscle.LATS).daysSinceLastTrained)
        // Ordenado por más reciente primero
        assertEquals(Muscle.CHEST, recovery.first().muscle)
        assertEquals(Muscle.LATS, recovery.last().muscle)
    }

    private fun recordDetail(id: String, type: RecordType, value: Double, achievedAt: Long): ExerciseRecordDetail =
        ExerciseRecordDetail(
            record =
                PersonalRecord(
                    id = id,
                    exerciseId = "ex-1",
                    recordType = type,
                    value = value,
                    workoutSetId = "set-$id",
                    achievedAt = achievedAt,
                    createdAt = achievedAt,
                    updatedAt = achievedAt
                ),
            weightKg = value,
            reps = 5,
            durationS = null,
            distanceM = null
        )
}
