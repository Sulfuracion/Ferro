package com.davidguijo.ferro.feature.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.ExerciseLetterGroup
import com.davidguijo.ferro.core.domain.filterExercises
import com.davidguijo.ferro.core.domain.groupExercisesByLetter
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseLibraryUiState(
    val selectionMode: ExerciseSelectionMode = ExerciseSelectionMode.NONE,
    val query: String = "",
    val selectedMuscle: Muscle? = null,
    val selectedEquipment: Equipment? = null,
    val showArchived: Boolean = false,
    val isSpanish: Boolean = true,
    val groups: List<ExerciseLetterGroup> = emptyList(),
    val totalAvailableCount: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val isCreateSheetVisible: Boolean = false
)

@HiltViewModel
class ExerciseLibraryViewModel
internal constructor(
    private val selectionMode: ExerciseSelectionMode,
    private val exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        clock: Clock
    ) : this(
        selectionMode = savedStateHandle.toRoute<ExerciseLibraryRoute>().selectionMode,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val filterState = MutableStateFlow(FilterParams())
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val createSheetVisible = MutableStateFlow(false)

    val uiState: StateFlow<ExerciseLibraryUiState> =
        combine(
            exerciseRepository.observeAllExercises(),
            settingsRepository.observeSettings(),
            filterState,
            selectedIds,
            createSheetVisible
        ) { allExercises, settings, filters, currentSelected, sheetVisible ->
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            val available =
                if (filters.showArchived) {
                    allExercises
                } else {
                    allExercises.filterNot { it.archived }
                }
            val filtered =
                filterExercises(
                    exercises = available,
                    query = filters.query,
                    muscle = filters.muscle,
                    equipment = filters.equipment,
                    isSpanish = isSpanish
                )
            val groups = groupExercisesByLetter(filtered, isSpanish = isSpanish)
            ExerciseLibraryUiState(
                selectionMode = selectionMode,
                query = filters.query,
                selectedMuscle = filters.muscle,
                selectedEquipment = filters.equipment,
                showArchived = filters.showArchived,
                isSpanish = isSpanish,
                groups = groups,
                totalAvailableCount = available.size,
                selectedIds = currentSelected,
                isCreateSheetVisible = sheetVisible
            )
        }.catch {
            emit(ExerciseLibraryUiState(selectionMode = selectionMode))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseLibraryUiState(selectionMode = selectionMode)
        )

    fun onQueryChanged(query: String) {
        filterState.update { it.copy(query = query) }
    }

    fun onMuscleSelected(muscle: Muscle?) {
        filterState.update { current ->
            val next = if (muscle != null && current.muscle == muscle) null else muscle
            current.copy(muscle = next)
        }
    }

    fun onEquipmentSelected(equipment: Equipment?) {
        filterState.update { current ->
            val next = if (equipment != null && current.equipment == equipment) null else equipment
            current.copy(equipment = next)
        }
    }

    fun onToggleShowArchived() {
        filterState.update { it.copy(showArchived = !it.showArchived) }
    }

    fun onToggleExerciseSelection(exerciseId: String) {
        if (selectionMode != ExerciseSelectionMode.MULTIPLE) return
        selectedIds.update { current ->
            if (exerciseId in current) current - exerciseId else current + exerciseId
        }
    }

    fun onOpenCreateSheet() {
        createSheetVisible.value = true
    }

    fun onDismissCreateSheet() {
        createSheetVisible.value = false
    }

    fun onCreateCustomExercise(draft: CustomExerciseDraft) {
        viewModelScope.launch {
            val now = clock.millis()
            val exercise =
                Exercise(
                    id = UUID.randomUUID().toString(),
                    nameEs = draft.name,
                    nameEn = draft.name,
                    primaryMuscle = draft.primaryMuscle,
                    secondaryMuscles = draft.secondaryMuscles,
                    equipment = draft.equipment,
                    trackingType = draft.trackingType,
                    instructionsEs = draft.instructions,
                    instructionsEn = draft.instructions,
                    isCustom = true,
                    archived = false,
                    createdAt = now,
                    updatedAt = now
                )
            runCatching {
                exerciseRepository.upsertExercise(exercise)
            }.onSuccess {
                createSheetVisible.value = false
            }
        }
    }

    private data class FilterParams(
        val query: String = "",
        val muscle: Muscle? = null,
        val equipment: Equipment? = null,
        val showArchived: Boolean = false
    )
}
