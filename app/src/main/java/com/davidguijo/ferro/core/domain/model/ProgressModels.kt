package com.davidguijo.ferro.core.domain.model

import java.time.LocalDate

/** Métricas disponibles en la gráfica temporal de un ejercicio. */
enum class ExerciseChartMetric {
    ESTIMATED_1RM,
    MAX_WEIGHT,
    BEST_SET_VOLUME,
    SESSION_VOLUME
}

/** Rango temporal seleccionable para filtrar las gráficas de ejercicio. */
enum class ProgressTimeRange {
    ONE_MONTH,
    THREE_MONTHS,
    ONE_YEAR,
    ALL
}

/** Agregado SQL de una sesión completada para un ejercicio concreto. */
data class ExerciseSessionMetricRow(
    val workoutId: String,
    val startedAt: Long,
    val estimated1RmKg: Double?,
    val maxWeightKg: Double?,
    val bestSetVolumeKg: Double?,
    val sessionVolumeKg: Double?
)

/** Punto de serie temporal para la gráfica de un ejercicio (valor canónico en kg). */
data class ExerciseChartPoint(val workoutId: String, val timestampMillis: Long, val valueKg: Double)

/** Mejor peso registrado para un número exacto de repeticiones (1..12). */
data class RepMaxEntry(val reps: Int, val weightKg: Double, val achievedAt: Long)

/** Récord personal enriquecido con los datos de la serie que lo consiguió. */
data class ExerciseRecordDetail(
    val record: PersonalRecord,
    val weightKg: Double?,
    val reps: Int?,
    val durationS: Int?,
    val distanceM: Double?
)

/** Agregado SQL ligero de una sesión completada para estadísticas globales. */
data class WorkoutSessionAggregate(val workoutId: String, val startedAt: Long, val endedAt: Long?, val volumeKg: Double)

/** Agregado SQL de series completadas (no calentamiento) de un ejercicio dentro de una sesión. */
data class MuscleExerciseSetAggregate(
    val workoutStartedAt: Long,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val completedSetCount: Int
)

/** Agregado SQL de la última vez que se trabajó una combinación de músculos. */
data class MuscleLastTrainedAggregate(
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val lastTrainedAt: Long
)

/** Volumen total (en kg) y número de entrenos de una semana concreta. */
data class WeeklyVolumePoint(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val volumeKg: Double,
    val workoutCount: Int
)

/** Indicadores resumen de frecuencia y duración media en la ventana de progreso. */
data class ProgressSummaryStats(
    val workoutsThisWeek: Int,
    val avgWorkoutsPerWeek: Double,
    val avgDurationMinutes: Int?,
    val totalWorkoutsInWindow: Int
)

/**
 * Comparativa semanal de series ponderadas por grupo muscular.
 * Regla de ponderación: el músculo principal suma 1,0 serie y cada músculo secundario suma 0,5.
 */
data class MuscleVolumeComparison(val muscle: Muscle, val thisWeekSets: Double, val previous4WeeksAvgSets: Double)

/** Estado orientativo de recuperación de un grupo muscular según el tiempo desde el último entreno. */
enum class RecoveryStatus {
    RECENT,
    RECOVERING_48_72H,
    READY
}

/** Estado de recuperación orientativo de un grupo muscular concreto. */
data class MuscleRecoveryItem(
    val muscle: Muscle,
    val lastTrainedAtMillis: Long,
    val daysSinceLastTrained: Int,
    val hoursSinceLastTrained: Long,
    val status: RecoveryStatus
)
