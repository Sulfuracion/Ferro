package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseChartMetric
import com.davidguijo.ferro.core.domain.model.ExerciseChartPoint
import com.davidguijo.ferro.core.domain.model.ExerciseRecordDetail
import com.davidguijo.ferro.core.domain.model.ExerciseSessionMetricRow
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.MuscleExerciseSetAggregate
import com.davidguijo.ferro.core.domain.model.MuscleLastTrainedAggregate
import com.davidguijo.ferro.core.domain.model.MuscleRecoveryItem
import com.davidguijo.ferro.core.domain.model.MuscleVolumeComparison
import com.davidguijo.ferro.core.domain.model.ProgressSummaryStats
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.RecoveryStatus
import com.davidguijo.ferro.core.domain.model.RepMaxEntry
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WeeklyVolumePoint
import com.davidguijo.ferro.core.domain.model.WorkoutSessionAggregate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Cálculos puros de progreso, gráficas, récords, volumen semanal por grupo muscular y recuperación.
 *
 * Regla de series por grupo muscular:
 * - Solo cuentan series completadas que no sean de calentamiento (`SetType.WARMUP`).
 * - Cada serie completada de un ejercicio suma **1,0** serie a su `primaryMuscle`.
 * - Cada músculo distinto en `secondaryMuscles` (excluyendo el principal si apareciese repetido)
 *   suma **0,5** series por cada serie completada.
 */
object ProgressCalculations {
    /** Número de semanas mostradas en la gráfica de volumen semanal. */
    const val WEEKLY_VOLUME_WEEKS: Int = 12

    /** Número de semanas previas usadas para calcular la media de series por grupo muscular. */
    const val MUSCLE_COMPARISON_PREVIOUS_WEEKS: Int = 4

    /** Peso de cada serie efectiva para el músculo principal. */
    const val PRIMARY_MUSCLE_SET_WEIGHT: Double = 1.0

    /** Peso de cada serie efectiva para cada músculo secundario. */
    const val SECONDARY_MUSCLE_SET_WEIGHT: Double = 0.5

    /** Límite superior (exclusivo) en horas del estado [RecoveryStatus.RECENT]. */
    const val RECOVERY_RECENT_MAX_HOURS: Long = 48L

    /** Límite superior (inclusivo) en horas del estado [RecoveryStatus.RECOVERING_48_72H]. */
    const val RECOVERY_MODERATE_MAX_HOURS: Long = 72L

    private const val MILLIS_PER_HOUR: Long = 3_600_000L
    private const val MILLIS_PER_MINUTE: Double = 60_000.0
    private val REP_MAX_RANGE: IntRange = 1..Records.MAX_EPLEY_REPS

    /**
     * Devuelve el inicio del rango temporal en epoch ms (UTC) o `null` para [ProgressTimeRange.ALL].
     */
    fun timeRangeStartMillis(
        range: ProgressTimeRange,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate: LocalDate =
            when (range) {
                ProgressTimeRange.ONE_MONTH -> today.minusMonths(1)
                ProgressTimeRange.THREE_MONTHS -> today.minusMonths(3)
                ProgressTimeRange.ONE_YEAR -> today.minusYears(1)
                ProgressTimeRange.ALL -> return null
            }
        return startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Devuelve el inicio (epoch ms) de la ventana de [weeksCount] semanas que termina en la semana actual.
     */
    fun rollingWeeksStartMillis(
        nowMillis: Long,
        firstDayOfWeek: DayOfWeek,
        weeksCount: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        require(weeksCount >= 1) { "weeksCount must be >= 1" }
        val currentWeekStart = weekStartFor(nowMillis, firstDayOfWeek, zoneId)
        val firstWeekStart = currentWeekStart.minusWeeks((weeksCount - 1).toLong())
        return firstWeekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Construye la serie temporal ordenada cronológicamente para una métrica de ejercicio.
     * Descarta sesiones sin dato válido (`null`, no finito o `<= 0`).
     */
    fun buildExerciseChartSeries(
        rows: List<ExerciseSessionMetricRow>,
        metric: ExerciseChartMetric
    ): List<ExerciseChartPoint> = rows
        .asSequence()
        .mapNotNull { row ->
            val value =
                when (metric) {
                    ExerciseChartMetric.ESTIMATED_1RM -> row.estimated1RmKg
                    ExerciseChartMetric.MAX_WEIGHT -> row.maxWeightKg
                    ExerciseChartMetric.BEST_SET_VOLUME -> row.bestSetVolumeKg
                    ExerciseChartMetric.SESSION_VOLUME -> row.sessionVolumeKg
                }
            if (value == null || !value.isFinite() || value <= 0.0) {
                null
            } else {
                ExerciseChartPoint(
                    workoutId = row.workoutId,
                    timestampMillis = row.startedAt,
                    valueKg = value
                )
            }
        }.sortedBy { it.timestampMillis }
        .toList()

    /**
     * Selecciona el mejor récord vigente para cada [RecordType] disponible en [records].
     * Respeta la semántica de menor-es-mejor en [TrackingType.ASSISTED_BODYWEIGHT] para [RecordType.MAX_WEIGHT].
     */
    fun selectBestRecordsByType(
        records: List<ExerciseRecordDetail>,
        trackingType: TrackingType
    ): List<ExerciseRecordDetail> {
        if (records.isEmpty()) return emptyList()
        val displayOrder =
            listOf(
                RecordType.ESTIMATED_1RM,
                RecordType.MAX_WEIGHT,
                RecordType.BEST_SET_VOLUME,
                RecordType.MAX_REPS_AT_WEIGHT
            )
        return displayOrder.mapNotNull { type ->
            val candidates = records.filter { it.record.recordType == type }
            if (candidates.isEmpty()) return@mapNotNull null
            val lowerIsBetter =
                trackingType == TrackingType.ASSISTED_BODYWEIGHT && type == RecordType.MAX_WEIGHT
            if (lowerIsBetter) {
                candidates.minWithOrNull(
                    compareBy<ExerciseRecordDetail> { it.record.value }
                        .thenByDescending { it.record.achievedAt }
                )
            } else {
                candidates.maxWithOrNull(
                    compareBy<ExerciseRecordDetail> { it.record.value }
                        .thenBy { it.record.achievedAt }
                )
            }
        }
    }

    /**
     * Normaliza la tabla de "mejor peso para 1 a 12 reps" quedándose con el mayor peso por repetición exacta.
     */
    fun buildRepMaxTable(entries: List<RepMaxEntry>): Map<Int, RepMaxEntry> {
        val result = linkedMapOf<Int, RepMaxEntry>()
        for (entry in entries) {
            if (entry.reps !in REP_MAX_RANGE) continue
            if (!entry.weightKg.isFinite() || entry.weightKg <= 0.0) continue
            val current = result[entry.reps]
            if (current == null ||
                entry.weightKg > current.weightKg + Records.EPSILON ||
                (Records.sameWeight(entry.weightKg, current.weightKg) && entry.achievedAt > current.achievedAt)
            ) {
                result[entry.reps] = entry
            }
        }
        return result
    }

    /**
     * Agrupa los agregados de sesiones completadas en exactamente [weeksCount] semanas consecutivas
     * terminando en la semana actual de [nowMillis], rellenando con 0 las semanas sin entrenos.
     */
    fun computeWeeklyVolumeSeries(
        sessions: List<WorkoutSessionAggregate>,
        nowMillis: Long,
        firstDayOfWeek: DayOfWeek,
        weeksCount: Int = WEEKLY_VOLUME_WEEKS,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<WeeklyVolumePoint> {
        require(weeksCount >= 1) { "weeksCount must be >= 1" }
        val currentWeekStart = weekStartFor(nowMillis, firstDayOfWeek, zoneId)
        val firstWeekStart = currentWeekStart.minusWeeks((weeksCount - 1).toLong())

        val volumeByWeek = mutableMapOf<LocalDate, Double>()
        val countByWeek = mutableMapOf<LocalDate, Int>()

        for (session in sessions) {
            val weekStart = weekStartFor(session.startedAt, firstDayOfWeek, zoneId)
            if (weekStart < firstWeekStart || weekStart > currentWeekStart) continue
            val safeVolume = if (session.volumeKg.isFinite() && session.volumeKg > 0.0) session.volumeKg else 0.0
            volumeByWeek[weekStart] = (volumeByWeek[weekStart] ?: 0.0) + safeVolume
            countByWeek[weekStart] = (countByWeek[weekStart] ?: 0) + 1
        }

        return (0 until weeksCount).map { offset ->
            val weekStart = firstWeekStart.plusWeeks(offset.toLong())
            WeeklyVolumePoint(
                weekStart = weekStart,
                weekEnd = weekEndFor(weekStart),
                volumeKg = volumeByWeek[weekStart] ?: 0.0,
                workoutCount = countByWeek[weekStart] ?: 0
            )
        }
    }

    /**
     * Calcula los indicadores de resumen (entrenos esta semana, media de entrenos/semana y duración media)
     * sobre la ventana de [weeklySeries].
     */
    fun computeProgressSummaryStats(
        weeklySeries: List<WeeklyVolumePoint>,
        sessions: List<WorkoutSessionAggregate>
    ): ProgressSummaryStats {
        if (weeklySeries.isEmpty()) {
            return ProgressSummaryStats(
                workoutsThisWeek = 0,
                avgWorkoutsPerWeek = 0.0,
                avgDurationMinutes = null,
                totalWorkoutsInWindow = 0
            )
        }
        val workoutsThisWeek = weeklySeries.last().workoutCount
        val totalWorkouts = weeklySeries.sumOf { it.workoutCount }
        val avgWorkoutsPerWeek = totalWorkouts.toDouble() / weeklySeries.size.toDouble()

        val validDurationsMillis =
            sessions.mapNotNull { session ->
                val endedAt = session.endedAt ?: return@mapNotNull null
                val duration = endedAt - session.startedAt
                if (duration > 0L) duration else null
            }
        val avgDurationMinutes =
            if (validDurationsMillis.isEmpty()) {
                null
            } else {
                val avgMillis = validDurationsMillis.average()
                (avgMillis / MILLIS_PER_MINUTE).roundToInt().coerceAtLeast(1)
            }

        return ProgressSummaryStats(
            workoutsThisWeek = workoutsThisWeek,
            avgWorkoutsPerWeek = avgWorkoutsPerWeek,
            avgDurationMinutes = avgDurationMinutes,
            totalWorkoutsInWindow = totalWorkouts
        )
    }

    /**
     * Calcula las series ponderadas por grupo muscular de la semana actual frente a la media de las
     * [previousWeeksCount] semanas inmediatamente anteriores (por defecto 4).
     *
     * Regla documentada:
     * - Músculo principal (`primaryMuscle`): **1,0** serie por cada serie completada (no calentamiento).
     * - Cada músculo secundario distinto (`secondaryMuscles`, sin duplicar el principal): **0,5** series.
     */
    fun computeMuscleVolumeComparison(
        rows: List<MuscleExerciseSetAggregate>,
        nowMillis: Long,
        firstDayOfWeek: DayOfWeek,
        previousWeeksCount: Int = MUSCLE_COMPARISON_PREVIOUS_WEEKS,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<MuscleVolumeComparison> {
        require(previousWeeksCount >= 1) { "previousWeeksCount must be >= 1" }
        val currentWeekStart = weekStartFor(nowMillis, firstDayOfWeek, zoneId)
        val previousWindowStart = currentWeekStart.minusWeeks(previousWeeksCount.toLong())

        val thisWeekTotals = mutableMapOf<Muscle, Double>()
        val previousWindowTotals = mutableMapOf<Muscle, Double>()

        for (row in rows) {
            if (row.completedSetCount <= 0) continue
            val weekStart = weekStartFor(row.workoutStartedAt, firstDayOfWeek, zoneId)
            val targetMap =
                when {
                    weekStart == currentWeekStart -> thisWeekTotals
                    weekStart >= previousWindowStart && weekStart < currentWeekStart -> previousWindowTotals
                    else -> continue
                }

            val primaryIncrement = row.completedSetCount * PRIMARY_MUSCLE_SET_WEIGHT
            targetMap[row.primaryMuscle] = (targetMap[row.primaryMuscle] ?: 0.0) + primaryIncrement

            val secondaryIncrement = row.completedSetCount * SECONDARY_MUSCLE_SET_WEIGHT
            val distinctSecondaries = row.secondaryMuscles.distinct().filter { it != row.primaryMuscle }
            for (secondary in distinctSecondaries) {
                targetMap[secondary] = (targetMap[secondary] ?: 0.0) + secondaryIncrement
            }
        }

        val muscles = (thisWeekTotals.keys + previousWindowTotals.keys).distinct()
        return muscles
            .map { muscle ->
                val thisWeek = thisWeekTotals[muscle] ?: 0.0
                val prevAvg = (previousWindowTotals[muscle] ?: 0.0) / previousWeeksCount.toDouble()
                MuscleVolumeComparison(
                    muscle = muscle,
                    thisWeekSets = thisWeek,
                    previous4WeeksAvgSets = prevAvg
                )
            }.filter { it.thisWeekSets > 0.0 || it.previous4WeeksAvgSets > 0.0 }
            .sortedWith(
                compareByDescending<MuscleVolumeComparison> { it.thisWeekSets }
                    .thenByDescending { it.previous4WeeksAvgSets }
                    .thenBy { it.muscle.ordinal }
            )
    }

    /**
     * Calcula el estado orientativo de recuperación de cada grupo muscular trabajado al menos una vez.
     *
     * Umbrales orientativos (no consejo médico):
     * - `< 48 h`: [RecoveryStatus.RECENT]
     * - `48 h..72 h` (inclusive): [RecoveryStatus.RECOVERING_48_72H]
     * - `> 72 h`: [RecoveryStatus.READY]
     */
    fun computeMuscleRecovery(
        rows: List<MuscleLastTrainedAggregate>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<MuscleRecoveryItem> {
        if (rows.isEmpty()) return emptyList()

        val lastTrainedByMuscle = mutableMapOf<Muscle, Long>()
        for (row in rows) {
            val timestamp = row.lastTrainedAt
            val muscles = buildList {
                add(row.primaryMuscle)
                addAll(row.secondaryMuscles)
            }
            for (muscle in muscles) {
                val previous = lastTrainedByMuscle[muscle]
                if (previous == null || timestamp > previous) {
                    lastTrainedByMuscle[muscle] = timestamp
                }
            }
        }

        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return lastTrainedByMuscle.entries
            .map { (muscle, lastTrainedAt) ->
                val elapsedMillis = (nowMillis - lastTrainedAt).coerceAtLeast(0L)
                val hoursSince = elapsedMillis / MILLIS_PER_HOUR
                val lastDate = Instant.ofEpochMilli(lastTrainedAt).atZone(zoneId).toLocalDate()
                val daysSince = ChronoUnit.DAYS.between(lastDate, nowDate).toInt().coerceAtLeast(0)
                val status =
                    when {
                        hoursSince < RECOVERY_RECENT_MAX_HOURS -> RecoveryStatus.RECENT
                        hoursSince <= RECOVERY_MODERATE_MAX_HOURS -> RecoveryStatus.RECOVERING_48_72H
                        else -> RecoveryStatus.READY
                    }
                MuscleRecoveryItem(
                    muscle = muscle,
                    lastTrainedAtMillis = lastTrainedAt,
                    daysSinceLastTrained = daysSince,
                    hoursSinceLastTrained = hoursSince,
                    status = status
                )
            }.sortedWith(
                compareBy<MuscleRecoveryItem> { it.hoursSinceLastTrained }
                    .thenBy { it.muscle.ordinal }
            )
    }
}
