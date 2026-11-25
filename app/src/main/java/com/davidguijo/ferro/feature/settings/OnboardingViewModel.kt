package com.davidguijo.ferro.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class OnboardingUiState(
    val currentStep: Int = 0,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = Settings.DEFAULT_REST_SECONDS,
    val isCompleted: Boolean = false,
    val hasError: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLocaleController: AppLocaleController
) : ViewModel() {

    private val currentStep = MutableStateFlow(0)
    private val isCompleted = MutableStateFlow(false)
    private val hasError = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> =
        combine(
            settingsRepository.observeSettings().catch { emit(Settings()) },
            currentStep,
            isCompleted,
            hasError
        ) { settings, step, completed, error ->
            OnboardingUiState(
                currentStep = step,
                language = settings.language,
                weightUnit = settings.weightUnit,
                defaultRestSeconds = settings.defaultRestSeconds,
                isCompleted = completed,
                hasError = error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OnboardingUiState()
        )

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setLanguage(language)
                appLocaleController.setLanguage(language)
            }.onFailure {
                hasError.value = true
            }.onSuccess {
                hasError.value = false
            }
        }
    }

    fun onWeightUnitSelected(unit: WeightUnit) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setWeightUnit(unit)
            }.onFailure {
                hasError.value = true
            }.onSuccess {
                hasError.value = false
            }
        }
    }

    fun onDefaultRestSelected(seconds: Int) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setDefaultRestSeconds(seconds)
            }.onFailure {
                hasError.value = true
            }.onSuccess {
                hasError.value = false
            }
        }
    }

    fun onDecreaseDefaultRest() {
        val current = uiState.value.defaultRestSeconds
        val next = (current - REST_STEP_SECONDS).coerceAtLeast(Settings.MIN_REST_SECONDS)
        if (next != current) {
            onDefaultRestSelected(next)
        }
    }

    fun onIncreaseDefaultRest() {
        val current = uiState.value.defaultRestSeconds
        val next = (current + REST_STEP_SECONDS).coerceAtMost(Settings.MAX_REST_SECONDS)
        if (next != current) {
            onDefaultRestSelected(next)
        }
    }

    fun onNextStep() {
        if (currentStep.value < TOTAL_STEPS - 1) {
            currentStep.update { it + 1 }
        } else {
            completeOnboarding()
        }
    }

    fun onPreviousStep() {
        if (currentStep.value > 0) {
            currentStep.update { it - 1 }
        }
    }

    fun onSkip() {
        completeOnboarding()
    }

    fun dismissError() {
        hasError.value = false
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setOnboardingCompleted(true)
            }.onFailure {
                hasError.value = true
            }.onSuccess {
                isCompleted.value = true
            }
        }
    }

    companion object {
        const val TOTAL_STEPS = 3
        const val REST_STEP_SECONDS = 15
    }
}
