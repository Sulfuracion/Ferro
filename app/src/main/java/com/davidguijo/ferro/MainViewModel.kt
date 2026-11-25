package com.davidguijo.ferro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.RestTimer
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import com.davidguijo.ferro.feature.workout.ActiveWorkoutMiniBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

@HiltViewModel
class MainViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    private val activeWorkoutManager: ActiveWorkoutManager,
    private val clock: Clock
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        settingsRepository
            .observeSettings()
            .map { it.themeMode }
            .catch { emit(ThemeMode.SYSTEM) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ThemeMode.SYSTEM
            )

    val onboardingCompleted: StateFlow<Boolean?> =
        settingsRepository
            .observeOnboardingCompleted()
            .map<Boolean, Boolean?> { it }
            .catch { emit(true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    private val _openActiveWorkoutSignal = MutableStateFlow(0)
    val openActiveWorkoutSignal: StateFlow<Int> = _openActiveWorkoutSignal.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val miniBarState: StateFlow<ActiveWorkoutMiniBarUiState?> =
        activeWorkoutManager.state
            .flatMapLatest { session ->
                val workout = session.workout ?: return@flatMapLatest flowOf<ActiveWorkoutMiniBarUiState?>(null)
                val restTimer = session.restTimer
                flow {
                    while (currentCoroutineContext().isActive) {
                        val now = clock.millis()
                        val elapsed = formatElapsedDuration((now - workout.workout.startedAt).coerceAtLeast(0L))
                        val activeRest =
                            if (restTimer != null && !RestTimer.isFinished(restTimer, clock)) {
                                restTimer
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
                            ActiveWorkoutMiniBarUiState(
                                workoutTitle = workout.workout.name,
                                elapsedText = elapsed,
                                restRemainingText =
                                    activeRest?.let {
                                        RestTimer.formatRemaining(RestTimer.remainingSeconds(it, clock))
                                    },
                                restProgress = activeRest?.let { RestTimer.progress(it, clock) }
                            )
                        )
                        delay(1_000L)
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun requestOpenActiveWorkout() {
        _openActiveWorkoutSignal.update { it + 1 }
    }
}
