package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/** Valores objetivo heredados de la rutina para mostrarlos como placeholder en una serie activa. */
data class SetTargetPlaceholder(
    val targetWeightKg: Double? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationS: Int? = null,
    val targetDistanceM: Double? = null
) {
    fun hasAnyTarget(): Boolean = targetWeightKg != null ||
        targetRepsMin != null ||
        targetRepsMax != null ||
        targetDurationS != null ||
        targetDistanceM != null
}

/** Valores de la última sesión completada para la columna «Anterior». */
data class PreviousSetSnapshot(
    val setType: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val durationS: Int?,
    val distanceM: Double?
)

/** Resultado de instanciar un entreno activo a partir de una rutina. */
data class WorkoutFromRoutineResult(
    val workout: WorkoutWithExercises,
    val targetPlaceholdersBySetId: Map<String, SetTargetPlaceholder>
)

/** Qué hacer con las series sin marcar cuando el usuario pulsa Terminar. */
enum class IncompleteSetsStrategy {
    DISCARD_INCOMPLETE,
    MARK_ALL_COMPLETED
}

/**
 * Volumen total en kg del entreno activo o completado: suma `weightKg * reps`
 * únicamente en series completadas que no sean de calentamiento y cuyo tipo de registro
 * utilice peso × reps (`WEIGHT_REPS` y `WEIGHTED_BODYWEIGHT`).
 */
fun computeWorkoutVolumeKg(
    workout: WorkoutWithExercises,
    trackingTypesByExerciseId: Map<String, TrackingType> = emptyMap()
): Double {
    var totalKg = 0.0
    for (exerciseWithSets in workout.exercises) {
        val trackingType =
            trackingTypesByExerciseId[exerciseWithSets.workoutExercise.exerciseId]
                ?: TrackingType.WEIGHT_REPS
        if (trackingType != TrackingType.WEIGHT_REPS &&
            trackingType != TrackingType.WEIGHTED_BODYWEIGHT
        ) {
            continue
        }
        for (set in exerciseWithSets.sets) {
            if (!set.completed || set.setType == SetType.WARMUP) continue
            val weightKg = set.weightKg ?: continue
            val reps = set.reps ?: continue
            if (weightKg > 0.0 && reps > 0) {
                totalKg += weightKg * reps
            }
        }
    }
    return totalKg
}

fun computeCompletedSetsCount(workout: WorkoutWithExercises): Int =
    workout.exercises.sumOf { ex -> ex.sets.count { it.completed } }

fun computeTotalSetsCount(workout: WorkoutWithExercises): Int = workout.exercises.sumOf { ex -> ex.sets.size }

fun computeIncompleteSetsCount(workout: WorkoutWithExercises): Int =
    workout.exercises.sumOf { ex -> ex.sets.count { !it.completed } }

fun createEmptyWorkout(
    defaultName: String,
    nowMillis: Long,
    idGenerator: () -> String = { UUID.randomUUID().toString() }
): WorkoutWithExercises = WorkoutWithExercises(
    workout =
        Workout(
            id = idGenerator(),
            name = defaultName,
            routineId = null,
            startedAt = nowMillis,
            endedAt = null,
            notes = "",
            status = WorkoutStatus.IN_PROGRESS,
            createdAt = nowMillis,
            updatedAt = nowMillis
        ),
    exercises = emptyList()
)

/**
 * Crea un entreno en curso copiando ejercicios, superseries, notas, descansos y series objetivo
 * desde [routine]. Los valores reales de cada serie empiezan vacíos (`null`) y los objetivos
 * se devuelven en [WorkoutFromRoutineResult.targetPlaceholdersBySetId].
 */
fun createWorkoutFromRoutine(
    routine: RoutineWithExercises,
    nowMillis: Long,
    idGenerator: () -> String = { UUID.randomUUID().toString() }
): WorkoutFromRoutineResult {
    val workoutId = idGenerator()
    val placeholders = linkedMapOf<String, SetTargetPlaceholder>()

    val rawExercises =
        routine.exercises
            .sortedBy { it.routineExercise.position }
            .mapIndexed { exerciseIndex, routineExerciseWithSets ->
                val workoutExerciseId = idGenerator()
                val routineExercise = routineExerciseWithSets.routineExercise
                val workoutSets =
                    routineExerciseWithSets.sets
                        .sortedBy { it.position }
                        .mapIndexed { setIndex, routineSet ->
                            val setId = idGenerator()
                            val placeholder =
                                SetTargetPlaceholder(
                                    targetWeightKg = routineSet.targetWeightKg,
                                    targetRepsMin = routineSet.targetRepsMin,
                                    targetRepsMax = routineSet.targetRepsMax,
                                    targetDurationS = routineSet.targetDurationS,
                                    targetDistanceM = routineSet.targetDistanceM
                                )
                            if (placeholder.hasAnyTarget()) {
                                placeholders[setId] = placeholder
                            }
                            WorkoutSet(
                                id = setId,
                                workoutExerciseId = workoutExerciseId,
                                position = setIndex,
                                setType = routineSet.setType,
                                weightKg = null,
                                reps = null,
                                rpe = null,
                                durationS = null,
                                distanceM = null,
                                completed = false,
                                completedAt = null,
                                createdAt = nowMillis,
                                updatedAt = nowMillis
                            )
                        }
                WorkoutExerciseWithSets(
                    workoutExercise =
                        WorkoutExercise(
                            id = workoutExerciseId,
                            workoutId = workoutId,
                            exerciseId = routineExercise.exerciseId,
                            position = exerciseIndex,
                            supersetGroup = routineExercise.supersetGroup,
                            restSeconds = routineExercise.restSeconds,
                            notes = routineExercise.notes,
                            createdAt = nowMillis,
                            updatedAt = nowMillis
                        ),
                    sets = workoutSets
                )
            }

    val normalizedExercises = normalizeWorkoutExercises(rawExercises)
    return WorkoutFromRoutineResult(
        workout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = workoutId,
                        name = routine.routine.name,
                        routineId = routine.routine.id,
                        startedAt = nowMillis,
                        endedAt = null,
                        notes = routine.routine.notes,
                        status = WorkoutStatus.IN_PROGRESS,
                        createdAt = nowMillis,
                        updatedAt = nowMillis
                    ),
                exercises = normalizedExercises
            ),
        targetPlaceholdersBySetId = placeholders
    )
}

/**
 * Reconstruye el mapa de placeholders objetivo tras recuperar un entreno en curso desde Room
 * vinculando ejercicios por `(exerciseId, orden de aparición)` y series por posición.
 */
fun rebuildTargetPlaceholders(
    workout: WorkoutWithExercises,
    routine: RoutineWithExercises
): Map<String, SetTargetPlaceholder> {
    val routineExercisesByExerciseId =
        routine.exercises
            .sortedBy { it.routineExercise.position }
            .groupBy { it.routineExercise.exerciseId }
    val seenOccurrences = mutableMapOf<String, Int>()
    val placeholders = linkedMapOf<String, SetTargetPlaceholder>()

    for (workoutExWithSets in workout.exercises.sortedBy { it.workoutExercise.position }) {
        val exerciseId = workoutExWithSets.workoutExercise.exerciseId
        val occurrence = seenOccurrences.getOrDefault(exerciseId, 0)
        seenOccurrences[exerciseId] = occurrence + 1
        val matchingRoutineEx = routineExercisesByExerciseId[exerciseId]?.getOrNull(occurrence) ?: continue
        val routineSets = matchingRoutineEx.sets.sortedBy { it.position }
        for (workoutSet in workoutExWithSets.sets.sortedBy { it.position }) {
            val routineSet = routineSets.getOrNull(workoutSet.position) ?: continue
            val placeholder =
                SetTargetPlaceholder(
                    targetWeightKg = routineSet.targetWeightKg,
                    targetRepsMin = routineSet.targetRepsMin,
                    targetRepsMax = routineSet.targetRepsMax,
                    targetDurationS = routineSet.targetDurationS,
                    targetDistanceM = routineSet.targetDistanceM
                )
            if (placeholder.hasAnyTarget()) {
                placeholders[workoutSet.id] = placeholder
            }
        }
    }
    return placeholders
}

/**
 * Extrae las series completadas de la última sesión de un ejercicio y las alinea por índice
 * con las [currentSetCount] series del entreno activo.
 */
fun resolvePreviousSetsForExercise(
    lastSession: ExerciseHistoryEntry?,
    currentSetCount: Int
): List<PreviousSetSnapshot?> {
    if (currentSetCount <= 0) return emptyList()
    val completedSets =
        lastSession?.sets
            ?.filter { it.completed }
            ?.sortedBy { it.position }
            .orEmpty()
    return List(currentSetCount) { index ->
        completedSets.getOrNull(index)?.let { set ->
            PreviousSetSnapshot(
                setType = set.setType,
                weightKg = set.weightKg,
                reps = set.reps,
                rpe = set.rpe,
                durationS = set.durationS,
                distanceM = set.distanceM
            )
        }
    }
}

/**
 * Determina si al completar una serie de [completedWorkoutExerciseId] debe iniciarse el descanso.
 * En una superserie, el descanso solo arranca tras completar una serie del último ejercicio del grupo.
 */
fun shouldStartRestTimer(exercises: List<WorkoutExercise>, completedWorkoutExerciseId: String): Boolean {
    val sorted = exercises.sortedBy { it.position }
    val index = sorted.indexOfFirst { it.id == completedWorkoutExerciseId }
    if (index < 0) return false
    val group = sorted[index].supersetGroup?.takeIf { it.isNotBlank() } ?: return true
    val lastInGroupIndex =
        sorted.indexOfLast { it.supersetGroup?.takeIf { g -> g.isNotBlank() } == group }
    return index == lastInGroupIndex
}

fun resolveRestDurationSeconds(exerciseRestSeconds: Int?, defaultRestSeconds: Int): Int =
    (exerciseRestSeconds ?: defaultRestSeconds).coerceAtLeast(0)

/** Rellena los campos vacíos (`null`) de [set] con los objetivos de [placeholder] sin pisar valores editados. */
fun applyTargetPlaceholderIfBlank(set: WorkoutSet, placeholder: SetTargetPlaceholder?): WorkoutSet {
    if (placeholder == null) return set
    return set.copy(
        weightKg = set.weightKg ?: placeholder.targetWeightKg,
        reps = set.reps ?: (placeholder.targetRepsMin ?: placeholder.targetRepsMax),
        durationS = set.durationS ?: placeholder.targetDurationS,
        distanceM = set.distanceM ?: placeholder.targetDistanceM
    )
}

/**
 * Prepara un entreno en curso para cerrarlo en transacción como `COMPLETED`:
 * descarta las series pendientes o las marca aplicando placeholders según [incompleteStrategy].
 */
fun prepareWorkoutForCompletion(
    workout: WorkoutWithExercises,
    incompleteStrategy: IncompleteSetsStrategy,
    targetPlaceholdersBySetId: Map<String, SetTargetPlaceholder>,
    endedAtMillis: Long
): WorkoutWithExercises {
    val processedExercises =
        workout.exercises
            .sortedBy { it.workoutExercise.position }
            .mapNotNull { exWithSets ->
                val sortedSets = exWithSets.sets.sortedBy { it.position }
                val updatedSets =
                    when (incompleteStrategy) {
                        IncompleteSetsStrategy.DISCARD_INCOMPLETE ->
                            sortedSets
                                .filter { it.completed }
                                .mapIndexed { index, set ->
                                    if (set.position != index) {
                                        set.copy(position = index, updatedAt = endedAtMillis)
                                    } else {
                                        set
                                    }
                                }

                        IncompleteSetsStrategy.MARK_ALL_COMPLETED ->
                            sortedSets.mapIndexed { index, set ->
                                if (set.completed) {
                                    if (set.position != index) {
                                        set.copy(position = index, updatedAt = endedAtMillis)
                                    } else {
                                        set
                                    }
                                } else {
                                    applyTargetPlaceholderIfBlank(set, targetPlaceholdersBySetId[set.id]).copy(
                                        position = index,
                                        completed = true,
                                        completedAt = endedAtMillis,
                                        updatedAt = endedAtMillis
                                    )
                                }
                            }
                    }
                if (updatedSets.isEmpty()) {
                    null
                } else {
                    exWithSets.copy(sets = updatedSets)
                }
            }

    val normalizedExercises = normalizeWorkoutExercises(processedExercises, updatedAt = endedAtMillis)
    return WorkoutWithExercises(
        workout =
            workout.workout.copy(
                status = WorkoutStatus.COMPLETED,
                endedAt = endedAtMillis,
                updatedAt = endedAtMillis
            ),
        exercises = normalizedExercises
    )
}

/** Normaliza posiciones (`0..n-1`) y etiquetas de superserie (`A`, `B`, `C`…) en ejercicios de entreno. */
fun normalizeWorkoutExercises(
    exercises: List<WorkoutExerciseWithSets>,
    updatedAt: Long = 0L
): List<WorkoutExerciseWithSets> = normalizeSupersetGroups(
    items = exercises,
    getSupersetGroup = { it.workoutExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.workoutExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                workoutExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/** Reordena un ejercicio del entreno activo y normaliza posiciones y superseries. */
fun moveWorkoutExercise(
    exercises: List<WorkoutExerciseWithSets>,
    workoutExerciseId: String,
    direction: MoveDirection,
    updatedAt: Long = 0L
): List<WorkoutExerciseWithSets> {
    val fromIndex = exercises.indexOfFirst { it.workoutExercise.id == workoutExerciseId }
    if (fromIndex < 0) return normalizeWorkoutExercises(exercises, updatedAt)
    val toIndex =
        when (direction) {
            MoveDirection.UP -> fromIndex - 1
            MoveDirection.DOWN -> fromIndex + 1
        }
    if (toIndex !in exercises.indices) return normalizeWorkoutExercises(exercises, updatedAt)
    return moveSupersetItem(
        items = exercises,
        fromIndex = fromIndex,
        toIndex = toIndex,
        getSupersetGroup = { it.workoutExercise.supersetGroup },
        updateItem = { item, pos, group ->
            val current = item.workoutExercise
            if (current.position != pos || current.supersetGroup != group) {
                item.copy(
                    workoutExercise =
                        current.copy(
                            position = pos,
                            supersetGroup = group,
                            updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                        )
                )
            } else {
                item
            }
        }
    )
}

/**
 * Agrupa el ejercicio [workoutExerciseId] en superserie con su vecino inmediato ([direction]),
 * incorporando también a los miembros contiguos si el vecino ya pertenecía a una superserie.
 */
fun pairWorkoutExerciseWithAdjacentSuperset(
    exercises: List<WorkoutExerciseWithSets>,
    workoutExerciseId: String,
    direction: MoveDirection,
    updatedAt: Long = 0L
): List<WorkoutExerciseWithSets> {
    val index = exercises.indexOfFirst { it.workoutExercise.id == workoutExerciseId }
    if (index < 0) return normalizeWorkoutExercises(exercises, updatedAt)
    val neighborIndex =
        when (direction) {
            MoveDirection.UP -> index - 1
            MoveDirection.DOWN -> index + 1
        }
    if (neighborIndex !in exercises.indices) return normalizeWorkoutExercises(exercises, updatedAt)

    val selectedIds = linkedSetOf(workoutExerciseId, exercises[neighborIndex].workoutExercise.id)
    val neighborGroup = exercises[neighborIndex].workoutExercise.supersetGroup?.takeIf { it.isNotBlank() }
    if (neighborGroup != null) {
        exercises
            .filter { it.workoutExercise.supersetGroup == neighborGroup }
            .forEach { selectedIds.add(it.workoutExercise.id) }
    }

    return groupConsecutiveAsSuperset(
        items = exercises,
        selectedIds = selectedIds,
        getId = { it.workoutExercise.id },
        getSupersetGroup = { it.workoutExercise.supersetGroup },
        updateItem = { item, pos, group ->
            val current = item.workoutExercise
            if (current.position != pos || current.supersetGroup != group) {
                item.copy(
                    workoutExercise =
                        current.copy(
                            position = pos,
                            supersetGroup = group,
                            updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                        )
                )
            } else {
                item
            }
        }
    )
}

/** Saca un ejercicio del entreno de su superserie; si el grupo queda con 1 elemento se desagrupa. */
fun removeWorkoutExerciseFromSuperset(
    exercises: List<WorkoutExerciseWithSets>,
    workoutExerciseId: String,
    updatedAt: Long = 0L
): List<WorkoutExerciseWithSets> = removeItemFromSuperset(
    items = exercises,
    itemId = workoutExerciseId,
    getId = { it.workoutExercise.id },
    getSupersetGroup = { it.workoutExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.workoutExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                workoutExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/** Desagrupa por completo una superserie del entreno activo. */
fun ungroupWorkoutSuperset(
    exercises: List<WorkoutExerciseWithSets>,
    supersetGroup: String,
    updatedAt: Long = 0L
): List<WorkoutExerciseWithSets> = ungroupSupersetGroup(
    items = exercises,
    supersetGroup = supersetGroup,
    getSupersetGroup = { it.workoutExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.workoutExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                workoutExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/** Formatea una duración transcurrida en milisegundos como `MM:SS` o `H:MM:SS`. */
fun formatElapsedDuration(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/** Comparación entre el entreno recién terminado y la última sesión completada de la misma rutina. */
data class RoutineLastSessionComparison(
    val previousWorkoutId: String,
    val previousStartedAtMillis: Long,
    val previousDurationMillis: Long,
    val durationDeltaMillis: Long,
    val previousVolumeKg: Double,
    val volumeDeltaKg: Double,
    val previousCompletedSets: Int,
    val completedSetsDelta: Int
)

/** Calcula la diferencia de duración, volumen y series frente a la última vez que se hizo la misma rutina. */
fun computeRoutineComparison(
    currentWorkout: WorkoutWithExercises,
    previousWorkout: WorkoutWithExercises,
    trackingTypesByExerciseId: Map<String, TrackingType> = emptyMap()
): RoutineLastSessionComparison {
    val currentEnd = currentWorkout.workout.endedAt ?: currentWorkout.workout.updatedAt
    val currentDuration = (currentEnd - currentWorkout.workout.startedAt).coerceAtLeast(0L)
    val prevEnd = previousWorkout.workout.endedAt ?: previousWorkout.workout.updatedAt
    val prevDuration = (prevEnd - previousWorkout.workout.startedAt).coerceAtLeast(0L)

    val currentVolume = computeWorkoutVolumeKg(currentWorkout, trackingTypesByExerciseId)
    val prevVolume = computeWorkoutVolumeKg(previousWorkout, trackingTypesByExerciseId)

    val currentSets = computeCompletedSetsCount(currentWorkout)
    val prevSets = computeCompletedSetsCount(previousWorkout)

    return RoutineLastSessionComparison(
        previousWorkoutId = previousWorkout.workout.id,
        previousStartedAtMillis = previousWorkout.workout.startedAt,
        previousDurationMillis = prevDuration,
        durationDeltaMillis = currentDuration - prevDuration,
        previousVolumeKg = prevVolume,
        volumeDeltaKg = currentVolume - prevVolume,
        previousCompletedSets = prevSets,
        completedSetsDelta = currentSets - prevSets
    )
}

/**
 * Determina si en un entreno iniciado desde [routine] cambiaron pesos, repeticiones, duraciones,
 * distancias, tipos de serie o estructura de ejercicios/series con respecto a los objetivos de la rutina.
 */
fun hasWorkoutRoutineValuesChanged(workout: WorkoutWithExercises, routine: RoutineWithExercises): Boolean {
    if (workout.workout.routineId != routine.routine.id) return false

    val workoutExercises =
        workout.exercises
            .sortedBy { it.workoutExercise.position }
            .mapNotNull { ex ->
                val completedSets = ex.sets.filter { it.completed }.sortedBy { it.position }
                if (completedSets.isEmpty()) null else ex.copy(sets = completedSets)
            }
    val routineExercises = routine.exercises.sortedBy { it.routineExercise.position }

    if (workoutExercises.size != routineExercises.size) return true

    for (i in workoutExercises.indices) {
        val wEx = workoutExercises[i]
        val rEx = routineExercises[i]
        if (wEx.workoutExercise.exerciseId != rEx.routineExercise.exerciseId) return true
        val rSets = rEx.sets.sortedBy { it.position }
        if (wEx.sets.size != rSets.size) return true

        for (j in wEx.sets.indices) {
            val wSet = wEx.sets[j]
            val rSet = rSets[j]
            if (wSet.setType != rSet.setType) return true
            if (!sameNullableDouble(wSet.weightKg, rSet.targetWeightKg)) return true
            if (!sameRepsTarget(wSet.reps, rSet.targetRepsMin, rSet.targetRepsMax)) return true
            if (wSet.durationS != rSet.targetDurationS) return true
            if (!sameNullableDouble(wSet.distanceM, rSet.targetDistanceM)) return true
        }
    }
    return false
}

/**
 * Construye una copia de [routine] actualizada con los ejercicios, series completadas y valores
 * reales (`weightKg`, `reps`, `durationS`, `distanceM`, `setType`) registrados en [workout].
 */
fun applyWorkoutValuesToRoutine(
    routine: RoutineWithExercises,
    workout: WorkoutWithExercises,
    nowMillis: Long,
    idGenerator: () -> String = { UUID.randomUUID().toString() }
): RoutineWithExercises {
    val existingRoutineExercisesByExerciseId =
        routine.exercises
            .sortedBy { it.routineExercise.position }
            .groupBy { it.routineExercise.exerciseId }
    val seenOccurrences = mutableMapOf<String, Int>()

    val updatedExercises =
        workout.exercises
            .sortedBy { it.workoutExercise.position }
            .mapNotNull { wEx ->
                val completedSets = wEx.sets.filter { it.completed }.sortedBy { it.position }
                if (completedSets.isEmpty()) return@mapNotNull null

                val exerciseId = wEx.workoutExercise.exerciseId
                val occurrence = seenOccurrences.getOrDefault(exerciseId, 0)
                seenOccurrences[exerciseId] = occurrence + 1
                val matchingRoutineEx =
                    existingRoutineExercisesByExerciseId[exerciseId]?.getOrNull(occurrence)

                val routineExerciseId = matchingRoutineEx?.routineExercise?.id ?: idGenerator()
                val existingSets = matchingRoutineEx?.sets?.sortedBy { it.position }.orEmpty()

                val updatedSets =
                    completedSets.mapIndexed { setIndex, wSet ->
                        val existingSet = existingSets.getOrNull(setIndex)
                        val preservedRangeReps =
                            existingSet != null &&
                                wSet.reps != null &&
                                sameRepsTarget(wSet.reps, existingSet.targetRepsMin, existingSet.targetRepsMax)
                        RoutineSet(
                            id = existingSet?.id ?: idGenerator(),
                            routineExerciseId = routineExerciseId,
                            position = setIndex,
                            setType = wSet.setType,
                            targetWeightKg = wSet.weightKg,
                            targetRepsMin =
                                if (preservedRangeReps) existingSet.targetRepsMin else wSet.reps,
                            targetRepsMax =
                                if (preservedRangeReps) existingSet.targetRepsMax else wSet.reps,
                            targetDurationS = wSet.durationS,
                            targetDistanceM = wSet.distanceM,
                            createdAt = existingSet?.createdAt ?: nowMillis,
                            updatedAt = nowMillis
                        )
                    }

                RoutineExerciseWithSets(
                    routineExercise =
                        RoutineExercise(
                            id = routineExerciseId,
                            routineId = routine.routine.id,
                            exerciseId = exerciseId,
                            position = 0,
                            supersetGroup = wEx.workoutExercise.supersetGroup,
                            restSeconds = wEx.workoutExercise.restSeconds,
                            notes = wEx.workoutExercise.notes,
                            createdAt = matchingRoutineEx?.routineExercise?.createdAt ?: nowMillis,
                            updatedAt = nowMillis
                        ),
                    sets = updatedSets
                )
            }

    val normalizedExercises =
        normalizeSupersetGroups(
            items = updatedExercises,
            getSupersetGroup = { it.routineExercise.supersetGroup },
            updateItem = { item, pos, group ->
                item.copy(
                    routineExercise =
                        item.routineExercise.copy(
                            position = pos,
                            supersetGroup = group
                        )
                )
            }
        )

    return RoutineWithExercises(
        routine = routine.routine.copy(updatedAt = nowMillis),
        exercises = normalizedExercises
    )
}

private fun sameNullableDouble(a: Double?, b: Double?): Boolean = when {
    a == null && b == null -> true
    a == null || b == null -> false
    else -> abs(a - b) <= Records.EPSILON
}

private fun sameRepsTarget(actualReps: Int?, targetMin: Int?, targetMax: Int?): Boolean {
    if (actualReps == null) return targetMin == null && targetMax == null
    return when {
        targetMin != null && targetMax != null && targetMin == targetMax -> actualReps == targetMin
        targetMin != null && targetMax == null -> actualReps == targetMin
        targetMin == null && targetMax != null -> actualReps == targetMax
        else -> false
    }
}
