package com.davidguijo.ferro.feature.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.canGroupAsSuperset
import com.davidguijo.ferro.core.domain.groupConsecutiveAsSuperset
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.moveSupersetItem
import com.davidguijo.ferro.core.domain.normalizeSupersetGroups
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import com.davidguijo.ferro.core.domain.ungroupSupersetGroup
import com.davidguijo.ferro.feature.exercises.ExerciseLibraryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Borrador editable de una serie objetivo en el editor de rutina. */
data class RoutineSetDraft(
    val id: String,
    val setType: SetType = SetType.NORMAL,
    val weightInput: String = "",
    val repsMinInput: String = "",
    val repsMaxInput: String = "",
    val durationSecondsInput: String = "",
    val distanceMetersInput: String = ""
)

/** Borrador editable de un ejercicio dentro del editor de rutina. */
data class RoutineExerciseDraft(
    val id: String,
    val exerciseId: String,
    val position: Int,
    val supersetGroup: String? = null,
    val restSeconds: Int? = null,
    val notes: String = "",
    val sets: List<RoutineSetDraft> = emptyList()
)

/** Selector modal activo para cambiar el tipo de una serie objetivo. */
data class ActiveSetTypePicker(val routineExerciseId: String, val setId: String, val currentType: SetType)

data class RoutineEditorUiState(
    val routineId: String? = null,
    val isEditingExisting: Boolean = false,
    val isLoaded: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val folderId: String? = null,
    val folders: List<RoutineFolder> = emptyList(),
    val exercises: List<RoutineExerciseDraft> = emptyList(),
    val exercisesById: Map<String, Exercise> = emptyMap(),
    val selectedSupersetExerciseIds: Set<String> = emptySet(),
    val canGroupSelectedAsSuperset: Boolean = false,
    val isSpanish: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = 90,
    val showNameError: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val activeSetTypePicker: ActiveSetTypePicker? = null,
    val showDiscardChangesDialog: Boolean = false,
    val showDeleteRoutineDialog: Boolean = false,
    val isFinished: Boolean = false
)

@HiltViewModel
class RoutineEditorViewModel
internal constructor(
    private val initialRoutineId: String?,
    private val savedStateHandle: SavedStateHandle?,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        routineRepository: RoutineRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        clock: Clock
    ) : this(
        initialRoutineId = savedStateHandle.toRoute<RoutineEditorRoute>().routineId,
        savedStateHandle = savedStateHandle,
        routineRepository = routineRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val draftState =
        MutableStateFlow(
            EditorDraftState(routineId = initialRoutineId ?: UUID.randomUUID().toString())
        )
    private val initialSnapshot = MutableStateFlow<DraftSnapshot?>(null)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.observeSettings().first()
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US

            if (initialRoutineId != null) {
                val existing = routineRepository.getRoutineWithExercises(initialRoutineId)
                if (existing != null) {
                    val draftExercises =
                        existing.exercises
                            .sortedBy { it.routineExercise.position }
                            .mapIndexed { index, exWithSets ->
                                RoutineExerciseDraft(
                                    id = exWithSets.routineExercise.id,
                                    exerciseId = exWithSets.routineExercise.exerciseId,
                                    position = index,
                                    supersetGroup = exWithSets.routineExercise.supersetGroup,
                                    restSeconds = exWithSets.routineExercise.restSeconds,
                                    notes = exWithSets.routineExercise.notes,
                                    sets =
                                        exWithSets.sets
                                            .sortedBy { it.position }
                                            .map { set ->
                                                RoutineSetDraft(
                                                    id = set.id,
                                                    setType = set.setType,
                                                    weightInput =
                                                        set.targetWeightKg?.let { kg ->
                                                            Units.formatWeight(
                                                                weightKg = kg,
                                                                unit = settings.weightUnit,
                                                                locale = locale,
                                                                includeUnit = false
                                                            )
                                                        } ?: "",
                                                    repsMinInput = set.targetRepsMin?.toString() ?: "",
                                                    repsMaxInput = set.targetRepsMax?.toString() ?: "",
                                                    durationSecondsInput = set.targetDurationS?.toString() ?: "",
                                                    distanceMetersInput =
                                                        set.targetDistanceM?.let { meters ->
                                                            formatDistanceMeters(meters, isSpanish)
                                                        } ?: ""
                                                )
                                            }
                                )
                            }
                    val normalized = normalizeDraftExercises(draftExercises)
                    val loadedDraft =
                        EditorDraftState(
                            routineId = existing.routine.id,
                            isEditingExisting = true,
                            isLoaded = true,
                            createdAt = existing.routine.createdAt,
                            position = existing.routine.position,
                            name = existing.routine.name,
                            notes = existing.routine.notes,
                            folderId = existing.routine.folderId,
                            exercises = normalized
                        )
                    initialSnapshot.value = loadedDraft.toSnapshot()
                    draftState.value = loadedDraft
                } else {
                    val blankDraft =
                        EditorDraftState(
                            routineId = initialRoutineId,
                            isEditingExisting = false,
                            isLoaded = true
                        )
                    initialSnapshot.value = blankDraft.toSnapshot()
                    draftState.value = blankDraft
                }
            } else {
                val blankDraft =
                    EditorDraftState(
                        routineId = draftState.value.routineId,
                        isEditingExisting = false,
                        isLoaded = true
                    )
                initialSnapshot.value = blankDraft.toSnapshot()
                draftState.value = blankDraft
            }
        }

        if (savedStateHandle != null) {
            viewModelScope.launch {
                savedStateHandle
                    .getStateFlow<ArrayList<String>?>(ExerciseLibraryResult.SELECTED_EXERCISE_IDS_KEY, null)
                    .filterNotNull()
                    .collect { selectedIds ->
                        if (selectedIds.isNotEmpty()) {
                            onAddExercises(selectedIds)
                        }
                        savedStateHandle[ExerciseLibraryResult.SELECTED_EXERCISE_IDS_KEY] = null
                    }
            }
        }
    }

    val uiState: StateFlow<RoutineEditorUiState> =
        combine(
            draftState,
            initialSnapshot,
            routineRepository.observeFolders(),
            exerciseRepository.observeAllExercises(),
            settingsRepository.observeSettings()
        ) { draft, baseline, folders, allExercises, settings ->
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            val canGroup =
                canGroupAsSuperset(
                    itemIds = draft.exercises.map { it.id },
                    selectedIds = draft.selectedSupersetExerciseIds
                )
            val hasChanges = baseline != null && draft.toSnapshot() != baseline

            RoutineEditorUiState(
                routineId = draft.routineId,
                isEditingExisting = draft.isEditingExisting,
                isLoaded = draft.isLoaded,
                name = draft.name,
                notes = draft.notes,
                folderId = draft.folderId,
                folders = folders,
                exercises = draft.exercises,
                exercisesById = allExercises.associateBy { it.id },
                selectedSupersetExerciseIds = draft.selectedSupersetExerciseIds,
                canGroupSelectedAsSuperset = canGroup,
                isSpanish = isSpanish,
                weightUnit = settings.weightUnit,
                defaultRestSeconds = settings.defaultRestSeconds,
                showNameError = draft.showNameError,
                hasUnsavedChanges = hasChanges,
                activeSetTypePicker = draft.activeSetTypePicker,
                showDiscardChangesDialog = draft.showDiscardChangesDialog,
                showDeleteRoutineDialog = draft.showDeleteRoutineDialog,
                isFinished = draft.isFinished
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RoutineEditorUiState(routineId = initialRoutineId)
        )

    fun onNameChanged(name: String) {
        draftState.update { current ->
            current.copy(
                name = name,
                showNameError = if (name.isNotBlank()) false else current.showNameError
            )
        }
    }

    fun onNotesChanged(notes: String) {
        draftState.update { it.copy(notes = notes) }
    }

    fun onFolderSelected(folderId: String?) {
        draftState.update { it.copy(folderId = folderId) }
    }

    fun onAddExercises(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        draftState.update { current ->
            val startPos = current.exercises.size
            val added =
                exerciseIds.mapIndexed { idx, exerciseId ->
                    RoutineExerciseDraft(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        position = startPos + idx,
                        supersetGroup = null,
                        restSeconds = null,
                        notes = "",
                        sets =
                            listOf(
                                RoutineSetDraft(
                                    id = UUID.randomUUID().toString(),
                                    setType = SetType.NORMAL,
                                    repsMinInput = "8",
                                    repsMaxInput = "10",
                                    durationSecondsInput = "60",
                                    distanceMetersInput = ""
                                )
                            )
                    )
                }
            current.copy(exercises = normalizeDraftExercises(current.exercises + added))
        }
    }

    fun onMoveExercise(routineExerciseId: String, direction: MoveDirection) {
        draftState.update { current ->
            val fromIndex = current.exercises.indexOfFirst { it.id == routineExerciseId }
            if (fromIndex < 0) return@update current
            val toIndex =
                when (direction) {
                    MoveDirection.UP -> fromIndex - 1
                    MoveDirection.DOWN -> fromIndex + 1
                }
            if (toIndex !in current.exercises.indices) return@update current
            val reordered =
                moveSupersetItem(
                    items = current.exercises,
                    fromIndex = fromIndex,
                    toIndex = toIndex,
                    getSupersetGroup = { it.supersetGroup },
                    updateItem = { item, pos, group ->
                        item.copy(position = pos, supersetGroup = group)
                    }
                )
            current.copy(exercises = reordered)
        }
    }

    fun onRemoveExercise(routineExerciseId: String) {
        draftState.update { current ->
            val filtered = current.exercises.filterNot { it.id == routineExerciseId }
            current.copy(
                exercises = normalizeDraftExercises(filtered),
                selectedSupersetExerciseIds = current.selectedSupersetExerciseIds - routineExerciseId
            )
        }
    }

    fun onExerciseNotesChanged(routineExerciseId: String, notes: String) {
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) ex.copy(notes = notes) else ex
                    }
            )
        }
    }

    fun onAdjustExerciseRest(routineExerciseId: String, deltaSeconds: Int) {
        val defaultRest = uiState.value.defaultRestSeconds
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) {
                            val base = ex.restSeconds ?: defaultRest
                            val next = (base + deltaSeconds).coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)
                            ex.copy(restSeconds = next)
                        } else {
                            ex
                        }
                    }
            )
        }
    }

    fun onResetExerciseRestToDefault(routineExerciseId: String) {
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) ex.copy(restSeconds = null) else ex
                    }
            )
        }
    }

    fun onToggleSupersetSelection(routineExerciseId: String) {
        draftState.update { current ->
            val updated =
                if (routineExerciseId in current.selectedSupersetExerciseIds) {
                    current.selectedSupersetExerciseIds - routineExerciseId
                } else {
                    current.selectedSupersetExerciseIds + routineExerciseId
                }
            current.copy(selectedSupersetExerciseIds = updated)
        }
    }

    fun onClearSupersetSelection() {
        draftState.update { it.copy(selectedSupersetExerciseIds = emptySet()) }
    }

    fun onGroupSelectedAsSuperset() {
        draftState.update { current ->
            val grouped =
                groupConsecutiveAsSuperset(
                    items = current.exercises,
                    selectedIds = current.selectedSupersetExerciseIds,
                    getId = { it.id },
                    getSupersetGroup = { it.supersetGroup },
                    updateItem = { item, pos, group ->
                        item.copy(position = pos, supersetGroup = group)
                    }
                )
            current.copy(
                exercises = grouped,
                selectedSupersetExerciseIds = emptySet()
            )
        }
    }

    fun onUngroupSuperset(supersetGroup: String) {
        draftState.update { current ->
            val ungrouped =
                ungroupSupersetGroup(
                    items = current.exercises,
                    supersetGroup = supersetGroup,
                    getSupersetGroup = { it.supersetGroup },
                    updateItem = { item, pos, group ->
                        item.copy(position = pos, supersetGroup = group)
                    }
                )
            current.copy(exercises = ungrouped)
        }
    }

    fun onAddSet(routineExerciseId: String) {
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) {
                            val lastSet = ex.sets.lastOrNull()
                            val newSet =
                                RoutineSetDraft(
                                    id = UUID.randomUUID().toString(),
                                    setType = SetType.NORMAL,
                                    weightInput = lastSet?.weightInput ?: "",
                                    repsMinInput = lastSet?.repsMinInput ?: "8",
                                    repsMaxInput = lastSet?.repsMaxInput ?: "10",
                                    durationSecondsInput = lastSet?.durationSecondsInput ?: "60",
                                    distanceMetersInput = lastSet?.distanceMetersInput ?: ""
                                )
                            ex.copy(sets = ex.sets + newSet)
                        } else {
                            ex
                        }
                    }
            )
        }
    }

    fun onRemoveSet(routineExerciseId: String, setId: String) {
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) {
                            ex.copy(sets = ex.sets.filterNot { it.id == setId })
                        } else {
                            ex
                        }
                    }
            )
        }
    }

    fun onOpenSetTypePicker(routineExerciseId: String, setId: String, currentType: SetType) {
        draftState.update {
            it.copy(
                activeSetTypePicker =
                    ActiveSetTypePicker(
                        routineExerciseId = routineExerciseId,
                        setId = setId,
                        currentType = currentType
                    )
            )
        }
    }

    fun onDismissSetTypePicker() {
        draftState.update { it.copy(activeSetTypePicker = null) }
    }

    fun onSelectSetType(setType: SetType) {
        val picker = draftState.value.activeSetTypePicker ?: return
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == picker.routineExerciseId) {
                            ex.copy(
                                sets =
                                    ex.sets.map { set ->
                                        if (set.id == picker.setId) set.copy(setType = setType) else set
                                    }
                            )
                        } else {
                            ex
                        }
                    },
                activeSetTypePicker = null
            )
        }
    }

    fun onSetWeightChanged(routineExerciseId: String, setId: String, value: String) {
        updateSetDraft(routineExerciseId, setId) { it.copy(weightInput = value) }
    }

    fun onSetRepsMinChanged(routineExerciseId: String, setId: String, value: String) {
        updateSetDraft(routineExerciseId, setId) { it.copy(repsMinInput = value) }
    }

    fun onSetRepsMaxChanged(routineExerciseId: String, setId: String, value: String) {
        updateSetDraft(routineExerciseId, setId) { it.copy(repsMaxInput = value) }
    }

    fun onSetDurationChanged(routineExerciseId: String, setId: String, value: String) {
        updateSetDraft(routineExerciseId, setId) { it.copy(durationSecondsInput = value) }
    }

    fun onSetDistanceChanged(routineExerciseId: String, setId: String, value: String) {
        updateSetDraft(routineExerciseId, setId) { it.copy(distanceMetersInput = value) }
    }

    fun onRequestBack() {
        if (uiState.value.hasUnsavedChanges) {
            draftState.update { it.copy(showDiscardChangesDialog = true) }
        } else {
            draftState.update { it.copy(isFinished = true) }
        }
    }

    fun onDismissDiscardChangesDialog() {
        draftState.update { it.copy(showDiscardChangesDialog = false) }
    }

    fun onConfirmDiscardChanges() {
        draftState.update {
            it.copy(
                showDiscardChangesDialog = false,
                isFinished = true
            )
        }
    }

    fun onRequestDeleteRoutine() {
        if (!draftState.value.isEditingExisting) return
        draftState.update { it.copy(showDeleteRoutineDialog = true) }
    }

    fun onDismissDeleteRoutineDialog() {
        draftState.update { it.copy(showDeleteRoutineDialog = false) }
    }

    fun onConfirmDeleteRoutine() {
        val current = draftState.value
        if (!current.isEditingExisting) return
        viewModelScope.launch {
            routineRepository.deleteRoutine(current.routineId)
            draftState.update {
                it.copy(
                    showDeleteRoutineDialog = false,
                    isFinished = true
                )
            }
        }
    }

    fun onDuplicateCurrentRoutine(copiedName: String) {
        val current = draftState.value
        if (!current.isEditingExisting) return
        viewModelScope.launch {
            routineRepository.duplicateRoutine(
                sourceRoutineId = current.routineId,
                newName = copiedName
            )
        }
    }

    fun onSaveRoutine() {
        val current = draftState.value
        val trimmedName = current.name.trim()
        if (trimmedName.isEmpty()) {
            draftState.update { it.copy(showNameError = true) }
            return
        }

        viewModelScope.launch {
            val state = uiState.value
            val now = clock.millis()
            val createdAt = current.createdAt ?: now
            val position =
                current.position ?: run {
                    val routinesInTargetFolder =
                        routineRepository.observeRoutinesInFolder(current.folderId).first()
                    (routinesInTargetFolder.maxOfOrNull { it.position } ?: -1) + 1
                }

            val routine =
                Routine(
                    id = current.routineId,
                    folderId = current.folderId,
                    name = trimmedName,
                    notes = current.notes.trim(),
                    position = position,
                    createdAt = createdAt,
                    updatedAt = now
                )

            val normalizedDrafts = normalizeDraftExercises(current.exercises)
            val domainExercises =
                normalizedDrafts.mapIndexed { exerciseIndex, exDraft ->
                    val trackingType =
                        state.exercisesById[exDraft.exerciseId]?.trackingType ?: TrackingType.WEIGHT_REPS
                    val domainSets =
                        exDraft.sets.mapIndexed { setIndex, setDraft ->
                            setDraft.toDomainSet(
                                routineExerciseId = exDraft.id,
                                position = setIndex,
                                trackingType = trackingType,
                                weightUnit = state.weightUnit,
                                now = now
                            )
                        }
                    RoutineExerciseWithSets(
                        routineExercise =
                            RoutineExercise(
                                id = exDraft.id,
                                routineId = routine.id,
                                exerciseId = exDraft.exerciseId,
                                position = exerciseIndex,
                                supersetGroup = exDraft.supersetGroup,
                                restSeconds = exDraft.restSeconds,
                                notes = exDraft.notes.trim(),
                                createdAt = now,
                                updatedAt = now
                            ),
                        sets = domainSets
                    )
                }

            routineRepository.saveRoutineWithExercises(
                RoutineWithExercises(
                    routine = routine,
                    exercises = domainExercises
                )
            )
            initialSnapshot.value = current.toSnapshot()
            draftState.update { it.copy(isFinished = true) }
        }
    }

    private fun updateSetDraft(
        routineExerciseId: String,
        setId: String,
        transform: (RoutineSetDraft) -> RoutineSetDraft
    ) {
        draftState.update { current ->
            current.copy(
                exercises =
                    current.exercises.map { ex ->
                        if (ex.id == routineExerciseId) {
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

    private fun normalizeDraftExercises(exercises: List<RoutineExerciseDraft>): List<RoutineExerciseDraft> =
        normalizeSupersetGroups(
            items = exercises,
            getSupersetGroup = { it.supersetGroup },
            updateItem = { item, pos, group ->
                item.copy(position = pos, supersetGroup = group)
            }
        )

    private fun RoutineSetDraft.toDomainSet(
        routineExerciseId: String,
        position: Int,
        trackingType: TrackingType,
        weightUnit: WeightUnit,
        now: Long
    ): RoutineSet {
        val parsedWeightKg = Units.parseWeightToKg(weightInput, weightUnit)
        val rawMin = repsMinInput.trim().toIntOrNull()?.takeIf { it > 0 }
        val rawMax = repsMaxInput.trim().toIntOrNull()?.takeIf { it > 0 }
        val (repsMin, repsMax) =
            when {
                rawMin != null && rawMax != null -> minOf(rawMin, rawMax) to maxOf(rawMin, rawMax)
                rawMin != null -> rawMin to rawMin
                rawMax != null -> rawMax to rawMax
                else -> null to null
            }
        val durationS = durationSecondsInput.trim().toIntOrNull()?.takeIf { it > 0 }
        val distanceM = Units.parseWeightInput(distanceMetersInput)?.takeIf { it > 0.0 }

        return when (trackingType) {
            TrackingType.WEIGHT_REPS,
            TrackingType.WEIGHTED_BODYWEIGHT,
            TrackingType.ASSISTED_BODYWEIGHT ->
                RoutineSet(
                    id = id,
                    routineExerciseId = routineExerciseId,
                    position = position,
                    setType = setType,
                    targetWeightKg = parsedWeightKg,
                    targetRepsMin = repsMin,
                    targetRepsMax = repsMax,
                    targetDurationS = null,
                    targetDistanceM = null,
                    createdAt = now,
                    updatedAt = now
                )

            TrackingType.BODYWEIGHT_REPS ->
                RoutineSet(
                    id = id,
                    routineExerciseId = routineExerciseId,
                    position = position,
                    setType = setType,
                    targetWeightKg = null,
                    targetRepsMin = repsMin,
                    targetRepsMax = repsMax,
                    targetDurationS = null,
                    targetDistanceM = null,
                    createdAt = now,
                    updatedAt = now
                )

            TrackingType.DURATION ->
                RoutineSet(
                    id = id,
                    routineExerciseId = routineExerciseId,
                    position = position,
                    setType = setType,
                    targetWeightKg = null,
                    targetRepsMin = null,
                    targetRepsMax = null,
                    targetDurationS = durationS,
                    targetDistanceM = null,
                    createdAt = now,
                    updatedAt = now
                )

            TrackingType.DISTANCE_DURATION ->
                RoutineSet(
                    id = id,
                    routineExerciseId = routineExerciseId,
                    position = position,
                    setType = setType,
                    targetWeightKg = null,
                    targetRepsMin = null,
                    targetRepsMax = null,
                    targetDurationS = durationS,
                    targetDistanceM = distanceM,
                    createdAt = now,
                    updatedAt = now
                )
        }
    }

    private data class EditorDraftState(
        val routineId: String,
        val isEditingExisting: Boolean = false,
        val isLoaded: Boolean = false,
        val createdAt: Long? = null,
        val position: Int? = null,
        val name: String = "",
        val notes: String = "",
        val folderId: String? = null,
        val exercises: List<RoutineExerciseDraft> = emptyList(),
        val selectedSupersetExerciseIds: Set<String> = emptySet(),
        val showNameError: Boolean = false,
        val activeSetTypePicker: ActiveSetTypePicker? = null,
        val showDiscardChangesDialog: Boolean = false,
        val showDeleteRoutineDialog: Boolean = false,
        val isFinished: Boolean = false
    ) {
        fun toSnapshot(): DraftSnapshot = DraftSnapshot(
            name = name.trim(),
            notes = notes.trim(),
            folderId = folderId,
            exercises =
                exercises.map { ex ->
                    ex.copy(notes = ex.notes.trim())
                }
        )
    }

    private data class DraftSnapshot(
        val name: String,
        val notes: String,
        val folderId: String?,
        val exercises: List<RoutineExerciseDraft>
    )

    private companion object {
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
    }
}
