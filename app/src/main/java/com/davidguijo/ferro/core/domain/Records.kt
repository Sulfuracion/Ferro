package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/** Récord batido en una serie concreta junto con su marca histórica anterior (si existía). */
data class BrokenRecord(
    val record: PersonalRecord,
    val set: WorkoutSet,
    val trackingType: TrackingType,
    val previousValue: Double? = null
)

/** Cálculos puros de 1RM estimado, récords personales y recálculo cronológico del historial. */
object Records {
    /** Número máximo de repeticiones para estimar el 1RM con la fórmula de Epley. */
    const val MAX_EPLEY_REPS: Int = 12

    /** Tolerancia en kg (0,5 g) para comparaciones de punto flotante y cambios kg/lb. */
    const val EPSILON: Double = 5e-4

    private const val WEIGHT_BUCKET_MILLIS_FACTOR: Double = 1000.0

    /**
     * Calcula el 1RM estimado mediante la fórmula de Epley: `weightKg * (1 + reps / 30.0)`.
     * - Con 1 repetición devuelve exactamente [weightKg].
     * - Por encima de [MAX_EPLEY_REPS] (12 reps), o con peso/reps no válidos (`<= 0`), devuelve `null`.
     */
    fun estimateOneRepMaxEpley(weightKg: Double, reps: Int): Double? {
        if (!weightKg.isFinite() || weightKg <= 0.0) return null
        if (reps < 1 || reps > MAX_EPLEY_REPS) return null
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    /** Indica si una serie es elegible para récords (completada y no de calentamiento). */
    fun isEligibleRecordSet(set: WorkoutSet): Boolean = set.completed && set.setType != SetType.WARMUP

    /**
     * Peso máximo (o asistencia mínima en [TrackingType.ASSISTED_BODYWEIGHT], o distancia máxima en
     * [TrackingType.DISTANCE_DURATION]) entre las series válidas de [sets].
     */
    fun computeBestMaxWeight(sets: Iterable<WorkoutSet>, trackingType: TrackingType): Double? {
        val eligible = sets.filter(::isEligibleRecordSet)
        return when (trackingType) {
            TrackingType.WEIGHT_REPS ->
                eligible
                    .filter { (it.reps ?: 0) > 0 && (it.weightKg ?: 0.0) > 0.0 }
                    .maxOfOrNull { it.weightKg ?: 0.0 }

            TrackingType.WEIGHTED_BODYWEIGHT ->
                eligible
                    .filter { (it.reps ?: 0) > 0 && (it.weightKg ?: -1.0) >= 0.0 }
                    .maxOfOrNull { it.weightKg ?: 0.0 }

            TrackingType.ASSISTED_BODYWEIGHT ->
                eligible
                    .filter { (it.reps ?: 0) > 0 && (it.weightKg ?: -1.0) >= 0.0 }
                    .minOfOrNull { it.weightKg ?: Double.POSITIVE_INFINITY }

            TrackingType.DISTANCE_DURATION ->
                eligible
                    .filter { (it.distanceM ?: 0.0) > 0.0 }
                    .maxOfOrNull { it.distanceM ?: 0.0 }

            TrackingType.BODYWEIGHT_REPS,
            TrackingType.DURATION -> null
        }
    }

    /** Mejor 1RM estimado (Epley, 1..12 reps) entre las series válidas de [sets]. */
    fun computeBestEstimated1Rm(sets: Iterable<WorkoutSet>, trackingType: TrackingType): Double? {
        if (trackingType != TrackingType.WEIGHT_REPS && trackingType != TrackingType.WEIGHTED_BODYWEIGHT) {
            return null
        }
        return sets
            .asSequence()
            .filter(::isEligibleRecordSet)
            .mapNotNull { set ->
                val weight = set.weightKg ?: return@mapNotNull null
                val reps = set.reps ?: return@mapNotNull null
                estimateOneRepMaxEpley(weight, reps)
            }.maxOrNull()
    }

    /**
     * Mejor volumen de una sola serie (`weightKg * reps`), o mayor duración (`durationS`) en ejercicios
     * de tiempo ([TrackingType.DURATION] / [TrackingType.DISTANCE_DURATION]), entre las series válidas.
     */
    fun computeBestSetVolume(sets: Iterable<WorkoutSet>, trackingType: TrackingType): Double? {
        val eligible = sets.filter(::isEligibleRecordSet)
        return when (trackingType) {
            TrackingType.WEIGHT_REPS,
            TrackingType.WEIGHTED_BODYWEIGHT ->
                eligible
                    .mapNotNull { set ->
                        val weight = set.weightKg ?: return@mapNotNull null
                        val reps = set.reps ?: return@mapNotNull null
                        if (weight > 0.0 && reps > 0) weight * reps else null
                    }.maxOrNull()

            TrackingType.DURATION,
            TrackingType.DISTANCE_DURATION ->
                eligible
                    .mapNotNull { set ->
                        val duration = set.durationS ?: return@mapNotNull null
                        if (duration > 0) duration.toDouble() else null
                    }.maxOrNull()

            TrackingType.BODYWEIGHT_REPS,
            TrackingType.ASSISTED_BODYWEIGHT -> null
        }
    }

    /**
     * Mayor número de repeticiones realizadas a un peso dado [targetWeightKg] (o con menor o igual
     * asistencia en [TrackingType.ASSISTED_BODYWEIGHT]). En [TrackingType.BODYWEIGHT_REPS] ignora el peso.
     */
    fun computeMaxRepsAtWeight(
        sets: Iterable<WorkoutSet>,
        trackingType: TrackingType,
        targetWeightKg: Double? = null
    ): Int? {
        val eligible = sets.filter(::isEligibleRecordSet)
        return when (trackingType) {
            TrackingType.BODYWEIGHT_REPS ->
                eligible
                    .mapNotNull { it.reps?.takeIf { r -> r > 0 } }
                    .maxOrNull()

            TrackingType.WEIGHT_REPS -> {
                val target = targetWeightKg ?: return null
                val targetBucket = weightBucketKey(target)
                eligible
                    .filter { set ->
                        val w = set.weightKg ?: return@filter false
                        w > 0.0 && weightBucketKey(w) == targetBucket
                    }.mapNotNull { it.reps?.takeIf { r -> r > 0 } }
                    .maxOrNull()
            }

            TrackingType.WEIGHTED_BODYWEIGHT -> {
                val target = targetWeightKg ?: return null
                val targetBucket = weightBucketKey(target)
                eligible
                    .filter { set ->
                        val w = set.weightKg ?: return@filter false
                        w >= 0.0 && weightBucketKey(w) == targetBucket
                    }.mapNotNull { it.reps?.takeIf { r -> r > 0 } }
                    .maxOrNull()
            }

            TrackingType.ASSISTED_BODYWEIGHT -> {
                val target = targetWeightKg ?: return null
                val targetBucket = weightBucketKey(target)
                eligible
                    .filter { set ->
                        val w = set.weightKg ?: return@filter false
                        w >= 0.0 && weightBucketKey(w) == targetBucket
                    }.mapNotNull { it.reps?.takeIf { r -> r > 0 } }
                    .maxOrNull()
            }

            TrackingType.DURATION,
            TrackingType.DISTANCE_DURATION -> null
        }
    }

    /**
     * Compara las series de un ejercicio en la sesión que acaba de terminar ([currentSets]) frente a
     * todas las series de sesiones completadas anteriores ([historicalSets]) y devuelve los nuevos récords.
     *
     * Reglas:
     * - Se ignoran series de calentamiento y series no completadas.
     * - Los empates (`==` dentro de [EPSILON]) NO baten récord: solo se premia una mejora estricta.
     * - Si varias series de la misma sesión superan la marca previa en la misma categoría, se registra
     *   como récord de la sesión la mejor serie (y en caso de empate interno dentro de la sesión, la primera).
     */
    fun detectNewRecordsForExercise(
        exerciseId: String,
        trackingType: TrackingType,
        currentSets: List<WorkoutSet>,
        historicalSets: List<WorkoutSet>,
        achievedAtMillis: Long,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): List<BrokenRecord> {
        val eligibleCurrent = currentSets.filter(::isEligibleRecordSet)
        if (eligibleCurrent.isEmpty()) return emptyList()

        val eligibleHistory = historicalSets.filter(::isEligibleRecordSet)
        val baseline = ExerciseRecordState.fromHistoricalSets(eligibleHistory, trackingType)
        val broken = mutableListOf<BrokenRecord>()

        // 1. MAX_WEIGHT (o mínima asistencia en ASSISTED_BODYWEIGHT, o máxima distancia en DISTANCE_DURATION)
        val bestMaxWeightCandidate =
            pickBestSetForCategory(
                sets = eligibleCurrent,
                trackingType = trackingType,
                recordType = RecordType.MAX_WEIGHT
            )
        if (bestMaxWeightCandidate != null) {
            val (candidateSet, candidateValue) = bestMaxWeightCandidate
            val previousBest = baseline.bestMaxWeight
            if (isStrictlyBetterValue(candidateValue, previousBest, trackingType, RecordType.MAX_WEIGHT)) {
                broken +=
                    createBrokenRecord(
                        idGenerator = idGenerator,
                        exerciseId = exerciseId,
                        recordType = RecordType.MAX_WEIGHT,
                        value = candidateValue,
                        set = candidateSet,
                        trackingType = trackingType,
                        previousValue = previousBest,
                        achievedAtMillis = achievedAtMillis
                    )
            }
        }

        // 2. ESTIMATED_1RM (Epley, 1..12 reps)
        val best1RmCandidate =
            pickBestSetForCategory(
                sets = eligibleCurrent,
                trackingType = trackingType,
                recordType = RecordType.ESTIMATED_1RM
            )
        if (best1RmCandidate != null) {
            val (candidateSet, candidateValue) = best1RmCandidate
            val previousBest = baseline.bestEstimated1Rm
            if (isStrictlyBetterValue(candidateValue, previousBest, trackingType, RecordType.ESTIMATED_1RM)) {
                broken +=
                    createBrokenRecord(
                        idGenerator = idGenerator,
                        exerciseId = exerciseId,
                        recordType = RecordType.ESTIMATED_1RM,
                        value = candidateValue,
                        set = candidateSet,
                        trackingType = trackingType,
                        previousValue = previousBest,
                        achievedAtMillis = achievedAtMillis
                    )
            }
        }

        // 3. BEST_SET_VOLUME (peso × reps, o duración en segundos para DURATION / DISTANCE_DURATION)
        val bestVolumeCandidate =
            pickBestSetForCategory(
                sets = eligibleCurrent,
                trackingType = trackingType,
                recordType = RecordType.BEST_SET_VOLUME
            )
        if (bestVolumeCandidate != null) {
            val (candidateSet, candidateValue) = bestVolumeCandidate
            val previousBest = baseline.bestSetVolume
            if (isStrictlyBetterValue(candidateValue, previousBest, trackingType, RecordType.BEST_SET_VOLUME)) {
                broken +=
                    createBrokenRecord(
                        idGenerator = idGenerator,
                        exerciseId = exerciseId,
                        recordType = RecordType.BEST_SET_VOLUME,
                        value = candidateValue,
                        set = candidateSet,
                        trackingType = trackingType,
                        previousValue = previousBest,
                        achievedAtMillis = achievedAtMillis
                    )
            }
        }

        // 4. MAX_REPS_AT_WEIGHT (más reps a un peso dado / lastre dado / asistencia dada / peso corporal)
        broken +=
            detectMaxRepsAtWeightRecords(
                exerciseId = exerciseId,
                trackingType = trackingType,
                eligibleCurrent = eligibleCurrent,
                baseline = baseline,
                achievedAtMillis = achievedAtMillis,
                idGenerator = idGenerator
            )

        return broken
    }

    /**
     * Detecta todos los récords batidos al terminar [completedWorkout] comparando contra el historial
     * previo por ejercicio ([historicalSetsByExerciseId]).
     * Si un mismo ejercicio aparece varias veces en la sesión (p. ej. bloques separados), unifica sus series.
     */
    fun detectNewRecordsForWorkout(
        completedWorkout: WorkoutWithExercises,
        trackingTypesByExerciseId: Map<String, TrackingType>,
        historicalSetsByExerciseId: Map<String, List<WorkoutSet>>,
        achievedAtMillis: Long = completedWorkout.workout.endedAt ?: completedWorkout.workout.startedAt,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): List<BrokenRecord> {
        if (completedWorkout.workout.status != WorkoutStatus.COMPLETED) return emptyList()

        val setsByExerciseId = linkedMapOf<String, MutableList<WorkoutSet>>()
        for (exerciseWithSets in completedWorkout.exercises.sortedBy { it.workoutExercise.position }) {
            val list = setsByExerciseId.getOrPut(exerciseWithSets.workoutExercise.exerciseId) { mutableListOf() }
            list.addAll(exerciseWithSets.sets.sortedBy { it.position })
        }

        val allBroken = mutableListOf<BrokenRecord>()
        for ((exerciseId, currentSets) in setsByExerciseId) {
            val trackingType = trackingTypesByExerciseId[exerciseId] ?: TrackingType.WEIGHT_REPS
            val historicalSets = historicalSetsByExerciseId[exerciseId].orEmpty()
            allBroken +=
                detectNewRecordsForExercise(
                    exerciseId = exerciseId,
                    trackingType = trackingType,
                    currentSets = currentSets,
                    historicalSets = historicalSets,
                    achievedAtMillis = achievedAtMillis,
                    idGenerator = idGenerator
                )
        }
        return allBroken
    }

    /**
     * Recalcula desde cero la historia completa de récords de un ejercicio recorriendo sus sesiones
     * completadas en orden cronológico ascendente (`workoutStartedAt ASC`).
     *
     * Esta función pura se reutiliza cuando se edita o borra una sesión pasada para reconstruir
     * `personal_records` sin inconsistencias.
     */
    fun recalculateRecordsForExerciseHistory(
        exerciseId: String,
        trackingType: TrackingType,
        sessions: List<ExerciseHistoryEntry>,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): List<PersonalRecord> {
        if (sessions.isEmpty()) return emptyList()
        val chronologicalSessions =
            sessions.sortedWith(
                compareBy<ExerciseHistoryEntry> { it.workoutStartedAt }
                    .thenBy { it.workoutEndedAt ?: it.workoutStartedAt }
                    .thenBy { it.workoutExercise.position }
                    .thenBy { it.workoutId }
            )

        // Agrupamos por workoutId por si un mismo entreno tiene dos bloques del mismo ejercicio
        val groupedByWorkout = linkedMapOf<String, MutableList<ExerciseHistoryEntry>>()
        for (entry in chronologicalSessions) {
            groupedByWorkout.getOrPut(entry.workoutId) { mutableListOf() }.add(entry)
        }

        val accumulatedHistorySets = mutableListOf<WorkoutSet>()
        val resultingRecords = mutableListOf<PersonalRecord>()

        for ((_, workoutEntries) in groupedByWorkout) {
            val firstEntry = workoutEntries.first()
            val sessionSets =
                workoutEntries.flatMap { entry ->
                    entry.sets.sortedBy { it.position }
                }
            val achievedAt = firstEntry.workoutEndedAt ?: firstEntry.workoutStartedAt
            val sessionBroken =
                detectNewRecordsForExercise(
                    exerciseId = exerciseId,
                    trackingType = trackingType,
                    currentSets = sessionSets,
                    historicalSets = accumulatedHistorySets,
                    achievedAtMillis = achievedAt,
                    idGenerator = idGenerator
                )
            resultingRecords.addAll(sessionBroken.map { it.record })
            accumulatedHistorySets.addAll(sessionSets)
        }

        return resultingRecords
    }

    /** Extrae el mapa `setId -> List<RecordType>` para marcar los récords en las filas de serie. */
    fun groupRecordTypesBySetId(records: Iterable<PersonalRecord>): Map<String, List<RecordType>> = records
        .groupBy { it.workoutSetId }
        .mapValues { (_, list) ->
            list.map { it.recordType }.distinct().sortedBy { it.ordinal }
        }

    private fun pickBestSetForCategory(
        sets: List<WorkoutSet>,
        trackingType: TrackingType,
        recordType: RecordType
    ): Pair<WorkoutSet, Double>? {
        var bestSet: WorkoutSet? = null
        var bestValue: Double? = null

        for (set in sets) {
            val candidate = metricValueForSet(set, trackingType, recordType) ?: continue
            if (isStrictlyBetterValue(candidate, bestValue, trackingType, recordType)) {
                bestValue = candidate
                bestSet = set
            }
        }
        val finalSet = bestSet ?: return null
        val finalValue = bestValue ?: return null
        return finalSet to finalValue
    }

    internal fun metricValueForSet(set: WorkoutSet, trackingType: TrackingType, recordType: RecordType): Double? {
        if (!isEligibleRecordSet(set)) return null
        return when (recordType) {
            RecordType.MAX_WEIGHT ->
                when (trackingType) {
                    TrackingType.WEIGHT_REPS -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        if (w > 0.0 && r > 0) w else null
                    }

                    TrackingType.WEIGHTED_BODYWEIGHT,
                    TrackingType.ASSISTED_BODYWEIGHT -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        if (w >= 0.0 && r > 0) w else null
                    }

                    TrackingType.DISTANCE_DURATION -> {
                        val d = set.distanceM ?: return null
                        if (d > 0.0) d else null
                    }

                    TrackingType.BODYWEIGHT_REPS,
                    TrackingType.DURATION -> null
                }

            RecordType.ESTIMATED_1RM ->
                when (trackingType) {
                    TrackingType.WEIGHT_REPS,
                    TrackingType.WEIGHTED_BODYWEIGHT -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        estimateOneRepMaxEpley(w, r)
                    }

                    else -> null
                }

            RecordType.BEST_SET_VOLUME ->
                when (trackingType) {
                    TrackingType.WEIGHT_REPS,
                    TrackingType.WEIGHTED_BODYWEIGHT -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        if (w > 0.0 && r > 0) w * r else null
                    }

                    TrackingType.DURATION,
                    TrackingType.DISTANCE_DURATION -> {
                        val d = set.durationS ?: return null
                        if (d > 0) d.toDouble() else null
                    }

                    TrackingType.BODYWEIGHT_REPS,
                    TrackingType.ASSISTED_BODYWEIGHT -> null
                }

            RecordType.MAX_REPS_AT_WEIGHT ->
                when (trackingType) {
                    TrackingType.BODYWEIGHT_REPS -> set.reps?.takeIf { it > 0 }?.toDouble()

                    TrackingType.WEIGHT_REPS -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        if (w > 0.0 && r > 0) r.toDouble() else null
                    }

                    TrackingType.WEIGHTED_BODYWEIGHT,
                    TrackingType.ASSISTED_BODYWEIGHT -> {
                        val w = set.weightKg ?: return null
                        val r = set.reps ?: return null
                        if (w >= 0.0 && r > 0) r.toDouble() else null
                    }

                    TrackingType.DURATION,
                    TrackingType.DISTANCE_DURATION -> null
                }
        }
    }

    private fun isStrictlyBetterValue(
        candidate: Double,
        currentBest: Double?,
        trackingType: TrackingType,
        recordType: RecordType
    ): Boolean {
        if (!candidate.isFinite()) return false
        if (currentBest == null) return true
        val lowerIsBetter =
            trackingType == TrackingType.ASSISTED_BODYWEIGHT && recordType == RecordType.MAX_WEIGHT
        return if (lowerIsBetter) {
            candidate < currentBest - EPSILON
        } else {
            candidate > currentBest + EPSILON
        }
    }

    private fun detectMaxRepsAtWeightRecords(
        exerciseId: String,
        trackingType: TrackingType,
        eligibleCurrent: List<WorkoutSet>,
        baseline: ExerciseRecordState,
        achievedAtMillis: Long,
        idGenerator: () -> String
    ): List<BrokenRecord> {
        return when (trackingType) {
            TrackingType.BODYWEIGHT_REPS -> {
                var bestSet: WorkoutSet? = null
                var bestReps = 0
                for (set in eligibleCurrent) {
                    val reps = set.reps ?: continue
                    if (reps > bestReps) {
                        bestReps = reps
                        bestSet = set
                    }
                }
                val winner = bestSet ?: return emptyList()
                val prevReps = baseline.maxRepsBodyweight
                if (prevReps == null || bestReps > prevReps) {
                    listOf(
                        createBrokenRecord(
                            idGenerator = idGenerator,
                            exerciseId = exerciseId,
                            recordType = RecordType.MAX_REPS_AT_WEIGHT,
                            value = bestReps.toDouble(),
                            set = winner,
                            trackingType = trackingType,
                            previousValue = prevReps?.toDouble(),
                            achievedAtMillis = achievedAtMillis
                        )
                    )
                } else {
                    emptyList()
                }
            }

            TrackingType.WEIGHT_REPS,
            TrackingType.WEIGHTED_BODYWEIGHT,
            TrackingType.ASSISTED_BODYWEIGHT -> {
                // Para cada peso distinto usado en la sesión actual, nos quedamos con la primera serie
                // que alcance el máximo de repeticiones en ese peso.
                val bestByWeightBucket = linkedMapOf<Long, Pair<WorkoutSet, Int>>()
                for (set in eligibleCurrent) {
                    val weight = set.weightKg ?: continue
                    val reps = set.reps ?: continue
                    val validWeight =
                        if (trackingType == TrackingType.WEIGHT_REPS) weight > 0.0 else weight >= 0.0
                    if (!validWeight || reps <= 0) continue

                    val bucket = weightBucketKey(weight)
                    val existing = bestByWeightBucket[bucket]
                    if (existing == null || reps > existing.second) {
                        bestByWeightBucket[bucket] = set to reps
                    }
                }

                val brokenList = mutableListOf<BrokenRecord>()
                for ((bucket, pair) in bestByWeightBucket) {
                    val (candidateSet, candidateReps) = pair
                    val prevBestAtWeight = baseline.maxRepsByWeightBucket[bucket]
                    if (prevBestAtWeight == null) {
                        // Solo es récord de "más reps a un peso dado" si ya existía ese peso antes
                        // o si supera las reps logradas con cualquier peso igual o más difícil en el historial.
                        val prevAtOrHarder =
                            baseline.maxRepsAtOrHarderWeight(
                                weightBucket = bucket,
                                trackingType = trackingType
                            )
                        // Si nunca había hecho el ejercicio (historial vacío), la serie de más reps
                        // o si supera lo hecho en pesos iguales/más duros con historial previo:
                        val isNewRecord =
                            if (baseline.maxRepsByWeightBucket.isEmpty()) {
                                true
                            } else {
                                prevAtOrHarder == null || candidateReps > prevAtOrHarder
                            }
                        if (isNewRecord) {
                            brokenList +=
                                createBrokenRecord(
                                    idGenerator = idGenerator,
                                    exerciseId = exerciseId,
                                    recordType = RecordType.MAX_REPS_AT_WEIGHT,
                                    value = candidateReps.toDouble(),
                                    set = candidateSet,
                                    trackingType = trackingType,
                                    previousValue = prevAtOrHarder?.toDouble(),
                                    achievedAtMillis = achievedAtMillis
                                )
                        }
                    } else if (candidateReps > prevBestAtWeight) {
                        brokenList +=
                            createBrokenRecord(
                                idGenerator = idGenerator,
                                exerciseId = exerciseId,
                                recordType = RecordType.MAX_REPS_AT_WEIGHT,
                                value = candidateReps.toDouble(),
                                set = candidateSet,
                                trackingType = trackingType,
                                previousValue = prevBestAtWeight.toDouble(),
                                achievedAtMillis = achievedAtMillis
                            )
                    }
                }
                brokenList
            }

            TrackingType.DURATION,
            TrackingType.DISTANCE_DURATION -> emptyList()
        }
    }

    private fun createBrokenRecord(
        idGenerator: () -> String,
        exerciseId: String,
        recordType: RecordType,
        value: Double,
        set: WorkoutSet,
        trackingType: TrackingType,
        previousValue: Double?,
        achievedAtMillis: Long
    ): BrokenRecord {
        val timestamp = set.completedAt ?: achievedAtMillis
        return BrokenRecord(
            record =
                PersonalRecord(
                    id = idGenerator(),
                    exerciseId = exerciseId,
                    recordType = recordType,
                    value = value,
                    workoutSetId = set.id,
                    achievedAt = timestamp,
                    createdAt = achievedAtMillis,
                    updatedAt = achievedAtMillis
                ),
            set = set,
            trackingType = trackingType,
            previousValue = previousValue
        )
    }

    /** Clave discreta al gramo (`round(weightKg * 1000)`) para agrupar pesos idénticos tras conversión kg/lb. */
    internal fun weightBucketKey(weightKg: Double): Long = (weightKg * WEIGHT_BUCKET_MILLIS_FACTOR).roundToLong()

    internal fun sameWeight(aKg: Double, bKg: Double): Boolean = abs(aKg - bKg) <= EPSILON

    private data class ExerciseRecordState(
        val bestMaxWeight: Double?,
        val bestEstimated1Rm: Double?,
        val bestSetVolume: Double?,
        val maxRepsBodyweight: Int?,
        val maxRepsByWeightBucket: Map<Long, Int>
    ) {
        fun maxRepsAtOrHarderWeight(weightBucket: Long, trackingType: TrackingType): Int? {
            if (maxRepsByWeightBucket.isEmpty()) return null
            val matching =
                maxRepsByWeightBucket.filterKeys { existingBucket ->
                    if (trackingType == TrackingType.ASSISTED_BODYWEIGHT) {
                        // En asistido, menor o igual asistencia es igual o más difícil
                        existingBucket <= weightBucket
                    } else {
                        // En peso/lastre, mayor o igual peso es igual o más difícil
                        existingBucket >= weightBucket
                    }
                }
            return matching.values.maxOrNull()
        }

        companion object {
            fun fromHistoricalSets(eligibleSets: List<WorkoutSet>, trackingType: TrackingType): ExerciseRecordState {
                val maxWeight = computeBestMaxWeight(eligibleSets, trackingType)
                val est1Rm = computeBestEstimated1Rm(eligibleSets, trackingType)
                val bestVolume = computeBestSetVolume(eligibleSets, trackingType)
                val maxRepsBw =
                    if (trackingType == TrackingType.BODYWEIGHT_REPS) {
                        eligibleSets.mapNotNull { it.reps?.takeIf { r -> r > 0 } }.maxOrNull()
                    } else {
                        null
                    }
                val repsByBucket = linkedMapOf<Long, Int>()
                if (trackingType == TrackingType.WEIGHT_REPS ||
                    trackingType == TrackingType.WEIGHTED_BODYWEIGHT ||
                    trackingType == TrackingType.ASSISTED_BODYWEIGHT
                ) {
                    for (set in eligibleSets) {
                        val w = set.weightKg ?: continue
                        val r = set.reps ?: continue
                        val validW =
                            if (trackingType == TrackingType.WEIGHT_REPS) w > 0.0 else w >= 0.0
                        if (!validW || r <= 0) continue
                        val bucket = weightBucketKey(w)
                        val current = repsByBucket[bucket]
                        if (current == null || r > current) {
                            repsByBucket[bucket] = r
                        }
                    }
                }
                return ExerciseRecordState(
                    bestMaxWeight = maxWeight,
                    bestEstimated1Rm = est1Rm,
                    bestSetVolume = bestVolume,
                    maxRepsBodyweight = maxRepsBw,
                    maxRepsByWeightBucket = repsByBucket
                )
            }
        }
    }
}
