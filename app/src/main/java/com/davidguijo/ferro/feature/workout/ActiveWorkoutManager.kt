package com.davidguijo.ferro.feature.workout

import com.davidguijo.ferro.core.data.di.ApplicationScope
import com.davidguijo.ferro.core.domain.IncompleteSetsStrategy
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.PreviousSetSnapshot
import com.davidguijo.ferro.core.domain.RestTimer
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.SetTargetPlaceholder
import com.davidguijo.ferro.core.domain.applyTargetPlaceholderIfBlank
import com.davidguijo.ferro.core.domain.createEmptyWorkout
import com.davidguijo.ferro.core.domain.createWorkoutFromRoutine
import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.moveWorkoutExercise
import com.davidguijo.ferro.core.domain.normalizeWorkoutExercises
import com.davidguijo.ferro.core.domain.pairWorkoutExerciseWithAdjacentSuperset
import com.davidguijo.ferro.core.domain.prepareWorkoutForCompletion
import com.davidguijo.ferro.core.domain.rebuildTargetPlaceholders
import com.davidguijo.ferro.core.domain.removeWorkoutExerciseFromSuperset
import com.davidguijo.ferro.core.domain.repository.ActiveRestTimerStore
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolvePreviousSetsForExercise
import com.davidguijo.ferro.core.domain.resolveRestDurationSeconds
import com.davidguijo.ferro.core.domain.shouldStartRestTimer
import com.davidguijo.ferro.core.domain.ungroupWorkoutSuperset
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActiveWorkoutSessionState(
    val isRestored: Boolean = false,
    val workout: WorkoutWithExercises? = null,
    val targetPlaceholdersBySetId: Map<String, SetTargetPlaceholder> = emptyMap(),
    val previousSetsByWorkoutExerciseId: Map<String, List<PreviousSetSnapshot?>> = emptyMap(),
    val restTimer: RestTimerState? = null,
    val lastCompletedRestEventId: Long = 0L
)

/**
 * Gestor único (@Singleton) del entreno activo en curso.
 * Mantiene el estado reactivo en memoria, agrupa escrituras frecuentes en Room (debounce corto)
 * y garantiza la recuperación completa si el sistema mata el proceso.
 */
@Singleton
class ActiveWorkoutManager
internal constructor(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    private val settingsRepository: SettingsRepository,
    private val activeRestTimerStore: ActiveRestTimerStore,
    private val notificationController: WorkoutNotificationController,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val debounceMillis: Long
) {
    @Inject
    constructor(
        workoutRepository: WorkoutRepository,
        routineRepository: RoutineRepository,
        personalRecordRepository: PersonalRecordRepository,
        settingsRepository: SettingsRepository,
        activeRestTimerStore: ActiveRestTimerStore,
        notificationController: WorkoutNotificationController,
        clock: Clock,
        @ApplicationScope scope: CoroutineScope
    ) : this(
        workoutRepository = workoutRepository,
        routineRepository = routineRepository,
        personalRecordRepository = personalRecordRepository,
        settingsRepository = settingsRepository,
        activeRestTimerStore = activeRestTimerStore,
        notificationController = notificationController,
        clock = clock,
        scope = scope,
        debounceMillis = DEFAULT_DEBOUNCE_MILLIS
    )

    private val _state = MutableStateFlow(ActiveWorkoutSessionState())
    val state: StateFlow<ActiveWorkoutSessionState> = _state.asStateFlow()

    private val restoreMutex = Mutex()
    private val saveMutex = Mutex()
    private val restMutex = Mutex()
    private val debounceLock = Any()

    private var debounceJob: Job? = null
    private var lastSessionsByWorkoutExerciseId: Map<String, ExerciseHistoryEntry?> = emptyMap()
    private var lastHandledRestEndMillis: Long = -1L

    @Volatile
    private var isWorkoutScreenVisible: Boolean = false

    fun hasActiveWorkout(): Boolean = _state.value.workout != null

    fun canScheduleExactAlarms(): Boolean = notificationController.canScheduleExactAlarms()

    fun setActiveWorkoutScreenVisible(visible: Boolean) {
        isWorkoutScreenVisible = visible
        if (visible) {
            notificationController.cancelRestFinishedNotification()
        }
    }

    /** Re-sincroniza el servicio en primer plano y la notificación cuando la app entra en primer plano. */
    fun syncNotificationForForeground() {
        val current = _state.value
        val workout = current.workout ?: return
        notificationController.syncWorkoutNotification(workout, current.restTimer)
    }

    /** Recupera un entreno `IN_PROGRESS` desde Room y el descanso desde DataStore tras arrancar el proceso. */
    suspend fun restoreIfNeeded() {
        if (_state.value.isRestored) return
        restoreMutex.withLock {
            if (_state.value.isRestored) return
            val activeWorkout = workoutRepository.getActiveWorkout()
            if (activeWorkout == null) {
                val orphanTimer = activeRestTimerStore.load()
                if (orphanTimer != null) {
                    activeRestTimerStore.save(null)
                    notificationController.cancelRestAlarm()
                }
                _state.update { it.copy(isRestored = true) }
                return
            }

            val restoredWorkout =
                activeWorkout.copy(
                    exercises = normalizeWorkoutExercises(activeWorkout.exercises)
                )

            val placeholders =
                restoredWorkout.workout.routineId?.let { routineId ->
                    routineRepository.getRoutineWithExercises(routineId)?.let { routine ->
                        rebuildTargetPlaceholders(restoredWorkout, routine)
                    }
                }.orEmpty()

            val historyEntries = loadHistoryEntries(restoredWorkout)
            lastSessionsByWorkoutExerciseId = historyEntries
            val previousMap = buildPreviousMap(restoredWorkout, historyEntries)

            val savedTimer = activeRestTimerStore.load()
            val restoredTimer =
                if (savedTimer != null &&
                    savedTimer.workoutId == restoredWorkout.workout.id &&
                    !RestTimer.isFinished(savedTimer, clock)
                ) {
                    savedTimer
                } else {
                    if (savedTimer != null) {
                        activeRestTimerStore.save(null)
                        notificationController.cancelRestAlarm()
                    }
                    null
                }

            _state.update {
                it.copy(
                    isRestored = true,
                    workout = restoredWorkout,
                    targetPlaceholdersBySetId = placeholders,
                    previousSetsByWorkoutExerciseId = previousMap,
                    restTimer = restoredTimer
                )
            }

            if (restoredTimer != null) {
                notificationController.scheduleRestAlarm(
                    workoutId = restoredWorkout.workout.id,
                    workoutName = restoredWorkout.workout.name,
                    endAtMillis = restoredTimer.endAtMillis
                )
            }
        }
    }

    /** Inicia un entreno vacío (si ya existe uno en curso, lo descarta primero). */
    suspend fun startEmptyWorkout(defaultName: String): WorkoutWithExercises {
        restoreIfNeeded()
        discardExistingIfAnyInternal()

        val now = clock.millis()
        val created = createEmptyWorkout(defaultName = defaultName, nowMillis = now)
        lastSessionsByWorkoutExerciseId = emptyMap()
        _state.update {
            it.copy(
                isRestored = true,
                workout = created,
                targetPlaceholdersBySetId = emptyMap(),
                previousSetsByWorkoutExerciseId = emptyMap(),
                restTimer = null
            )
        }
        flushNow()
        notificationController.syncWorkoutNotification(created, restTimer = null)
        return created
    }

    /** Inicia un entreno copiando ejercicios, superseries, notas, descansos y objetivos desde una rutina. */
    suspend fun startFromRoutine(routineId: String): WorkoutWithExercises? {
        restoreIfNeeded()
        val routine = routineRepository.getRoutineWithExercises(routineId) ?: return null
        discardExistingIfAnyInternal()

        val now = clock.millis()
        val result = createWorkoutFromRoutine(routine = routine, nowMillis = now)
        val historyEntries = loadHistoryEntries(result.workout)
        lastSessionsByWorkoutExerciseId = historyEntries
        val previousMap = buildPreviousMap(result.workout, historyEntries)

        _state.update {
            it.copy(
                isRestored = true,
                workout = result.workout,
                targetPlaceholdersBySetId = result.targetPlaceholdersBySetId,
                previousSetsByWorkoutExerciseId = previousMap,
                restTimer = null
            )
        }
        flushNow()
        notificationController.syncWorkoutNotification(result.workout, restTimer = null)
        return result.workout
    }

    fun updateWorkoutName(name: String) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(name = name, updatedAt = now)
            )
        }
        scheduleDebouncedSave()
        _state.value.let { current ->
            current.workout?.let { notificationController.syncWorkoutNotification(it, current.restTimer) }
        }
    }

    fun updateWorkoutNotes(notes: String) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(notes = notes, updatedAt = now)
            )
        }
        scheduleDebouncedSave()
    }

    fun updateExerciseNotes(workoutExerciseId: String, notes: String) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                exercises =
                    workout.exercises.map { ex ->
                        if (ex.workoutExercise.id == workoutExerciseId) {
                            ex.copy(workoutExercise = ex.workoutExercise.copy(notes = notes, updatedAt = now))
                        } else {
                            ex
                        }
                    }
            )
        }
        scheduleDebouncedSave()
    }

    suspend fun updateExerciseRest(workoutExerciseId: String, restSeconds: Int?) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                exercises =
                    workout.exercises.map { ex ->
                        if (ex.workoutExercise.id == workoutExerciseId) {
                            ex.copy(
                                workoutExercise =
                                    ex.workoutExercise.copy(restSeconds = restSeconds, updatedAt = now)
                            )
                        } else {
                            ex
                        }
                    }
            )
        }
        flushNow()
    }

    suspend fun addExercises(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        val currentWorkout = _state.value.workout ?: return
        val now = clock.millis()
        val startPos = currentWorkout.exercises.size

        val addedExercises =
            exerciseIds.mapIndexed { offset, exerciseId ->
                val workoutExerciseId = UUID.randomUUID().toString()
                val setId = UUID.randomUUID().toString()
                WorkoutExerciseWithSets(
                    workoutExercise =
                        WorkoutExercise(
                            id = workoutExerciseId,
                            workoutId = currentWorkout.workout.id,
                            exerciseId = exerciseId,
                            position = startPos + offset,
                            supersetGroup = null,
                            restSeconds = null,
                            notes = "",
                            createdAt = now,
                            updatedAt = now
                        ),
                    sets =
                        listOf(
                            WorkoutSet(
                                id = setId,
                                workoutExerciseId = workoutExerciseId,
                                position = 0,
                                setType = SetType.NORMAL,
                                weightKg = null,
                                reps = null,
                                rpe = null,
                                durationS = null,
                                distanceM = null,
                                completed = false,
                                completedAt = null,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                )
            }

        val newHistoryEntries =
            addedExercises.associate { ex ->
                ex.workoutExercise.id to
                    workoutRepository.getLastSessionForExercise(
                        exerciseId = ex.workoutExercise.exerciseId,
                        excludeWorkoutId = currentWorkout.workout.id
                    )
            }
        lastSessionsByWorkoutExerciseId = lastSessionsByWorkoutExerciseId + newHistoryEntries

        _state.update { current ->
            val workout = current.workout ?: return@update current
            val updatedExercises = normalizeWorkoutExercises(workout.exercises + addedExercises, updatedAt = now)
            val updatedWorkout =
                workout.copy(
                    workout = workout.workout.copy(updatedAt = now),
                    exercises = updatedExercises
                )
            current.copy(
                workout = updatedWorkout,
                previousSetsByWorkoutExerciseId = buildPreviousMap(updatedWorkout, lastSessionsByWorkoutExerciseId)
            )
        }
        flushNow()
    }

    /** Reemplaza el ejercicio de [workoutExerciseId] por [newExerciseId] conservando todas sus series. */
    suspend fun replaceExercise(workoutExerciseId: String, newExerciseId: String) {
        val currentWorkout = _state.value.workout ?: return
        val target = currentWorkout.exercises.firstOrNull { it.workoutExercise.id == workoutExerciseId } ?: return
        if (target.workoutExercise.exerciseId == newExerciseId) return

        val now = clock.millis()
        val newHistory =
            workoutRepository.getLastSessionForExercise(
                exerciseId = newExerciseId,
                excludeWorkoutId = currentWorkout.workout.id
            )
        lastSessionsByWorkoutExerciseId = lastSessionsByWorkoutExerciseId + (workoutExerciseId to newHistory)
        val removedSetIds = target.sets.map { it.id }.toSet()

        _state.update { current ->
            val workout = current.workout ?: return@update current
            val updatedExercises =
                workout.exercises.map { ex ->
                    if (ex.workoutExercise.id == workoutExerciseId) {
                        ex.copy(
                            workoutExercise =
                                ex.workoutExercise.copy(
                                    exerciseId = newExerciseId,
                                    updatedAt = now
                                )
                        )
                    } else {
                        ex
                    }
                }
            val updatedWorkout =
                workout.copy(
                    workout = workout.workout.copy(updatedAt = now),
                    exercises = updatedExercises
                )
            current.copy(
                workout = updatedWorkout,
                targetPlaceholdersBySetId = current.targetPlaceholdersBySetId - removedSetIds,
                previousSetsByWorkoutExerciseId = buildPreviousMap(updatedWorkout, lastSessionsByWorkoutExerciseId)
            )
        }
        flushNow()
    }

    suspend fun removeExercise(workoutExerciseId: String) {
        val now = clock.millis()
        val removedSetIds =
            _state.value.workout?.exercises
                ?.firstOrNull { it.workoutExercise.id == workoutExerciseId }
                ?.sets?.map { it.id }?.toSet()
                .orEmpty()
        lastSessionsByWorkoutExerciseId = lastSessionsByWorkoutExerciseId - workoutExerciseId

        _state.update { current ->
            val workout = current.workout ?: return@update current
            val filtered = workout.exercises.filterNot { it.workoutExercise.id == workoutExerciseId }
            val normalized = normalizeWorkoutExercises(filtered, updatedAt = now)
            val updatedWorkout =
                workout.copy(
                    workout = workout.workout.copy(updatedAt = now),
                    exercises = normalized
                )
            current.copy(
                workout = updatedWorkout,
                targetPlaceholdersBySetId = current.targetPlaceholdersBySetId - removedSetIds,
                previousSetsByWorkoutExerciseId = current.previousSetsByWorkoutExerciseId - workoutExerciseId
            )
        }
        flushNow()
        syncNotificationForForeground()
    }

    suspend fun moveExercise(workoutExerciseId: String, direction: MoveDirection) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(updatedAt = now),
                exercises = moveWorkoutExercise(workout.exercises, workoutExerciseId, direction, updatedAt = now)
            )
        }
        flushNow()
    }

    suspend fun pairExerciseWithAdjacentSuperset(workoutExerciseId: String, direction: MoveDirection) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(updatedAt = now),
                exercises =
                    pairWorkoutExerciseWithAdjacentSuperset(
                        exercises = workout.exercises,
                        workoutExerciseId = workoutExerciseId,
                        direction = direction,
                        updatedAt = now
                    )
            )
        }
        flushNow()
    }

    suspend fun removeExerciseFromSuperset(workoutExerciseId: String) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(updatedAt = now),
                exercises =
                    removeWorkoutExerciseFromSuperset(
                        exercises = workout.exercises,
                        workoutExerciseId = workoutExerciseId,
                        updatedAt = now
                    )
            )
        }
        flushNow()
    }

    suspend fun ungroupSuperset(supersetGroup: String) {
        val now = clock.millis()
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(updatedAt = now),
                exercises =
                    ungroupWorkoutSuperset(
                        exercises = workout.exercises,
                        supersetGroup = supersetGroup,
                        updatedAt = now
                    )
            )
        }
        flushNow()
    }

    /** Añade una serie al ejercicio copiando los valores de la serie anterior (o vacía si no hay ninguna). */
    suspend fun addSet(workoutExerciseId: String): WorkoutSet? {
        val now = clock.millis()
        var createdSet: WorkoutSet? = null
        _state.update { current ->
            val workout = current.workout ?: return@update current
            var updatedPlaceholders = current.targetPlaceholdersBySetId
            val updatedExercises =
                workout.exercises.map { ex ->
                    if (ex.workoutExercise.id == workoutExerciseId) {
                        val lastSet = ex.sets.lastOrNull()
                        val newSetId = UUID.randomUUID().toString()
                        val newSet =
                            WorkoutSet(
                                id = newSetId,
                                workoutExerciseId = workoutExerciseId,
                                position = ex.sets.size,
                                setType = SetType.NORMAL,
                                weightKg = lastSet?.weightKg,
                                reps = lastSet?.reps,
                                rpe = lastSet?.rpe,
                                durationS = lastSet?.durationS,
                                distanceM = lastSet?.distanceM,
                                completed = false,
                                completedAt = null,
                                createdAt = now,
                                updatedAt = now
                            )
                        createdSet = newSet
                        val lastPlaceholder = lastSet?.let { current.targetPlaceholdersBySetId[it.id] }
                        if (lastPlaceholder != null) {
                            updatedPlaceholders = updatedPlaceholders + (newSetId to lastPlaceholder)
                        }
                        ex.copy(sets = ex.sets + newSet)
                    } else {
                        ex
                    }
                }
            val updatedWorkout =
                workout.copy(
                    workout = workout.workout.copy(updatedAt = now),
                    exercises = updatedExercises
                )
            current.copy(
                workout = updatedWorkout,
                targetPlaceholdersBySetId = updatedPlaceholders,
                previousSetsByWorkoutExerciseId = buildPreviousMap(updatedWorkout, lastSessionsByWorkoutExerciseId)
            )
        }
        flushNow()
        return createdSet
    }

    suspend fun removeSet(workoutExerciseId: String, setId: String) {
        val now = clock.millis()
        _state.update { current ->
            val workout = current.workout ?: return@update current
            val updatedExercises =
                workout.exercises.map { ex ->
                    if (ex.workoutExercise.id == workoutExerciseId) {
                        val remaining =
                            ex.sets
                                .filterNot { it.id == setId }
                                .mapIndexed { idx, set ->
                                    if (set.position != idx) set.copy(position = idx, updatedAt = now) else set
                                }
                        ex.copy(sets = remaining)
                    } else {
                        ex
                    }
                }
            val updatedWorkout =
                workout.copy(
                    workout = workout.workout.copy(updatedAt = now),
                    exercises = updatedExercises
                )
            current.copy(
                workout = updatedWorkout,
                targetPlaceholdersBySetId = current.targetPlaceholdersBySetId - setId,
                previousSetsByWorkoutExerciseId = buildPreviousMap(updatedWorkout, lastSessionsByWorkoutExerciseId)
            )
        }
        flushNow()
        syncNotificationForForeground()
    }

    suspend fun updateSetType(workoutExerciseId: String, setId: String, setType: SetType) {
        val now = clock.millis()
        mutateSet(workoutExerciseId, setId) { set ->
            set.copy(setType = setType, updatedAt = now)
        }
        flushNow()
    }

    fun updateSetValues(
        workoutExerciseId: String,
        setId: String,
        weightKg: Double? = null,
        updateWeight: Boolean = false,
        reps: Int? = null,
        updateReps: Boolean = false,
        durationS: Int? = null,
        updateDuration: Boolean = false,
        distanceM: Double? = null,
        updateDistance: Boolean = false
    ) {
        val now = clock.millis()
        mutateSet(workoutExerciseId, setId) { set ->
            set.copy(
                weightKg = if (updateWeight) weightKg else set.weightKg,
                reps = if (updateReps) reps else set.reps,
                durationS = if (updateDuration) durationS else set.durationS,
                distanceM = if (updateDistance) distanceM else set.distanceM,
                updatedAt = now
            )
        }
        scheduleDebouncedSave()
    }

    suspend fun updateSetRpe(workoutExerciseId: String, setId: String, rpe: Double?) {
        val now = clock.millis()
        mutateSet(workoutExerciseId, setId) { set ->
            set.copy(rpe = rpe, updatedAt = now)
        }
        flushNow()
    }

    /** Copia los valores de la columna «Anterior» a la serie indicada y persiste inmediatamente. */
    suspend fun copyPreviousToSet(workoutExerciseId: String, setId: String, trackingType: TrackingType): WorkoutSet? {
        val current = _state.value
        val workout = current.workout ?: return null
        val exIndex = workout.exercises.indexOfFirst { it.workoutExercise.id == workoutExerciseId }
        if (exIndex < 0) return null
        val sets = workout.exercises[exIndex].sets
        val setIndex = sets.indexOfFirst { it.id == setId }
        if (setIndex < 0) return null
        val previous = current.previousSetsByWorkoutExerciseId[workoutExerciseId]?.getOrNull(setIndex) ?: return null

        val now = clock.millis()
        var resultingSet: WorkoutSet? = null
        mutateSet(workoutExerciseId, setId) { set ->
            val updated =
                when (trackingType) {
                    TrackingType.WEIGHT_REPS,
                    TrackingType.WEIGHTED_BODYWEIGHT,
                    TrackingType.ASSISTED_BODYWEIGHT ->
                        set.copy(
                            weightKg = previous.weightKg ?: set.weightKg,
                            reps = previous.reps ?: set.reps,
                            updatedAt = now
                        )

                    TrackingType.BODYWEIGHT_REPS ->
                        set.copy(
                            reps = previous.reps ?: set.reps,
                            updatedAt = now
                        )

                    TrackingType.DURATION ->
                        set.copy(
                            durationS = previous.durationS ?: set.durationS,
                            updatedAt = now
                        )

                    TrackingType.DISTANCE_DURATION ->
                        set.copy(
                            distanceM = previous.distanceM ?: set.distanceM,
                            durationS = previous.durationS ?: set.durationS,
                            updatedAt = now
                        )
                }
            resultingSet = updated
            updated
        }
        flushNow()
        return resultingSet
    }

    /**
     * Marca o desmarca una serie como completada.
     * Al marcarla: rellena campos vacíos con objetivos de rutina si existen y arranca el descanso
     * (salvo en superseries, donde solo arranca al completar el último ejercicio del grupo).
     */
    suspend fun toggleSetCompleted(workoutExerciseId: String, setId: String): WorkoutSet? {
        val current = _state.value
        val workout = current.workout ?: return null
        val exerciseWithSets =
            workout.exercises.firstOrNull { it.workoutExercise.id == workoutExerciseId } ?: return null
        val targetSet = exerciseWithSets.sets.firstOrNull { it.id == setId } ?: return null
        val now = clock.millis()
        val becomingCompleted = !targetSet.completed

        val updatedSet =
            if (becomingCompleted) {
                val placeholder = current.targetPlaceholdersBySetId[setId]
                applyTargetPlaceholderIfBlank(targetSet, placeholder).copy(
                    completed = true,
                    completedAt = now,
                    updatedAt = now
                )
            } else {
                targetSet.copy(
                    completed = false,
                    completedAt = null,
                    updatedAt = now
                )
            }

        mutateSet(workoutExerciseId, setId) { updatedSet }
        flushNow()

        val updatedWorkout = _state.value.workout
        if (becomingCompleted && updatedWorkout != null) {
            val shouldRest =
                shouldStartRestTimer(
                    exercises = updatedWorkout.exercises.map { it.workoutExercise },
                    completedWorkoutExerciseId = workoutExerciseId
                )
            if (shouldRest) {
                val settings = settingsRepository.observeSettings().first()
                val restSeconds =
                    resolveRestDurationSeconds(
                        exerciseRestSeconds = exerciseWithSets.workoutExercise.restSeconds,
                        defaultRestSeconds = settings.defaultRestSeconds
                    )
                if (restSeconds > 0) {
                    startRestTimerInternal(
                        workout = updatedWorkout,
                        exerciseId = exerciseWithSets.workoutExercise.exerciseId,
                        durationSeconds = restSeconds
                    )
                    return updatedSet
                }
            }
        }

        if (updatedWorkout != null) {
            notificationController.syncWorkoutNotification(updatedWorkout, _state.value.restTimer)
        }
        return updatedSet
    }

    suspend fun startManualRestTimer(durationSeconds: Int, exerciseId: String? = null) {
        val workout = _state.value.workout ?: return
        startRestTimerInternal(workout = workout, exerciseId = exerciseId, durationSeconds = durationSeconds)
    }

    suspend fun adjustRestTimer(deltaSeconds: Int) {
        restMutex.withLock {
            val current = _state.value
            val workout = current.workout ?: return
            val existing = current.restTimer ?: return
            val adjusted = RestTimer.adjust(existing, deltaSeconds, clock)
            if (adjusted == null) {
                activeRestTimerStore.save(null)
                notificationController.cancelRestAlarm()
                notificationController.cancelRestFinishedNotification()
                _state.update { it.copy(restTimer = null) }
                notificationController.syncWorkoutNotification(workout, restTimer = null)
            } else {
                if (lastHandledRestEndMillis == adjusted.endAtMillis) {
                    lastHandledRestEndMillis = -1L
                }
                activeRestTimerStore.save(adjusted)
                _state.update { it.copy(restTimer = adjusted) }
                notificationController.scheduleRestAlarm(
                    workoutId = workout.workout.id,
                    workoutName = workout.workout.name,
                    endAtMillis = adjusted.endAtMillis
                )
                notificationController.syncWorkoutNotification(workout, restTimer = adjusted)
            }
        }
    }

    suspend fun skipRestTimer() {
        restMutex.withLock {
            val current = _state.value
            val workout = current.workout
            activeRestTimerStore.save(null)
            notificationController.cancelRestAlarm()
            notificationController.cancelRestFinishedNotification()
            _state.update { it.copy(restTimer = null) }
            if (workout != null) {
                notificationController.syncWorkoutNotification(workout, restTimer = null)
            }
        }
    }

    /**
     * Gestiona el vencimiento del descanso de forma idempotente y deduplicada entre UI, servicio y alarma.
     */
    suspend fun onRestTimerExpired(expectedEndAtMillis: Long? = null, fromBackgroundReceiver: Boolean = false) {
        var expiredWorkoutName: String? = null
        var screenVisibleSnapshot = false

        restMutex.withLock {
            val current = _state.value
            val activeTimer = current.restTimer ?: return
            if (expectedEndAtMillis != null && activeTimer.endAtMillis != expectedEndAtMillis) {
                return
            }
            if (!RestTimer.isFinished(activeTimer, clock)) {
                return
            }
            if (lastHandledRestEndMillis == activeTimer.endAtMillis) {
                return
            }
            lastHandledRestEndMillis = activeTimer.endAtMillis
            expiredWorkoutName = current.workout?.workout?.name.orEmpty()
            screenVisibleSnapshot = isWorkoutScreenVisible

            activeRestTimerStore.save(null)
            notificationController.cancelRestAlarm()
            _state.update {
                it.copy(
                    restTimer = null,
                    lastCompletedRestEventId = it.lastCompletedRestEventId + 1L
                )
            }
            val workoutAfter = _state.value.workout
            if (workoutAfter != null && (!fromBackgroundReceiver || screenVisibleSnapshot)) {
                notificationController.syncWorkoutNotification(workoutAfter, restTimer = null)
            }
        }

        val workoutName = expiredWorkoutName ?: return
        val settings = settingsRepository.observeSettings().first()
        notificationController.notifyRestFinished(
            workoutName = workoutName,
            settings = settings,
            isWorkoutScreenVisible = screenVisibleSnapshot
        )
    }

    /** Finaliza y guarda el entreno en transacción (`COMPLETED`, `endedAt`) y limpia la notificación. */
    suspend fun finishWorkout(incompleteStrategy: IncompleteSetsStrategy): String? {
        cancelPendingSave()
        return saveMutex.withLock {
            val current = _state.value
            val currentWorkout = current.workout ?: return@withLock null
            val now = clock.millis()
            val completed =
                prepareWorkoutForCompletion(
                    workout = currentWorkout,
                    incompleteStrategy = incompleteStrategy,
                    targetPlaceholdersBySetId = current.targetPlaceholdersBySetId,
                    endedAtMillis = now
                )
            workoutRepository.saveWorkoutWithExercises(completed)
            personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(completed)
            clearRestAndNotificationsLocked()
            lastSessionsByWorkoutExerciseId = emptyMap()
            _state.update {
                it.copy(
                    workout = null,
                    targetPlaceholdersBySetId = emptyMap(),
                    previousSetsByWorkoutExerciseId = emptyMap(),
                    restTimer = null
                )
            }
            completed.workout.id
        }
    }

    /** Descarta el entreno activo (`DISCARDED`) y quita la notificación persistente. */
    suspend fun discardActiveWorkout() {
        cancelPendingSave()
        saveMutex.withLock {
            discardExistingLocked()
        }
    }

    /** Fuerza el volcado inmediato a Room de cualquier cambio pendiente. */
    suspend fun flushNow() {
        cancelPendingSave()
        saveMutex.withLock {
            val workout = _state.value.workout ?: return@withLock
            if (workout.workout.status == WorkoutStatus.IN_PROGRESS) {
                workoutRepository.saveWorkoutWithExercises(workout)
            }
        }
    }

    private suspend fun startRestTimerInternal(
        workout: WorkoutWithExercises,
        exerciseId: String?,
        durationSeconds: Int
    ) {
        restMutex.withLock {
            val timer =
                RestTimer.start(
                    workoutId = workout.workout.id,
                    exerciseId = exerciseId,
                    durationSeconds = durationSeconds,
                    clock = clock
                ) ?: return
            if (lastHandledRestEndMillis == timer.endAtMillis) {
                lastHandledRestEndMillis = -1L
            }
            activeRestTimerStore.save(timer)
            notificationController.cancelRestFinishedNotification()
            _state.update { it.copy(restTimer = timer) }
            notificationController.scheduleRestAlarm(
                workoutId = workout.workout.id,
                workoutName = workout.workout.name,
                endAtMillis = timer.endAtMillis
            )
            notificationController.syncWorkoutNotification(workout, restTimer = timer)
        }
    }

    private suspend fun discardExistingIfAnyInternal() {
        cancelPendingSave()
        saveMutex.withLock {
            discardExistingLocked()
        }
    }

    private suspend fun discardExistingLocked() {
        val currentWorkout = _state.value.workout
        if (currentWorkout != null) {
            workoutRepository.updateWorkoutStatus(
                workoutId = currentWorkout.workout.id,
                status = WorkoutStatus.DISCARDED,
                endedAt = clock.millis()
            )
        } else {
            val dbActive = workoutRepository.getActiveWorkout()
            if (dbActive != null) {
                workoutRepository.updateWorkoutStatus(
                    workoutId = dbActive.workout.id,
                    status = WorkoutStatus.DISCARDED,
                    endedAt = clock.millis()
                )
            }
        }
        clearRestAndNotificationsLocked()
        lastSessionsByWorkoutExerciseId = emptyMap()
        _state.update {
            it.copy(
                workout = null,
                targetPlaceholdersBySetId = emptyMap(),
                previousSetsByWorkoutExerciseId = emptyMap(),
                restTimer = null
            )
        }
    }

    private suspend fun clearRestAndNotificationsLocked() {
        restMutex.withLock {
            activeRestTimerStore.save(null)
            notificationController.cancelRestAlarm()
            notificationController.cancelRestFinishedNotification()
            notificationController.stopWorkoutNotification()
        }
    }

    private fun scheduleDebouncedSave() {
        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob =
                scope.launch {
                    if (debounceMillis > 0L) {
                        delay(debounceMillis)
                    }
                    if (!isActive) return@launch
                    saveMutex.withLock {
                        val workout = _state.value.workout ?: return@withLock
                        if (workout.workout.status == WorkoutStatus.IN_PROGRESS) {
                            workoutRepository.saveWorkoutWithExercises(workout)
                        }
                    }
                }
        }
    }

    private fun cancelPendingSave() {
        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob = null
        }
    }

    private fun mutateWorkout(transform: (WorkoutWithExercises) -> WorkoutWithExercises) {
        _state.update { current ->
            val workout = current.workout ?: return@update current
            current.copy(workout = transform(workout))
        }
    }

    private fun mutateSet(workoutExerciseId: String, setId: String, transform: (WorkoutSet) -> WorkoutSet) {
        mutateWorkout { workout ->
            workout.copy(
                workout = workout.workout.copy(updatedAt = clock.millis()),
                exercises =
                    workout.exercises.map { ex ->
                        if (ex.workoutExercise.id == workoutExerciseId) {
                            ex.copy(
                                sets =
                                    ex.sets.map { set ->
                                        if (set.id == setId) transform(set) else set
                                    }
                            )
                        } else {
                            ex
                        }
                    }
            )
        }
    }

    private suspend fun loadHistoryEntries(workout: WorkoutWithExercises): Map<String, ExerciseHistoryEntry?> =
        workout.exercises.associate { ex ->
            ex.workoutExercise.id to
                workoutRepository.getLastSessionForExercise(
                    exerciseId = ex.workoutExercise.exerciseId,
                    excludeWorkoutId = workout.workout.id
                )
        }

    private fun buildPreviousMap(
        workout: WorkoutWithExercises,
        historyEntries: Map<String, ExerciseHistoryEntry?>
    ): Map<String, List<PreviousSetSnapshot?>> = workout.exercises.associate { ex ->
        val entry = historyEntries[ex.workoutExercise.id]
        ex.workoutExercise.id to resolvePreviousSetsForExercise(entry, ex.sets.size)
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS: Long = 250L
    }
}
