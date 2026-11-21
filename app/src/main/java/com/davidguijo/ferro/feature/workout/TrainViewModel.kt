package com.davidguijo.ferro.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainRoutineItemUi(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val totalSetCount: Int,
    val exerciseNamesSummary: String
)

sealed interface TrainStartRequest {
    data class Empty(val defaultName: String) : TrainStartRequest

    data class FromRoutine(val routineId: String) : TrainStartRequest
}

data class TrainUiState(
    val routines: List<TrainRoutineItemUi> = emptyList(),
    val activeWorkoutName: String? = null,
    val pendingConflict: TrainStartRequest? = null,
    val shouldOpenActiveWorkout: Boolean = false
)

@HiltViewModel
class TrainViewModel
internal constructor(
    routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val activeWorkoutManager: ActiveWorkoutManager,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        routineRepository: RoutineRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        activeWorkoutManager: ActiveWorkoutManager
    ) : this(
        routineRepository = routineRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        activeWorkoutManager = activeWorkoutManager,
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val localOverlay = MutableStateFlow(LocalOverlay())

    init {
        viewModelScope.launch {
            activeWorkoutManager.restoreIfNeeded()
        }
    }

    val uiState: StateFlow<TrainUiState> =
        combine(
            routineRepository.observeAllRoutinesWithExercises(),
            exerciseRepository.observeAllExercises(),
            settingsRepository.observeSettings(),
            activeWorkoutManager.state,
            localOverlay
        ) { routines, exercises, settings, activeSession, overlay ->
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            val exercisesById = exercises.associateBy { it.id }
            val routineItems =
                routines
                    .sortedWith(
                        compareBy<RoutineWithExercises> { it.routine.position }
                            .thenBy { it.routine.name.lowercase() }
                    )
                    .map { rwe ->
                        rwe.toTrainRoutineItem(exercisesById, isSpanish)
                    }
            TrainUiState(
                routines = routineItems,
                activeWorkoutName = activeSession.workout?.workout?.name,
                pendingConflict = overlay.pendingConflict,
                shouldOpenActiveWorkout = overlay.shouldOpenActiveWorkout
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrainUiState()
        )

    fun onStartEmptyWorkoutClick(defaultWorkoutName: String) {
        handleStartRequest(TrainStartRequest.Empty(defaultWorkoutName))
    }

    fun onStartRoutineClick(routineId: String) {
        handleStartRequest(TrainStartRequest.FromRoutine(routineId))
    }

    fun onResumeActiveWorkout() {
        localOverlay.update {
            it.copy(
                pendingConflict = null,
                shouldOpenActiveWorkout = true
            )
        }
    }

    fun onDiscardAndStartPending() {
        val request = localOverlay.value.pendingConflict ?: return
        viewModelScope.launch {
            startWorkoutFromRequest(request)
            localOverlay.update {
                it.copy(
                    pendingConflict = null,
                    shouldOpenActiveWorkout = true
                )
            }
        }
    }

    fun onDismissConflictDialog() {
        localOverlay.update { it.copy(pendingConflict = null) }
    }

    fun onNavigatedToActiveWorkout() {
        localOverlay.update { it.copy(shouldOpenActiveWorkout = false) }
    }

    private fun handleStartRequest(request: TrainStartRequest) {
        viewModelScope.launch {
            activeWorkoutManager.restoreIfNeeded()
            if (activeWorkoutManager.hasActiveWorkout()) {
                localOverlay.update { it.copy(pendingConflict = request) }
            } else {
                startWorkoutFromRequest(request)
                localOverlay.update { it.copy(shouldOpenActiveWorkout = true) }
            }
        }
    }

    private suspend fun startWorkoutFromRequest(request: TrainStartRequest) {
        when (request) {
            is TrainStartRequest.Empty -> activeWorkoutManager.startEmptyWorkout(request.defaultName)
            is TrainStartRequest.FromRoutine -> activeWorkoutManager.startFromRoutine(request.routineId)
        }
    }

    private fun RoutineWithExercises.toTrainRoutineItem(
        exercisesById: Map<String, Exercise>,
        isSpanish: Boolean
    ): TrainRoutineItemUi = TrainRoutineItemUi(
        id = routine.id,
        name = routine.name,
        exerciseCount = exercises.size,
        totalSetCount = exercises.sumOf { it.sets.size },
        exerciseNamesSummary =
            exercises
                .mapNotNull { ex -> exercisesById[ex.routineExercise.exerciseId]?.localizedName(isSpanish) }
                .joinToString(" · ")
    )

    private data class LocalOverlay(
        val pendingConflict: TrainStartRequest? = null,
        val shouldOpenActiveWorkout: Boolean = false
    )
}
