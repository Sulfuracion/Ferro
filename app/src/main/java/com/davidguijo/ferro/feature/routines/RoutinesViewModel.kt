package com.davidguijo.ferro.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineTemplate
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.moveRoutineInFolder
import com.davidguijo.ferro.core.domain.moveRoutineToFolder
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import com.davidguijo.ferro.core.domain.toRoutineWithExercises
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resumen de una rutina para mostrar en la lista agrupada por carpetas. */
data class RoutineSummaryUi(
    val routineWithExercises: RoutineWithExercises,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val exerciseNamesSummary: String
)

/** Sección de carpeta plegable en la pantalla de Rutinas. */
data class RoutineFolderSectionUi(
    val folder: RoutineFolder,
    val isExpanded: Boolean,
    val routines: List<RoutineSummaryUi>
)

/** Estado del diálogo/hoja para crear o renombrar una carpeta. */
sealed interface FolderEditorDialogState {
    data object Create : FolderEditorDialogState

    data class Rename(val folder: RoutineFolder) : FolderEditorDialogState
}

data class RoutinesUiState(
    val folderSections: List<RoutineFolderSectionUi> = emptyList(),
    val uncategorizedRoutines: List<RoutineSummaryUi> = emptyList(),
    val isUncategorizedExpanded: Boolean = true,
    val exercisesById: Map<String, Exercise> = emptyMap(),
    val templatePrograms: List<RoutineTemplateProgram> = emptyList(),
    val isSpanish: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = 90,
    val folderEditorDialog: FolderEditorDialogState? = null,
    val folderPendingDelete: RoutineFolder? = null,
    val routinePendingDelete: RoutineWithExercises? = null,
    val routinePendingMove: RoutineWithExercises? = null,
    val previewRoutine: RoutineWithExercises? = null,
    val previewTemplateProgram: RoutineTemplateProgram? = null
) {
    val hasAnyRoutinesOrFolders: Boolean
        get() = folderSections.isNotEmpty() || uncategorizedRoutines.isNotEmpty()
}

@HiltViewModel
class RoutinesViewModel
internal constructor(
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        routineRepository: RoutineRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        clock: Clock
    ) : this(
        routineRepository = routineRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val collapsedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    private val isUncategorizedCollapsed = MutableStateFlow(false)
    private val templatePrograms = MutableStateFlow<List<RoutineTemplateProgram>>(emptyList())
    private val overlayState = MutableStateFlow(OverlayState())

    init {
        viewModelScope.launch {
            templatePrograms.value =
                runCatching { routineRepository.getTemplatePrograms() }
                    .getOrDefault(emptyList())
        }
    }

    private val dataFlow =
        combine(
            routineRepository.observeFolders(),
            routineRepository.observeAllRoutinesWithExercises(),
            exerciseRepository.observeAllExercises(),
            settingsRepository.observeSettings()
        ) { folders, routines, exercises, settings ->
            LoadedData(
                folders = folders,
                routines = routines,
                exercisesById = exercises.associateBy { it.id },
                settings = settings
            )
        }

    val uiState: StateFlow<RoutinesUiState> =
        combine(
            dataFlow,
            collapsedFolderIds,
            isUncategorizedCollapsed,
            templatePrograms,
            overlayState
        ) { data, collapsedIds, uncategorizedCollapsed, templates, overlay ->
            val isSpanish = resolveIsSpanish(data.settings.language, systemLanguageProvider())
            val folderIds = data.folders.map { it.id }.toSet()

            val folderSections =
                data.folders
                    .sortedWith(compareBy<RoutineFolder> { it.position }.thenBy { it.name.lowercase() })
                    .map { folder ->
                        val inFolder =
                            data.routines
                                .filter { it.routine.folderId == folder.id }
                                .sortedWith(
                                    compareBy<RoutineWithExercises> { it.routine.position }
                                        .thenBy { it.routine.name.lowercase() }
                                )
                        RoutineFolderSectionUi(
                            folder = folder,
                            isExpanded = folder.id !in collapsedIds,
                            routines = buildSummaries(inFolder, data.exercisesById, isSpanish)
                        )
                    }

            val uncategorized =
                data.routines
                    .filter { it.routine.folderId == null || it.routine.folderId !in folderIds }
                    .sortedWith(
                        compareBy<RoutineWithExercises> { it.routine.position }
                            .thenBy { it.routine.name.lowercase() }
                    )

            val routinesById = data.routines.associateBy { it.routine.id }
            val foldersById = data.folders.associateBy { it.id }
            val templatesById = templates.associateBy { it.id }

            RoutinesUiState(
                folderSections = folderSections,
                uncategorizedRoutines = buildSummaries(uncategorized, data.exercisesById, isSpanish),
                isUncategorizedExpanded = !uncategorizedCollapsed,
                exercisesById = data.exercisesById,
                templatePrograms = templates,
                isSpanish = isSpanish,
                weightUnit = data.settings.weightUnit,
                defaultRestSeconds = data.settings.defaultRestSeconds,
                folderEditorDialog = overlay.folderEditorDialog,
                folderPendingDelete = overlay.folderPendingDeleteId?.let { foldersById[it] },
                routinePendingDelete = overlay.routinePendingDeleteId?.let { routinesById[it] },
                routinePendingMove = overlay.routinePendingMoveId?.let { routinesById[it] },
                previewRoutine = overlay.previewRoutineId?.let { routinesById[it] },
                previewTemplateProgram = overlay.previewTemplateProgramId?.let { templatesById[it] }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RoutinesUiState()
        )

    fun onToggleFolderExpanded(folderId: String) {
        collapsedFolderIds.update { current ->
            if (folderId in current) current - folderId else current + folderId
        }
    }

    fun onToggleUncategorizedExpanded() {
        isUncategorizedCollapsed.update { !it }
    }

    fun onOpenCreateFolder() {
        overlayState.update { it.copy(folderEditorDialog = FolderEditorDialogState.Create) }
    }

    fun onOpenRenameFolder(folder: RoutineFolder) {
        overlayState.update { it.copy(folderEditorDialog = FolderEditorDialogState.Rename(folder)) }
    }

    fun onDismissFolderEditor() {
        overlayState.update { it.copy(folderEditorDialog = null) }
    }

    fun onSaveFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val editorState = overlayState.value.folderEditorDialog ?: return
        viewModelScope.launch {
            val now = clock.millis()
            when (editorState) {
                FolderEditorDialogState.Create -> {
                    val currentFolders = uiState.value.folderSections.map { it.folder }
                    val nextPos = (currentFolders.maxOfOrNull { it.position } ?: -1) + 1
                    val folder =
                        RoutineFolder(
                            id = UUID.randomUUID().toString(),
                            name = trimmed,
                            position = nextPos,
                            createdAt = now,
                            updatedAt = now
                        )
                    routineRepository.upsertFolder(folder)
                }

                is FolderEditorDialogState.Rename -> {
                    val updated =
                        editorState.folder.copy(
                            name = trimmed,
                            updatedAt = now
                        )
                    routineRepository.upsertFolder(updated)
                }
            }
            overlayState.update { it.copy(folderEditorDialog = null) }
        }
    }

    fun onRequestDeleteFolder(folderId: String) {
        overlayState.update { it.copy(folderPendingDeleteId = folderId) }
    }

    fun onDismissDeleteFolder() {
        overlayState.update { it.copy(folderPendingDeleteId = null) }
    }

    fun onConfirmDeleteFolder() {
        val folderId = overlayState.value.folderPendingDeleteId ?: return
        viewModelScope.launch {
            routineRepository.deleteFolder(folderId)
            collapsedFolderIds.update { it - folderId }
            overlayState.update { it.copy(folderPendingDeleteId = null) }
        }
    }

    fun onMoveRoutine(routineId: String, direction: MoveDirection) {
        viewModelScope.launch {
            val state = uiState.value
            val allRoutines =
                state.folderSections.flatMap { section -> section.routines.map { it.routineWithExercises.routine } } +
                    state.uncategorizedRoutines.map { it.routineWithExercises.routine }
            val target = allRoutines.firstOrNull { it.id == routineId } ?: return@launch
            val sameGroup = allRoutines.filter { it.folderId == target.folderId }
            val reordered =
                moveRoutineInFolder(
                    routinesInFolder = sameGroup,
                    routineId = routineId,
                    direction = direction,
                    updatedAt = clock.millis()
                )
            routineRepository.upsertRoutines(reordered)
        }
    }

    fun onRequestMoveRoutineToFolder(routineId: String) {
        overlayState.update { it.copy(routinePendingMoveId = routineId) }
    }

    fun onDismissMoveRoutineToFolder() {
        overlayState.update { it.copy(routinePendingMoveId = null) }
    }

    fun onConfirmMoveRoutineToFolder(targetFolderId: String?) {
        val routineId = overlayState.value.routinePendingMoveId ?: return
        viewModelScope.launch {
            val state = uiState.value
            val allRoutines =
                state.folderSections.flatMap { section -> section.routines.map { it.routineWithExercises.routine } } +
                    state.uncategorizedRoutines.map { it.routineWithExercises.routine }
            val changed =
                moveRoutineToFolder(
                    allRoutines = allRoutines,
                    routineId = routineId,
                    targetFolderId = targetFolderId,
                    updatedAt = clock.millis()
                )
            if (changed.isNotEmpty()) {
                routineRepository.upsertRoutines(changed)
            }
            if (targetFolderId != null) {
                collapsedFolderIds.update { it - targetFolderId }
            } else {
                isUncategorizedCollapsed.value = false
            }
            overlayState.update { it.copy(routinePendingMoveId = null) }
        }
    }

    fun onDuplicateRoutine(routineId: String, newName: String) {
        viewModelScope.launch {
            routineRepository.duplicateRoutine(
                sourceRoutineId = routineId,
                newName = newName
            )
        }
    }

    fun onRequestDeleteRoutine(routineId: String) {
        overlayState.update { it.copy(routinePendingDeleteId = routineId) }
    }

    fun onDismissDeleteRoutine() {
        overlayState.update { it.copy(routinePendingDeleteId = null) }
    }

    fun onConfirmDeleteRoutine() {
        val routineId = overlayState.value.routinePendingDeleteId ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId)
            overlayState.update { current ->
                current.copy(
                    routinePendingDeleteId = null,
                    previewRoutineId = current.previewRoutineId?.takeUnless { it == routineId }
                )
            }
        }
    }

    fun onOpenRoutinePreview(routineId: String) {
        overlayState.update { it.copy(previewRoutineId = routineId) }
    }

    fun onDismissRoutinePreview() {
        overlayState.update { it.copy(previewRoutineId = null) }
    }

    fun onOpenTemplateProgram(programId: String) {
        overlayState.update { it.copy(previewTemplateProgramId = programId) }
    }

    fun onDismissTemplateProgram() {
        overlayState.update { it.copy(previewTemplateProgramId = null) }
    }

    fun onCopyTemplateProgram(program: RoutineTemplateProgram) {
        viewModelScope.launch {
            val state = uiState.value
            val isSpanish = state.isSpanish
            val now = clock.millis()
            val nextFolderPos = (state.folderSections.maxOfOrNull { it.folder.position } ?: -1) + 1
            val folder =
                RoutineFolder(
                    id = UUID.randomUUID().toString(),
                    name = program.localizedName(isSpanish),
                    position = nextFolderPos,
                    createdAt = now,
                    updatedAt = now
                )
            val routines =
                program.routines.mapIndexed { index, templateRoutine ->
                    templateRoutine.toRoutineWithExercises(
                        folderId = folder.id,
                        position = index,
                        isSpanish = isSpanish,
                        now = now
                    )
                }
            routineRepository.saveFolderWithRoutines(
                folder = folder,
                routinesWithExercises = routines
            )
            overlayState.update { it.copy(previewTemplateProgramId = null) }
        }
    }

    fun onCopyTemplateRoutine(template: RoutineTemplate) {
        viewModelScope.launch {
            val state = uiState.value
            val isSpanish = state.isSpanish
            val now = clock.millis()
            val nextPos =
                (state.uncategorizedRoutines.maxOfOrNull { it.routineWithExercises.routine.position } ?: -1) + 1
            val routineWithExercises =
                template.toRoutineWithExercises(
                    folderId = null,
                    position = nextPos,
                    isSpanish = isSpanish,
                    now = now
                )
            routineRepository.saveRoutineWithExercises(routineWithExercises)
            isUncategorizedCollapsed.value = false
            overlayState.update { it.copy(previewTemplateProgramId = null) }
        }
    }

    private fun buildSummaries(
        routines: List<RoutineWithExercises>,
        exercisesById: Map<String, Exercise>,
        isSpanish: Boolean
    ): List<RoutineSummaryUi> = routines.mapIndexed { index, rwe ->
        val totalSets = rwe.exercises.sumOf { it.sets.size }
        val namesSummary =
            rwe.exercises
                .mapNotNull { ex -> exercisesById[ex.routineExercise.exerciseId]?.localizedName(isSpanish) }
                .joinToString(" · ")
        RoutineSummaryUi(
            routineWithExercises = rwe,
            canMoveUp = index > 0,
            canMoveDown = index < routines.lastIndex,
            exerciseCount = rwe.exercises.size,
            totalSetCount = totalSets,
            exerciseNamesSummary = namesSummary
        )
    }

    private data class LoadedData(
        val folders: List<RoutineFolder>,
        val routines: List<RoutineWithExercises>,
        val exercisesById: Map<String, Exercise>,
        val settings: com.davidguijo.ferro.core.domain.model.Settings
    )

    private data class OverlayState(
        val folderEditorDialog: FolderEditorDialogState? = null,
        val folderPendingDeleteId: String? = null,
        val routinePendingDeleteId: String? = null,
        val routinePendingMoveId: String? = null,
        val previewRoutineId: String? = null,
        val previewTemplateProgramId: String? = null
    )
}
