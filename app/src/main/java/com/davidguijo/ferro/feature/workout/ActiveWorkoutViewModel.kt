package com.davidguijo.ferro.feature.workout

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.IncompleteSetsStrategy
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.PreviousSetSnapshot
import com.davidguijo.ferro.core.domain.RestTimer
import com.davidguijo.ferro.core.domain.SetTargetPlaceholder
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeIncompleteSetsCount
import com.davidguijo.ferro.core.domain.computeNormalSetNumbers
import com.davidguijo.ferro.core.domain.computeTotalSetsCount
import com.davidguijo.ferro.core.domain.computeWorkoutVolumeKg
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import com.davidguijo.ferro.feature.routines.formatDistanceMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Textos editables locales de una fila de serie para no perder decimales al teclear. */
data class SetInputStrings(
    val weightInput: String = "",
    val repsInput: String = "",
    val durationInput: String = "",
    val distanceInput: String = ""
)

@Immutable
data class ActiveSetUi(
    val id: String,
    val setType: SetType,
    val normalSetNumber: Int,
    val previous: PreviousSetSnapshot?,
    val previousLabel: String?,
    val weightInput: String,
    val repsInput: String,
    val durationInput: String,
    val distanceInput: String,
    val weightPlaceholder: String,
    val repsPlaceholder: String,
    val durationPlaceholder: String,
    val distancePlaceholder: String,
    val rpeLabel: String?,
    val rpeValue: Double?,
    val completed: Boolean
)

@Immutable
data class ActiveExerciseUi(
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val muscle: com.davidguijo.ferro.core.domain.model.Muscle?,
    val trackingType: TrackingType,
    val supersetGroup: String?,
    val restSeconds: Int?,
    val notes: String,
    val showRpeColumn: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val canPairAbove: Boolean,
    val canPairBelow: Boolean,
    val sets: List<ActiveSetUi>
)

@Immutable
data class ActiveRestTimerUi(val remainingSeconds: Int, val remainingText: String, val progress: Float)

@Immutable
data class ActiveWorkoutClockUiState(val elapsedText: String = "00:00", val restTimer: ActiveRestTimerUi? = null)

@Immutable
data class ActiveSetTypePickerTarget(val workoutExerciseId: String, val setId: String, val currentType: SetType)

@Immutable
data class ActiveRpePickerTarget(val workoutExerciseId: String, val setId: String, val currentRpe: Double?)

@Immutable
data class ActiveWorkoutUiState(
    val isReady: Boolean = false,
    val hasActiveWorkout: Boolean = false,
    val workoutId: String? = null,
    val workoutName: String = "",
    val workoutNotes: String = "",
    val volumeText: String = "0 kg",
    val completedSetsCount: Int = 0,
    val totalSetsCount: Int = 0,
    val incompleteSetsCount: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = Settings.DEFAULT_REST_SECONDS,
    val isSpanish: Boolean = true,
    val exercises: List<ActiveExerciseUi> = emptyList(),
    val lastCompletedRestEventId: Long = 0L,
    val canScheduleExactAlarms: Boolean = true,
    val showWorkoutMenu: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showNotesDialog: Boolean = false,
    val activeExerciseMenuId: String? = null,
    val activeExerciseRestDialogId: String? = null,
    val activeSetTypePicker: ActiveSetTypePickerTarget? = null,
    val activeRpePicker: ActiveRpePickerTarget? = null,
    val showFinishIncompleteDialog: Boolean = false,
    val showDiscardConfirmDialog: Boolean = false,
    val conflictRoutineId: String? = null,
    val completedWorkoutIdForSummary: String? = null,
    val shouldNavigateBackAfterDiscard: Boolean = false
)

@HiltViewModel
class ActiveWorkoutViewModel
internal constructor(
    private val initialRoutineId: String?,
    private val savedStateHandle: SavedStateHandle?,
    private val activeWorkoutManager: ActiveWorkoutManager,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val systemLanguageProvider: () -> String,
    bodyMeasurementRepository: BodyMeasurementRepository? = null
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        activeWorkoutManager: ActiveWorkoutManager,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        bodyMeasurementRepository: BodyMeasurementRepository,
        clock: Clock
    ) : this(
        initialRoutineId = savedStateHandle.toRoute<ActiveWorkoutRoute>().routineId,
        savedStateHandle = savedStateHandle,
        activeWorkoutManager = activeWorkoutManager,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language },
        bodyMeasurementRepository = bodyMeasurementRepository
    )

    private val setInputOverrides = MutableStateFlow<Map<String, SetInputStrings>>(emptyMap())
    private val rpeVisibleExerciseIds = MutableStateFlow<Set<String>>(emptySet())
    private val overlay = MutableStateFlow(OverlayState())
    private val exactAlarmRefreshTick = MutableStateFlow(0)

    private var lastFormattingKey: Pair<WeightUnit, Boolean>? = null
    private var initializedEntry = false

    fun onExercisesSelected(selectedIds: List<String>) {
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            activeWorkoutManager.restoreIfNeeded()
            val pendingReplaceId =
                savedStateHandle?.get<String>(PENDING_REPLACE_WORKOUT_EXERCISE_ID_KEY)
            if (pendingReplaceId != null) {
                activeWorkoutManager.replaceExercise(
                    workoutExerciseId = pendingReplaceId,
                    newExerciseId = selectedIds.first()
                )
                savedStateHandle[PENDING_REPLACE_WORKOUT_EXERCISE_ID_KEY] = null
            } else {
                activeWorkoutManager.addExercises(selectedIds)
            }
        }
    }

    fun initializeIfNeeded(defaultWorkoutName: String) {
        if (initializedEntry) return
        initializedEntry = true
        viewModelScope.launch {
            activeWorkoutManager.restoreIfNeeded()
            val existing = activeWorkoutManager.state.value.workout
            when {
                initialRoutineId != null && existing != null -> {
                    overlay.update { it.copy(conflictRoutineId = initialRoutineId) }
                }

                initialRoutineId != null && existing == null -> {
                    activeWorkoutManager.startFromRoutine(initialRoutineId)
                }

                initialRoutineId == null && existing == null -> {
                    activeWorkoutManager.startEmptyWorkout(defaultWorkoutName)
                }
            }
        }
    }

    fun setScreenVisible(visible: Boolean) {
        activeWorkoutManager.setActiveWorkoutScreenVisible(visible)
        if (visible) {
            exactAlarmRefreshTick.update { it + 1 }
        }
    }

    fun refreshExactAlarmPermissionState() {
        exactAlarmRefreshTick.update { it + 1 }
    }

    fun onNotificationPermissionGranted() {
        activeWorkoutManager.syncNotificationForForeground()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val clockState: StateFlow<ActiveWorkoutClockUiState> =
        activeWorkoutManager.state
            .flatMapLatest { session ->
                val workout = session.workout ?: return@flatMapLatest flowOf(ActiveWorkoutClockUiState())
                val restTimer = session.restTimer
                flow {
                    while (currentCoroutineContext().isActive) {
                        val now = clock.millis()
                        val elapsed = formatElapsedDuration((now - workout.workout.startedAt).coerceAtLeast(0L))
                        val activeRest =
                            if (restTimer != null && !RestTimer.isFinished(restTimer, clock)) {
                                val remSec = RestTimer.remainingSeconds(restTimer, clock)
                                ActiveRestTimerUi(
                                    remainingSeconds = remSec,
                                    remainingText = RestTimer.formatRemaining(remSec),
                                    progress = RestTimer.progress(restTimer, clock)
                                )
                            } else {
                                if (restTimer != null) {
                                    activeWorkoutManager.onRestTimerExpired(
                                        expectedEndAtMillis = restTimer.endAtMillis,
                                        fromBackgroundReceiver = false
                                    )
                                }
                                null
                            }
                        emit(
                            ActiveWorkoutClockUiState(
                                elapsedText = elapsed,
                                restTimer = activeRest
                            )
                        )
                        delay(1_000L)
                    }
                }
            }.catch {
                emit(ActiveWorkoutClockUiState())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ActiveWorkoutClockUiState()
            )

    private val latestBodyWeightFlow =
        bodyMeasurementRepository?.observeLatestMeasurement()?.map { it?.weightKg } ?: flowOf(null)

    private val dataFlow =
        combine(
            combine(
                activeWorkoutManager.state,
                exerciseRepository.observeAllExercises(),
                settingsRepository.observeSettings(),
                latestBodyWeightFlow
            ) { session, allExercises, settings, bodyWeightKg ->
                Triple(session, allExercises.associateBy { it.id }, settings to bodyWeightKg)
            },
            setInputOverrides,
            rpeVisibleExerciseIds
        ) { (session, exercisesById, settingsAndWeight), overrides, rpeVisible ->
            CombinedData(
                session = session,
                exercisesById = exercisesById,
                settings = settingsAndWeight.first,
                latestBodyWeightKg = settingsAndWeight.second,
                inputOverrides = overrides,
                rpeVisibleIds = rpeVisible
            )
        }

    val uiState: StateFlow<ActiveWorkoutUiState> =
        combine(
            dataFlow,
            overlay,
            exactAlarmRefreshTick
        ) { data, uiOverlay, _ ->
            val session = data.session
            val workout = session.workout
            val isSpanish = resolveIsSpanish(data.settings.language, systemLanguageProvider())
            val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
            val weightUnit = data.settings.weightUnit

            val currentFormattingKey = weightUnit to isSpanish
            if (lastFormattingKey != null && lastFormattingKey != currentFormattingKey) {
                setInputOverrides.value = emptyMap()
            }
            lastFormattingKey = currentFormattingKey

            if (workout == null) {
                return@combine ActiveWorkoutUiState(
                    isReady = session.isRestored,
                    hasActiveWorkout = false,
                    weightUnit = weightUnit,
                    defaultRestSeconds = data.settings.defaultRestSeconds,
                    isSpanish = isSpanish,
                    lastCompletedRestEventId = session.lastCompletedRestEventId,
                    canScheduleExactAlarms = activeWorkoutManager.canScheduleExactAlarms(),
                    conflictRoutineId = uiOverlay.conflictRoutineId,
                    completedWorkoutIdForSummary = uiOverlay.completedWorkoutIdForSummary,
                    shouldNavigateBackAfterDiscard = uiOverlay.shouldNavigateBackAfterDiscard
                )
            }

            val trackingTypesById =
                data.exercisesById.mapValues { (_, ex) -> ex.trackingType }
            val volumeKg = computeWorkoutVolumeKg(workout, trackingTypesById, data.latestBodyWeightKg)
            val volumeText =
                Units.formatWeight(
                    weightKg = volumeKg,
                    unit = weightUnit,
                    locale = locale,
                    roundingStep = null,
                    includeUnit = true
                )

            val exercisesUi =
                buildExerciseUiList(
                    workout = workout,
                    session = session,
                    exercisesById = data.exercisesById,
                    overrides = data.inputOverrides,
                    rpeVisibleIds = data.rpeVisibleIds,
                    weightUnit = weightUnit,
                    isSpanish = isSpanish,
                    locale = locale
                )

            ActiveWorkoutUiState(
                isReady = session.isRestored,
                hasActiveWorkout = true,
                workoutId = workout.workout.id,
                workoutName = workout.workout.name,
                workoutNotes = workout.workout.notes,
                volumeText = volumeText,
                completedSetsCount = computeCompletedSetsCount(workout),
                totalSetsCount = computeTotalSetsCount(workout),
                incompleteSetsCount = computeIncompleteSetsCount(workout),
                weightUnit = weightUnit,
                defaultRestSeconds = data.settings.defaultRestSeconds,
                isSpanish = isSpanish,
                exercises = exercisesUi,
                lastCompletedRestEventId = session.lastCompletedRestEventId,
                canScheduleExactAlarms = activeWorkoutManager.canScheduleExactAlarms(),
                showWorkoutMenu = uiOverlay.showWorkoutMenu,
                showRenameDialog = uiOverlay.showRenameDialog,
                showNotesDialog = uiOverlay.showNotesDialog,
                activeExerciseMenuId = uiOverlay.activeExerciseMenuId,
                activeExerciseRestDialogId = uiOverlay.activeExerciseRestDialogId,
                activeSetTypePicker = uiOverlay.activeSetTypePicker,
                activeRpePicker = uiOverlay.activeRpePicker,
                showFinishIncompleteDialog = uiOverlay.showFinishIncompleteDialog,
                showDiscardConfirmDialog = uiOverlay.showDiscardConfirmDialog,
                conflictRoutineId = uiOverlay.conflictRoutineId,
                completedWorkoutIdForSummary = uiOverlay.completedWorkoutIdForSummary,
                shouldNavigateBackAfterDiscard = uiOverlay.shouldNavigateBackAfterDiscard
            )
        }.catch {
            emit(ActiveWorkoutUiState())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveWorkoutUiState()
        )

    fun onPrepareAddExercises() {
        savedStateHandle?.set(PENDING_REPLACE_WORKOUT_EXERCISE_ID_KEY, null)
    }

    fun onPrepareReplaceExercise(workoutExerciseId: String) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        savedStateHandle?.set(PENDING_REPLACE_WORKOUT_EXERCISE_ID_KEY, workoutExerciseId)
    }

    fun onWorkoutNameChanged(name: String) {
        activeWorkoutManager.updateWorkoutName(name)
        overlay.update { it.copy(showRenameDialog = false) }
    }

    fun onWorkoutNotesChanged(notes: String) {
        activeWorkoutManager.updateWorkoutNotes(notes)
        overlay.update { it.copy(showNotesDialog = false) }
    }

    fun onExerciseNotesChanged(workoutExerciseId: String, notes: String) {
        activeWorkoutManager.updateExerciseNotes(workoutExerciseId, notes)
    }

    fun onSetWeightChanged(workoutExerciseId: String, setId: String, value: String) {
        val unit = uiState.value.weightUnit
        updateInputOverride(setId) { it.copy(weightInput = value) }
        val parsedKg = Units.parseWeightToKg(value, unit)
        activeWorkoutManager.updateSetValues(
            workoutExerciseId = workoutExerciseId,
            setId = setId,
            weightKg = parsedKg,
            updateWeight = true
        )
    }

    fun onSetRepsChanged(workoutExerciseId: String, setId: String, value: String) {
        updateInputOverride(setId) { it.copy(repsInput = value) }
        val parsedReps = value.trim().toIntOrNull()?.takeIf { it >= 0 }
        activeWorkoutManager.updateSetValues(
            workoutExerciseId = workoutExerciseId,
            setId = setId,
            reps = parsedReps,
            updateReps = true
        )
    }

    fun onSetDurationChanged(workoutExerciseId: String, setId: String, value: String) {
        updateInputOverride(setId) { it.copy(durationInput = value) }
        val parsedDuration = value.trim().toIntOrNull()?.takeIf { it >= 0 }
        activeWorkoutManager.updateSetValues(
            workoutExerciseId = workoutExerciseId,
            setId = setId,
            durationS = parsedDuration,
            updateDuration = true
        )
    }

    fun onSetDistanceChanged(workoutExerciseId: String, setId: String, value: String) {
        updateInputOverride(setId) { it.copy(distanceInput = value) }
        val parsedDistance = Units.parseWeightInput(value)
        activeWorkoutManager.updateSetValues(
            workoutExerciseId = workoutExerciseId,
            setId = setId,
            distanceM = parsedDistance,
            updateDistance = true
        )
    }

    fun onCopyPreviousToSet(workoutExerciseId: String, setId: String, trackingType: TrackingType) {
        viewModelScope.launch {
            val updatedSet =
                activeWorkoutManager.copyPreviousToSet(
                    workoutExerciseId = workoutExerciseId,
                    setId = setId,
                    trackingType = trackingType
                ) ?: return@launch
            syncOverrideFromSet(updatedSet)
        }
    }

    fun onToggleSetCompleted(workoutExerciseId: String, setId: String) {
        viewModelScope.launch {
            val updatedSet = activeWorkoutManager.toggleSetCompleted(workoutExerciseId, setId) ?: return@launch
            syncOverrideFromSet(updatedSet)
        }
    }

    fun onAddSet(workoutExerciseId: String) {
        viewModelScope.launch {
            val created = activeWorkoutManager.addSet(workoutExerciseId) ?: return@launch
            syncOverrideFromSet(created)
        }
    }

    fun onRemoveSet(workoutExerciseId: String, setId: String) {
        setInputOverrides.update { it - setId }
        viewModelScope.launch {
            activeWorkoutManager.removeSet(workoutExerciseId, setId)
        }
    }

    fun onOpenSetTypePicker(workoutExerciseId: String, setId: String, currentType: SetType) {
        overlay.update {
            it.copy(
                activeSetTypePicker =
                    ActiveSetTypePickerTarget(
                        workoutExerciseId = workoutExerciseId,
                        setId = setId,
                        currentType = currentType
                    )
            )
        }
    }

    fun onDismissSetTypePicker() {
        overlay.update { it.copy(activeSetTypePicker = null) }
    }

    fun onSelectSetType(setType: SetType) {
        val target = overlay.value.activeSetTypePicker ?: return
        overlay.update { it.copy(activeSetTypePicker = null) }
        viewModelScope.launch {
            activeWorkoutManager.updateSetType(target.workoutExerciseId, target.setId, setType)
        }
    }

    fun onToggleExerciseRpeColumn(workoutExerciseId: String) {
        rpeVisibleExerciseIds.update { current ->
            if (workoutExerciseId in current) current - workoutExerciseId else current + workoutExerciseId
        }
    }

    fun onOpenRpePicker(workoutExerciseId: String, setId: String, currentRpe: Double?) {
        overlay.update {
            it.copy(
                activeRpePicker =
                    ActiveRpePickerTarget(
                        workoutExerciseId = workoutExerciseId,
                        setId = setId,
                        currentRpe = currentRpe
                    )
            )
        }
    }

    fun onDismissRpePicker() {
        overlay.update { it.copy(activeRpePicker = null) }
    }

    fun onSelectRpe(rpe: Double?) {
        val target = overlay.value.activeRpePicker ?: return
        overlay.update { it.copy(activeRpePicker = null) }
        viewModelScope.launch {
            activeWorkoutManager.updateSetRpe(target.workoutExerciseId, target.setId, rpe)
        }
    }

    fun onOpenExerciseMenu(workoutExerciseId: String) {
        overlay.update { it.copy(activeExerciseMenuId = workoutExerciseId) }
    }

    fun onDismissExerciseMenu() {
        overlay.update { it.copy(activeExerciseMenuId = null) }
    }

    fun onOpenExerciseRestDialog(workoutExerciseId: String) {
        overlay.update {
            it.copy(
                activeExerciseMenuId = null,
                activeExerciseRestDialogId = workoutExerciseId
            )
        }
    }

    fun onDismissExerciseRestDialog() {
        overlay.update { it.copy(activeExerciseRestDialogId = null) }
    }

    fun onSetExerciseRestSeconds(workoutExerciseId: String, restSeconds: Int?) {
        overlay.update { it.copy(activeExerciseRestDialogId = null) }
        viewModelScope.launch {
            activeWorkoutManager.updateExerciseRest(workoutExerciseId, restSeconds)
        }
    }

    fun onMoveExercise(workoutExerciseId: String, direction: MoveDirection) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        viewModelScope.launch {
            activeWorkoutManager.moveExercise(workoutExerciseId, direction)
        }
    }

    fun onPairSuperset(workoutExerciseId: String, direction: MoveDirection) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        viewModelScope.launch {
            activeWorkoutManager.pairExerciseWithAdjacentSuperset(workoutExerciseId, direction)
        }
    }

    fun onRemoveFromSuperset(workoutExerciseId: String) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        viewModelScope.launch {
            activeWorkoutManager.removeExerciseFromSuperset(workoutExerciseId)
        }
    }

    fun onUngroupSuperset(supersetGroup: String) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        viewModelScope.launch {
            activeWorkoutManager.ungroupSuperset(supersetGroup)
        }
    }

    fun onRemoveExercise(workoutExerciseId: String) {
        overlay.update { it.copy(activeExerciseMenuId = null) }
        viewModelScope.launch {
            activeWorkoutManager.removeExercise(workoutExerciseId)
        }
    }

    fun onAdjustRestTimer(deltaSeconds: Int) {
        viewModelScope.launch {
            activeWorkoutManager.adjustRestTimer(deltaSeconds)
        }
    }

    fun onSkipRestTimer() {
        viewModelScope.launch {
            activeWorkoutManager.skipRestTimer()
        }
    }

    fun onStartManualRestTimer() {
        overlay.update { it.copy(showWorkoutMenu = false) }
        viewModelScope.launch {
            activeWorkoutManager.startManualRestTimer(uiState.value.defaultRestSeconds)
        }
    }

    fun onOpenWorkoutMenu() {
        overlay.update { it.copy(showWorkoutMenu = true) }
    }

    fun onDismissWorkoutMenu() {
        overlay.update { it.copy(showWorkoutMenu = false) }
    }

    fun onOpenRenameDialog() {
        overlay.update { it.copy(showWorkoutMenu = false, showRenameDialog = true) }
    }

    fun onDismissRenameDialog() {
        overlay.update { it.copy(showRenameDialog = false) }
    }

    fun onOpenNotesDialog() {
        overlay.update { it.copy(showWorkoutMenu = false, showNotesDialog = true) }
    }

    fun onDismissNotesDialog() {
        overlay.update { it.copy(showNotesDialog = false) }
    }

    fun onRequestFinishWorkout() {
        val currentState = uiState.value
        if (!currentState.hasActiveWorkout) return
        if (currentState.incompleteSetsCount > 0) {
            overlay.update { it.copy(showFinishIncompleteDialog = true) }
        } else {
            finishWithStrategy(IncompleteSetsStrategy.DISCARD_INCOMPLETE)
        }
    }

    fun onDismissFinishIncompleteDialog() {
        overlay.update { it.copy(showFinishIncompleteDialog = false) }
    }

    fun onConfirmFinishIncomplete(strategy: IncompleteSetsStrategy) {
        overlay.update { it.copy(showFinishIncompleteDialog = false) }
        finishWithStrategy(strategy)
    }

    fun onRequestDiscardWorkout() {
        overlay.update { it.copy(showWorkoutMenu = false, showDiscardConfirmDialog = true) }
    }

    fun onDismissDiscardDialog() {
        overlay.update { it.copy(showDiscardConfirmDialog = false) }
    }

    fun onConfirmDiscardWorkout() {
        overlay.update { it.copy(showDiscardConfirmDialog = false) }
        viewModelScope.launch {
            activeWorkoutManager.discardActiveWorkout()
            setInputOverrides.value = emptyMap()
            overlay.update { it.copy(shouldNavigateBackAfterDiscard = true) }
        }
    }

    fun onResumeExistingInConflict() {
        overlay.update { it.copy(conflictRoutineId = null) }
    }

    fun onDiscardAndStartConflictRoutine() {
        val routineId = overlay.value.conflictRoutineId ?: return
        overlay.update { it.copy(conflictRoutineId = null) }
        setInputOverrides.value = emptyMap()
        viewModelScope.launch {
            activeWorkoutManager.startFromRoutine(routineId)
        }
    }

    fun onNavigatedAfterFinishOrDiscard() {
        overlay.update {
            it.copy(
                completedWorkoutIdForSummary = null,
                shouldNavigateBackAfterDiscard = false
            )
        }
    }

    private fun finishWithStrategy(strategy: IncompleteSetsStrategy) {
        viewModelScope.launch {
            val finishedId = activeWorkoutManager.finishWorkout(strategy)
            setInputOverrides.value = emptyMap()
            overlay.update { it.copy(completedWorkoutIdForSummary = finishedId) }
        }
    }

    private fun updateInputOverride(setId: String, transform: (SetInputStrings) -> SetInputStrings) {
        val currentState = uiState.value
        val existingSet =
            currentState.exercises
                .asSequence()
                .flatMap { it.sets.asSequence() }
                .firstOrNull { it.id == setId }
        val base =
            setInputOverrides.value[setId] ?: SetInputStrings(
                weightInput = existingSet?.weightInput.orEmpty(),
                repsInput = existingSet?.repsInput.orEmpty(),
                durationInput = existingSet?.durationInput.orEmpty(),
                distanceInput = existingSet?.distanceInput.orEmpty()
            )
        setInputOverrides.update { current ->
            current + (setId to transform(base))
        }
    }

    private fun syncOverrideFromSet(set: WorkoutSet) {
        val state = uiState.value
        val locale = if (state.isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
        setInputOverrides.update { current ->
            current + (set.id to set.toInputStrings(state.weightUnit, state.isSpanish, locale))
        }
    }

    private fun buildExerciseUiList(
        workout: WorkoutWithExercises,
        session: ActiveWorkoutSessionState,
        exercisesById: Map<String, Exercise>,
        overrides: Map<String, SetInputStrings>,
        rpeVisibleIds: Set<String>,
        weightUnit: WeightUnit,
        isSpanish: Boolean,
        locale: Locale
    ): List<ActiveExerciseUi> = workout.exercises.mapIndexed { index, exWithSets ->
        val exId = exWithSets.workoutExercise.id
        val exercise = exercisesById[exWithSets.workoutExercise.exerciseId]
        val trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
        val normalNumbers = computeNormalSetNumbers(exWithSets.sets.map { it.setType })
        val previousSnapshots = session.previousSetsByWorkoutExerciseId[exId].orEmpty()
        val hasAnyRpe = exWithSets.sets.any { it.rpe != null }
        val showRpe = hasAnyRpe || exId in rpeVisibleIds

        val setsUi =
            exWithSets.sets.mapIndexed { setIndex, set ->
                val override = overrides[set.id]
                val baseline = set.toInputStrings(weightUnit, isSpanish, locale)
                val inputs = override ?: baseline
                val placeholder = session.targetPlaceholdersBySetId[set.id]
                val previous = previousSnapshots.getOrNull(setIndex)
                ActiveSetUi(
                    id = set.id,
                    setType = set.setType,
                    normalSetNumber = normalNumbers.getOrElse(setIndex) { setIndex + 1 },
                    previous = previous,
                    previousLabel =
                        previous?.let {
                            formatPreviousSnapshot(
                                snapshot = it,
                                trackingType = trackingType,
                                weightUnit = weightUnit,
                                isSpanish = isSpanish,
                                locale = locale
                            )
                        },
                    weightInput = inputs.weightInput,
                    repsInput = inputs.repsInput,
                    durationInput = inputs.durationInput,
                    distanceInput = inputs.distanceInput,
                    weightPlaceholder =
                        placeholder?.targetWeightKg?.let { kg ->
                            Units.formatWeight(kg, weightUnit, locale, includeUnit = false)
                        } ?: "",
                    repsPlaceholder = formatRepsPlaceholder(placeholder),
                    durationPlaceholder = placeholder?.targetDurationS?.toString() ?: "",
                    distancePlaceholder =
                        placeholder?.targetDistanceM?.let { meters ->
                            formatDistanceMeters(meters, isSpanish)
                        } ?: "",
                    rpeLabel = set.rpe?.let { formatRpeValue(it, locale) },
                    rpeValue = set.rpe,
                    completed = set.completed
                )
            }

        ActiveExerciseUi(
            workoutExerciseId = exId,
            exerciseId = exWithSets.workoutExercise.exerciseId,
            name = exercise?.localizedName(isSpanish) ?: exWithSets.workoutExercise.exerciseId,
            muscle = exercise?.primaryMuscle,
            trackingType = trackingType,
            supersetGroup = exWithSets.workoutExercise.supersetGroup,
            restSeconds = exWithSets.workoutExercise.restSeconds,
            notes = exWithSets.workoutExercise.notes,
            showRpeColumn = showRpe,
            canMoveUp = index > 0,
            canMoveDown = index < workout.exercises.lastIndex,
            canPairAbove = index > 0 && exWithSets.workoutExercise.supersetGroup == null,
            canPairBelow = index < workout.exercises.lastIndex && exWithSets.workoutExercise.supersetGroup == null,
            sets = setsUi
        )
    }

    private fun WorkoutSet.toInputStrings(weightUnit: WeightUnit, isSpanish: Boolean, locale: Locale): SetInputStrings =
        SetInputStrings(
            weightInput =
                weightKg?.let { kg ->
                    Units.formatWeight(kg, weightUnit, locale, includeUnit = false)
                } ?: "",
            repsInput = reps?.toString() ?: "",
            durationInput = durationS?.toString() ?: "",
            distanceInput = distanceM?.let { meters -> formatDistanceMeters(meters, isSpanish) } ?: ""
        )

    private fun formatRepsPlaceholder(placeholder: SetTargetPlaceholder?): String {
        if (placeholder == null) return ""
        val min = placeholder.targetRepsMin
        val max = placeholder.targetRepsMax
        return when {
            min != null && max != null && min != max -> "$min-$max"
            min != null -> min.toString()
            max != null -> max.toString()
            else -> ""
        }
    }

    private fun formatPreviousSnapshot(
        snapshot: PreviousSetSnapshot,
        trackingType: TrackingType,
        weightUnit: WeightUnit,
        isSpanish: Boolean,
        locale: Locale
    ): String? {
        val formattedWeight =
            snapshot.weightKg?.let { kg ->
                Units.formatWeight(kg, weightUnit, locale, includeUnit = false)
            }
        return when (trackingType) {
            TrackingType.WEIGHT_REPS ->
                when {
                    formattedWeight != null && snapshot.reps != null -> "$formattedWeight × ${snapshot.reps}"
                    snapshot.reps != null -> snapshot.reps.toString()
                    else -> null
                }

            TrackingType.WEIGHTED_BODYWEIGHT ->
                when {
                    formattedWeight != null && snapshot.reps != null -> "+$formattedWeight × ${snapshot.reps}"
                    snapshot.reps != null -> snapshot.reps.toString()
                    else -> null
                }

            TrackingType.ASSISTED_BODYWEIGHT ->
                when {
                    formattedWeight != null && snapshot.reps != null -> "-$formattedWeight × ${snapshot.reps}"
                    snapshot.reps != null -> snapshot.reps.toString()
                    else -> null
                }

            TrackingType.BODYWEIGHT_REPS -> snapshot.reps?.toString()

            TrackingType.DURATION -> snapshot.durationS?.let { "$it s" }

            TrackingType.DISTANCE_DURATION ->
                when {
                    snapshot.distanceM != null && snapshot.durationS != null ->
                        "${formatDistanceMeters(snapshot.distanceM, isSpanish)} m · ${snapshot.durationS} s"

                    snapshot.distanceM != null -> "${formatDistanceMeters(snapshot.distanceM, isSpanish)} m"

                    snapshot.durationS != null -> "${snapshot.durationS} s"

                    else -> null
                }
        }
    }

    private fun formatRpeValue(rpe: Double, locale: Locale): String {
        val formatter =
            DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale)).apply {
                isGroupingUsed = false
            }
        return formatter.format(rpe)
    }

    private data class CombinedData(
        val session: ActiveWorkoutSessionState,
        val exercisesById: Map<String, Exercise>,
        val settings: Settings,
        val latestBodyWeightKg: Double?,
        val inputOverrides: Map<String, SetInputStrings>,
        val rpeVisibleIds: Set<String>
    )

    private data class OverlayState(
        val showWorkoutMenu: Boolean = false,
        val showRenameDialog: Boolean = false,
        val showNotesDialog: Boolean = false,
        val activeExerciseMenuId: String? = null,
        val activeExerciseRestDialogId: String? = null,
        val activeSetTypePicker: ActiveSetTypePickerTarget? = null,
        val activeRpePicker: ActiveRpePickerTarget? = null,
        val showFinishIncompleteDialog: Boolean = false,
        val showDiscardConfirmDialog: Boolean = false,
        val conflictRoutineId: String? = null,
        val completedWorkoutIdForSummary: String? = null,
        val shouldNavigateBackAfterDiscard: Boolean = false
    )

    companion object {
        private const val PENDING_REPLACE_WORKOUT_EXERCISE_ID_KEY = "pending_replace_workout_exercise_id"
        val RPE_OPTIONS: List<Double> = listOf(6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
    }
}
