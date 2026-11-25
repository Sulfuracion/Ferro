package com.davidguijo.ferro.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.Records
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeNormalSetNumbers
import com.davidguijo.ferro.core.domain.computeWorkoutVolumeKg
import com.davidguijo.ferro.core.domain.createRoutineFromWorkout
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionDetailSetUi(
    val id: String,
    val setType: SetType,
    val normalSetNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val durationS: Int?,
    val distanceM: Double?,
    val completed: Boolean,
    val recordTypes: List<RecordType>
) {
    val isRecord: Boolean
        get() = recordTypes.isNotEmpty()
}

data class SessionDetailExerciseUi(
    val id: String,
    val exerciseId: String,
    val name: String,
    val trackingType: TrackingType,
    val supersetGroup: String?,
    val notes: String,
    val sets: List<SessionDetailSetUi>
)

data class EditSetDraft(
    val id: String,
    val setType: SetType,
    val weightText: String,
    val repsText: String,
    val rpeText: String,
    val durationText: String,
    val distanceText: String,
    val completed: Boolean
)

data class EditExerciseDraft(
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val trackingType: TrackingType,
    val supersetGroup: String?,
    val restSeconds: Int?,
    val notes: String,
    val sets: List<EditSetDraft>
)

data class EditSessionDraft(
    val name: String,
    val startedAtMillis: Long,
    val durationMinutesText: String,
    val notes: String,
    val exercises: List<EditExerciseDraft>
)

data class SessionDetailUiState(
    val workoutId: String = "",
    val isLoaded: Boolean = false,
    val workoutFound: Boolean = false,
    val name: String = "",
    val dateTimeText: String = "",
    val durationText: String? = null,
    val volumeText: String = "0 kg",
    val completedSetsCount: Int = 0,
    val totalRecordsCount: Int = 0,
    val notes: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isSpanish: Boolean = true,
    val exercises: List<SessionDetailExerciseUi> = emptyList(),
    val editDraft: EditSessionDraft? = null,
    val isSavingEdit: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleted: Boolean = false,
    val showSaveRoutineDialog: Boolean = false,
    val saveRoutineNameDraft: String = "",
    val savedRoutineName: String? = null,
    val showRepeatConflictDialog: Boolean = false,
    val activeWorkoutName: String? = null,
    val shouldOpenActiveWorkout: Boolean = false
) {
    val isEditing: Boolean
        get() = editDraft != null
}

private data class SessionDetailOverlay(
    val editDraft: EditSessionDraft? = null,
    val isSavingEdit: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleted: Boolean = false,
    val showSaveRoutineDialog: Boolean = false,
    val saveRoutineNameDraft: String = "",
    val savedRoutineName: String? = null,
    val showRepeatConflictDialog: Boolean = false,
    val shouldOpenActiveWorkout: Boolean = false
)

@HiltViewModel
class SessionDetailViewModel
internal constructor(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val activeWorkoutManager: ActiveWorkoutManager,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val systemLanguageProvider: () -> String,
    bodyMeasurementRepository: BodyMeasurementRepository? = null
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        workoutRepository: WorkoutRepository,
        personalRecordRepository: PersonalRecordRepository,
        routineRepository: RoutineRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        activeWorkoutManager: ActiveWorkoutManager,
        bodyMeasurementRepository: BodyMeasurementRepository,
        clock: Clock
    ) : this(
        workoutId = savedStateHandle.toRoute<SessionDetailRoute>().workoutId,
        workoutRepository = workoutRepository,
        personalRecordRepository = personalRecordRepository,
        routineRepository = routineRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        activeWorkoutManager = activeWorkoutManager,
        clock = clock,
        zoneId = ZoneId.systemDefault(),
        systemLanguageProvider = { Locale.getDefault().language },
        bodyMeasurementRepository = bodyMeasurementRepository
    )

    private val overlay = MutableStateFlow(SessionDetailOverlay())
    private var latestWorkout: WorkoutWithExercises? = null
    private var latestExercisesById: Map<String, Exercise> = emptyMap()
    private var latestSettings: Settings = Settings()
    private var latestIsSpanish: Boolean = true

    private val latestBodyWeightFlow =
        bodyMeasurementRepository?.observeLatestMeasurement()?.map { it?.weightKg } ?: flowOf(null)

    val uiState: StateFlow<SessionDetailUiState> =
        combine(
            combine(
                workoutRepository.observeWorkoutWithExercises(workoutId),
                personalRecordRepository.observeRecordsForWorkout(workoutId),
                exerciseRepository.observeAllExercises(),
                settingsRepository.observeSettings(),
                latestBodyWeightFlow
            ) { workout, records, exercises, settings, latestBodyWeightKg ->
                LoadedSessionBundle(workout, records, exercises, settings, latestBodyWeightKg)
            },
            activeWorkoutManager.state,
            overlay
        ) { bundle, activeSession, ov ->
            val workout = bundle.workout
            val exercisesById = bundle.exercises.associateBy { it.id }
            val settings = bundle.settings
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())

            latestWorkout = workout
            latestExercisesById = exercisesById
            latestSettings = settings
            latestIsSpanish = isSpanish

            if (workout == null) {
                SessionDetailUiState(
                    workoutId = workoutId,
                    isLoaded = true,
                    workoutFound = false,
                    weightUnit = settings.weightUnit,
                    isSpanish = isSpanish,
                    isDeleted = ov.isDeleted
                )
            } else {
                buildUiState(
                    workout = workout,
                    records = bundle.records,
                    exercisesById = exercisesById,
                    settings = settings,
                    latestBodyWeightKg = bundle.latestBodyWeightKg,
                    isSpanish = isSpanish,
                    activeWorkoutName = activeSession.workout?.workout?.name,
                    overlay = ov
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionDetailUiState(workoutId = workoutId)
        )

    fun onEnterEditMode() {
        val workout = latestWorkout ?: return
        val draft = buildEditDraft(workout, latestExercisesById, latestSettings.weightUnit, latestIsSpanish)
        overlay.update { it.copy(editDraft = draft) }
    }

    fun onCancelEdit() {
        overlay.update { it.copy(editDraft = null) }
    }

    fun onEditNameChange(value: String) {
        updateDraft { it.copy(name = value) }
    }

    fun onEditDateSelected(selectedUtcDayMillis: Long) {
        updateDraft { draft ->
            val newDate = Instant.ofEpochMilli(selectedUtcDayMillis).atZone(ZoneOffset.UTC).toLocalDate()
            val currentLocalTime = Instant.ofEpochMilli(draft.startedAtMillis).atZone(zoneId).toLocalTime()
            val updatedMillis = newDate.atTime(currentLocalTime).atZone(zoneId).toInstant().toEpochMilli()
            draft.copy(startedAtMillis = updatedMillis)
        }
    }

    fun onEditDurationMinutesChange(value: String) {
        updateDraft { it.copy(durationMinutesText = value) }
    }

    fun onEditNotesChange(value: String) {
        updateDraft { it.copy(notes = value) }
    }

    fun onEditExerciseNotesChange(workoutExerciseId: String, notes: String) {
        updateDraft { draft ->
            draft.copy(
                exercises =
                    draft.exercises.map { ex ->
                        if (ex.workoutExerciseId == workoutExerciseId) ex.copy(notes = notes) else ex
                    }
            )
        }
    }

    fun onCycleEditSetType(workoutExerciseId: String, setId: String) {
        updateSet(workoutExerciseId, setId) { set ->
            val nextType =
                when (set.setType) {
                    SetType.NORMAL -> SetType.WARMUP
                    SetType.WARMUP -> SetType.FAILURE
                    SetType.FAILURE -> SetType.DROP
                    SetType.DROP -> SetType.NORMAL
                }
            set.copy(setType = nextType)
        }
    }

    fun onEditSetWeightChange(workoutExerciseId: String, setId: String, value: String) {
        updateSet(workoutExerciseId, setId) { it.copy(weightText = value) }
    }

    fun onEditSetRepsChange(workoutExerciseId: String, setId: String, value: String) {
        updateSet(workoutExerciseId, setId) { it.copy(repsText = value) }
    }

    fun onEditSetRpeChange(workoutExerciseId: String, setId: String, value: String) {
        updateSet(workoutExerciseId, setId) { it.copy(rpeText = value) }
    }

    fun onEditSetDurationChange(workoutExerciseId: String, setId: String, value: String) {
        updateSet(workoutExerciseId, setId) { it.copy(durationText = value) }
    }

    fun onEditSetDistanceChange(workoutExerciseId: String, setId: String, value: String) {
        updateSet(workoutExerciseId, setId) { it.copy(distanceText = value) }
    }

    fun onToggleEditSetCompleted(workoutExerciseId: String, setId: String) {
        updateSet(workoutExerciseId, setId) { it.copy(completed = !it.completed) }
    }

    fun onAddEditSet(workoutExerciseId: String) {
        updateDraft { draft ->
            draft.copy(
                exercises =
                    draft.exercises.map { ex ->
                        if (ex.workoutExerciseId != workoutExerciseId) {
                            ex
                        } else {
                            val lastSet = ex.sets.lastOrNull()
                            val newSet =
                                EditSetDraft(
                                    id = UUID.randomUUID().toString(),
                                    setType = SetType.NORMAL,
                                    weightText = lastSet?.weightText.orEmpty(),
                                    repsText = lastSet?.repsText.orEmpty(),
                                    rpeText = "",
                                    durationText = lastSet?.durationText.orEmpty(),
                                    distanceText = lastSet?.distanceText.orEmpty(),
                                    completed = true
                                )
                            ex.copy(sets = ex.sets + newSet)
                        }
                    }
            )
        }
    }

    fun onRemoveEditSet(workoutExerciseId: String, setId: String) {
        updateDraft { draft ->
            draft.copy(
                exercises =
                    draft.exercises.map { ex ->
                        if (ex.workoutExerciseId == workoutExerciseId) {
                            ex.copy(sets = ex.sets.filterNot { it.id == setId })
                        } else {
                            ex
                        }
                    }
            )
        }
    }

    fun onSaveEdit() {
        val original = latestWorkout ?: return
        val draft = overlay.value.editDraft ?: return
        if (overlay.value.isSavingEdit) return

        overlay.update { it.copy(isSavingEdit = true) }
        viewModelScope.launch {
            val now = clock.millis()
            val durationMinutes = draft.durationMinutesText.trim().toLongOrNull()?.coerceAtLeast(0L)
            val newStartedAt = draft.startedAtMillis
            val newEndedAt =
                if (durationMinutes != null) {
                    newStartedAt + durationMinutes * 60_000L
                } else {
                    val oldDuration =
                        original.workout.endedAt?.let { (it - original.workout.startedAt).coerceAtLeast(0L) }
                    oldDuration?.let { newStartedAt + it }
                }

            val unit = latestSettings.weightUnit
            val originalExercisesById = original.exercises.associateBy { it.workoutExercise.id }
            val updatedExercises =
                draft.exercises.mapIndexed { exIndex, exDraft ->
                    val origEx = originalExercisesById[exDraft.workoutExerciseId]
                    val origSetsById = origEx?.sets?.associateBy { it.id }.orEmpty()
                    val updatedSets =
                        exDraft.sets.mapIndexed { setIndex, setDraft ->
                            val origSet = origSetsById[setDraft.id]
                            val parsedWeightKg = Units.parseWeightToKg(setDraft.weightText, unit)
                            val parsedReps = setDraft.repsText.trim().toIntOrNull()?.takeIf { it >= 0 }
                            val parsedRpe = setDraft.rpeText.trim().replace(
                                ',',
                                '.'
                            ).toDoubleOrNull()?.coerceIn(1.0, 10.0)
                            val parsedDuration = setDraft.durationText.trim().toIntOrNull()?.takeIf { it >= 0 }
                            val parsedDistance =
                                setDraft.distanceText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }
                            WorkoutSet(
                                id = setDraft.id,
                                workoutExerciseId = exDraft.workoutExerciseId,
                                position = setIndex,
                                setType = setDraft.setType,
                                weightKg = parsedWeightKg,
                                reps = parsedReps,
                                rpe = parsedRpe,
                                durationS = parsedDuration,
                                distanceM = parsedDistance,
                                completed = setDraft.completed,
                                completedAt =
                                    if (setDraft.completed) {
                                        origSet?.completedAt ?: newEndedAt ?: newStartedAt
                                    } else {
                                        null
                                    },
                                createdAt = origSet?.createdAt ?: now,
                                updatedAt = now
                            )
                        }

                    WorkoutExerciseWithSets(
                        workoutExercise =
                            origEx?.workoutExercise?.copy(
                                position = exIndex,
                                notes = exDraft.notes.trim(),
                                updatedAt = now
                            ) ?: com.davidguijo.ferro.core.domain.model.WorkoutExercise(
                                id = exDraft.workoutExerciseId,
                                workoutId = original.workout.id,
                                exerciseId = exDraft.exerciseId,
                                position = exIndex,
                                supersetGroup = exDraft.supersetGroup,
                                restSeconds = exDraft.restSeconds,
                                notes = exDraft.notes.trim(),
                                createdAt = now,
                                updatedAt = now
                            ),
                        sets = updatedSets
                    )
                }

            val updatedWorkout =
                WorkoutWithExercises(
                    workout =
                        original.workout.copy(
                            name = draft.name.trim().ifEmpty { original.workout.name },
                            startedAt = newStartedAt,
                            endedAt = newEndedAt,
                            notes = draft.notes.trim(),
                            updatedAt = now
                        ),
                    exercises = updatedExercises
                )

            val affectedExerciseIds =
                (
                    original.exercises.map { it.workoutExercise.exerciseId } +
                        updatedWorkout.exercises.map { it.workoutExercise.exerciseId }
                    ).toSet()

            workoutRepository.saveWorkoutWithExercises(updatedWorkout)
            if (affectedExerciseIds.isNotEmpty()) {
                personalRecordRepository.recalculateRecords(affectedExerciseIds)
            }
            overlay.update { it.copy(editDraft = null, isSavingEdit = false) }
        }
    }

    fun onRequestDelete() {
        overlay.update { it.copy(showDeleteConfirm = true) }
    }

    fun onDismissDelete() {
        overlay.update { it.copy(showDeleteConfirm = false) }
    }

    fun onConfirmDelete() {
        val current = latestWorkout ?: return
        val affectedExerciseIds = current.exercises.map { it.workoutExercise.exerciseId }.toSet()
        overlay.update { it.copy(showDeleteConfirm = false) }
        viewModelScope.launch {
            workoutRepository.deleteWorkout(current.workout.id)
            if (affectedExerciseIds.isNotEmpty()) {
                personalRecordRepository.recalculateRecords(affectedExerciseIds)
            }
            overlay.update { it.copy(isDeleted = true) }
        }
    }

    fun onRepeatWorkoutClick() {
        if (activeWorkoutManager.hasActiveWorkout()) {
            overlay.update { it.copy(showRepeatConflictDialog = true) }
            return
        }
        viewModelScope.launch {
            activeWorkoutManager.startFromCompletedWorkout(workoutId)
            overlay.update { it.copy(shouldOpenActiveWorkout = true) }
        }
    }

    fun onDismissRepeatConflict() {
        overlay.update { it.copy(showRepeatConflictDialog = false) }
    }

    fun onResumeActiveWorkout() {
        overlay.update { it.copy(showRepeatConflictDialog = false, shouldOpenActiveWorkout = true) }
    }

    fun onDiscardActiveAndRepeat() {
        overlay.update { it.copy(showRepeatConflictDialog = false) }
        viewModelScope.launch {
            activeWorkoutManager.startFromCompletedWorkout(workoutId)
            overlay.update { it.copy(shouldOpenActiveWorkout = true) }
        }
    }

    fun onNavigatedToActiveWorkout() {
        overlay.update { it.copy(shouldOpenActiveWorkout = false) }
    }

    fun onOpenSaveRoutineDialog() {
        val name = latestWorkout?.workout?.name.orEmpty()
        overlay.update { it.copy(showSaveRoutineDialog = true, saveRoutineNameDraft = name) }
    }

    fun onDismissSaveRoutineDialog() {
        overlay.update { it.copy(showSaveRoutineDialog = false) }
    }

    fun onSaveRoutineNameChange(name: String) {
        overlay.update { it.copy(saveRoutineNameDraft = name) }
    }

    fun onConfirmSaveRoutine() {
        val source = latestWorkout ?: return
        val routineName = overlay.value.saveRoutineNameDraft.trim().ifEmpty { source.workout.name }
        overlay.update { it.copy(showSaveRoutineDialog = false) }
        viewModelScope.launch {
            val routine =
                createRoutineFromWorkout(
                    source = source,
                    routineName = routineName,
                    nowMillis = clock.millis()
                )
            routineRepository.saveRoutineWithExercises(routine)
            overlay.update { it.copy(savedRoutineName = routine.routine.name) }
        }
    }

    fun onConsumeSavedRoutineMessage() {
        overlay.update { it.copy(savedRoutineName = null) }
    }

    private fun updateDraft(transform: (EditSessionDraft) -> EditSessionDraft) {
        overlay.update { ov ->
            val current = ov.editDraft ?: return@update ov
            ov.copy(editDraft = transform(current))
        }
    }

    private fun updateSet(workoutExerciseId: String, setId: String, transform: (EditSetDraft) -> EditSetDraft) {
        updateDraft { draft ->
            draft.copy(
                exercises =
                    draft.exercises.map { ex ->
                        if (ex.workoutExerciseId != workoutExerciseId) {
                            ex
                        } else {
                            ex.copy(sets = ex.sets.map { s -> if (s.id == setId) transform(s) else s })
                        }
                    }
            )
        }
    }

    private fun buildUiState(
        workout: WorkoutWithExercises,
        records: List<PersonalRecord>,
        exercisesById: Map<String, Exercise>,
        settings: Settings,
        latestBodyWeightKg: Double?,
        isSpanish: Boolean,
        activeWorkoutName: String?,
        overlay: SessionDetailOverlay
    ): SessionDetailUiState {
        val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
        val trackingTypes = exercisesById.mapValues { it.value.trackingType }
        val volumeKg = computeWorkoutVolumeKg(workout, trackingTypes, latestBodyWeightKg)
        val volumeText = Units.formatWeight(volumeKg, settings.weightUnit, locale = locale, includeUnit = true)
        val completedSetsCount = computeCompletedSetsCount(workout)
        val durationMillis =
            workout.workout.endedAt?.let { ended ->
                (ended - workout.workout.startedAt).coerceAtLeast(0L)
            }
        val recordsBySetId = Records.groupRecordTypesBySetId(records)

        val exerciseItems =
            workout.exercises
                .sortedBy { it.workoutExercise.position }
                .map { exWithSets ->
                    val exercise = exercisesById[exWithSets.workoutExercise.exerciseId]
                    val sortedSets = exWithSets.sets.sortedBy { it.position }
                    val normalNumbers = computeNormalSetNumbers(sortedSets.map { it.setType })
                    SessionDetailExerciseUi(
                        id = exWithSets.workoutExercise.id,
                        exerciseId = exWithSets.workoutExercise.exerciseId,
                        name = exercise?.localizedName(isSpanish) ?: exWithSets.workoutExercise.exerciseId,
                        trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS,
                        supersetGroup = exWithSets.workoutExercise.supersetGroup,
                        notes = exWithSets.workoutExercise.notes,
                        sets =
                            sortedSets.mapIndexed { idx, set ->
                                SessionDetailSetUi(
                                    id = set.id,
                                    setType = set.setType,
                                    normalSetNumber = normalNumbers.getOrElse(idx) { idx + 1 },
                                    weightKg = set.weightKg,
                                    reps = set.reps,
                                    rpe = set.rpe,
                                    durationS = set.durationS,
                                    distanceM = set.distanceM,
                                    completed = set.completed,
                                    recordTypes = recordsBySetId[set.id].orEmpty()
                                )
                            }
                    )
                }

        return SessionDetailUiState(
            workoutId = workout.workout.id,
            isLoaded = true,
            workoutFound = true,
            name = workout.workout.name,
            dateTimeText = formatSessionDateTime(workout.workout.startedAt, isSpanish, zoneId),
            durationText = durationMillis?.let { formatHistoryDuration(it) },
            volumeText = volumeText,
            completedSetsCount = completedSetsCount,
            totalRecordsCount = records.size,
            notes = workout.workout.notes,
            weightUnit = settings.weightUnit,
            isSpanish = isSpanish,
            exercises = exerciseItems,
            editDraft = overlay.editDraft,
            isSavingEdit = overlay.isSavingEdit,
            showDeleteConfirm = overlay.showDeleteConfirm,
            isDeleted = overlay.isDeleted,
            showSaveRoutineDialog = overlay.showSaveRoutineDialog,
            saveRoutineNameDraft = overlay.saveRoutineNameDraft,
            savedRoutineName = overlay.savedRoutineName,
            showRepeatConflictDialog = overlay.showRepeatConflictDialog,
            activeWorkoutName = activeWorkoutName,
            shouldOpenActiveWorkout = overlay.shouldOpenActiveWorkout
        )
    }

    private fun buildEditDraft(
        workout: WorkoutWithExercises,
        exercisesById: Map<String, Exercise>,
        weightUnit: WeightUnit,
        isSpanish: Boolean
    ): EditSessionDraft {
        val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
        val durationMinutes =
            workout.workout.endedAt?.let { ended ->
                ((ended - workout.workout.startedAt).coerceAtLeast(0L) / 60_000L).toString()
            }.orEmpty()
        return EditSessionDraft(
            name = workout.workout.name,
            startedAtMillis = workout.workout.startedAt,
            durationMinutesText = durationMinutes,
            notes = workout.workout.notes,
            exercises =
                workout.exercises
                    .sortedBy { it.workoutExercise.position }
                    .map { exWithSets ->
                        val exercise = exercisesById[exWithSets.workoutExercise.exerciseId]
                        EditExerciseDraft(
                            workoutExerciseId = exWithSets.workoutExercise.id,
                            exerciseId = exWithSets.workoutExercise.exerciseId,
                            name = exercise?.localizedName(isSpanish) ?: exWithSets.workoutExercise.exerciseId,
                            trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS,
                            supersetGroup = exWithSets.workoutExercise.supersetGroup,
                            restSeconds = exWithSets.workoutExercise.restSeconds,
                            notes = exWithSets.workoutExercise.notes,
                            sets =
                                exWithSets.sets
                                    .sortedBy { it.position }
                                    .map { set ->
                                        EditSetDraft(
                                            id = set.id,
                                            setType = set.setType,
                                            weightText =
                                                set.weightKg?.let {
                                                    Units.formatWeight(
                                                        it,
                                                        weightUnit,
                                                        locale = locale,
                                                        includeUnit = false
                                                    )
                                                }.orEmpty(),
                                            repsText = set.reps?.toString().orEmpty(),
                                            rpeText = set.rpe?.let { formatRpeText(it) }.orEmpty(),
                                            durationText = set.durationS?.toString().orEmpty(),
                                            distanceText = set.distanceM?.let { formatPlainNumber(it) }.orEmpty(),
                                            completed = set.completed
                                        )
                                    }
                        )
                    }
        )
    }
}

private data class LoadedSessionBundle(
    val workout: WorkoutWithExercises?,
    val records: List<PersonalRecord>,
    val exercises: List<Exercise>,
    val settings: Settings,
    val latestBodyWeightKg: Double?
)

private fun formatRpeText(rpe: Double): String = if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

private fun formatPlainNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
