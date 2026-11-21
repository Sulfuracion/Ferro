package com.davidguijo.ferro.feature.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isSpanish: Boolean = true,
    val isLoaded: Boolean = false,
    val isEditSheetVisible: Boolean = false,
    val isArchiveDialogVisible: Boolean = false
)

@HiltViewModel
class ExerciseDetailViewModel
internal constructor(
    private val exerciseId: String,
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
        exerciseId = savedStateHandle.toRoute<ExerciseDetailRoute>().exerciseId,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val editSheetVisible = MutableStateFlow(false)
    private val archiveDialogVisible = MutableStateFlow(false)

    val uiState: StateFlow<ExerciseDetailUiState> =
        combine(
            exerciseRepository.observeExerciseById(exerciseId),
            settingsRepository.observeSettings(),
            editSheetVisible,
            archiveDialogVisible
        ) { exercise, settings, editVisible, archiveVisible ->
            ExerciseDetailUiState(
                exercise = exercise,
                isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider()),
                isLoaded = true,
                isEditSheetVisible = editVisible,
                isArchiveDialogVisible = archiveVisible
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseDetailUiState()
        )

    fun onOpenEditSheet() {
        if (uiState.value.exercise?.isCustom == true) {
            editSheetVisible.value = true
        }
    }

    fun onDismissEditSheet() {
        editSheetVisible.value = false
    }

    fun onUpdateCustomExercise(draft: CustomExerciseDraft) {
        val current = uiState.value.exercise ?: return
        if (!current.isCustom) return
        val isSpanish = uiState.value.isSpanish
        val sameNames = current.nameEs == current.nameEn
        val sameInstructions = current.instructionsEs == current.instructionsEn

        viewModelScope.launch {
            val updated =
                current.copy(
                    nameEs = if (isSpanish || sameNames) draft.name else current.nameEs,
                    nameEn = if (!isSpanish || sameNames) draft.name else current.nameEn,
                    primaryMuscle = draft.primaryMuscle,
                    secondaryMuscles = draft.secondaryMuscles,
                    equipment = draft.equipment,
                    trackingType = draft.trackingType,
                    instructionsEs =
                        if (isSpanish || sameInstructions) {
                            draft.instructions
                        } else {
                            current.instructionsEs
                        },
                    instructionsEn =
                        if (!isSpanish || sameInstructions) {
                            draft.instructions
                        } else {
                            current.instructionsEn
                        },
                    updatedAt = clock.millis()
                )
            exerciseRepository.upsertExercise(updated)
            editSheetVisible.value = false
        }
    }

    fun onRequestToggleArchive() {
        archiveDialogVisible.value = true
    }

    fun onDismissArchiveDialog() {
        archiveDialogVisible.value = false
    }

    fun onConfirmToggleArchive() {
        val current = uiState.value.exercise ?: return
        viewModelScope.launch {
            exerciseRepository.setArchived(id = current.id, archived = !current.archived)
            archiveDialogVisible.value = false
        }
    }
}
