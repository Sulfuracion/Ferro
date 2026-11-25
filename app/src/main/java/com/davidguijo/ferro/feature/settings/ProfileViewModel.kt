package com.davidguijo.ferro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.BackupCalculations
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val isReady: Boolean = true,
    val showBackupReminder: Boolean = false,
    val daysSinceLastBackup: Int? = null
)

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    workoutRepository: WorkoutRepository,
    private val clock: Clock
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> =
        combine(
            settingsRepository.observeSettings(),
            workoutRepository.observeCompletedWorkouts()
        ) { settings, completedWorkouts ->
            val now = clock.millis()
            val lastBackupAt = settings.lastBackupAtMillis
            val daysSince = BackupCalculations.daysSinceLastBackup(lastBackupAt, now)
            val isStaleOver30Days =
                lastBackupAt != null &&
                    BackupCalculations.isBackupReminderDue(
                        lastBackupAtMillis = lastBackupAt,
                        nowMillis = now,
                        hasCompletedWorkouts = completedWorkouts.isNotEmpty()
                    )
            val isNeverBackedUpWithData = lastBackupAt == null && completedWorkouts.isNotEmpty()
            ProfileUiState(
                isReady = true,
                showBackupReminder = isStaleOver30Days || isNeverBackedUpWithData,
                daysSinceLastBackup = daysSince
            )
        }.catch {
            emit(ProfileUiState())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )
}
