package com.davidguijo.ferro.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(val settings: Settings = Settings())

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLocaleController: AppLocaleController
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        settingsRepository
            .observeSettings()
            .map { SettingsUiState(settings = it) }
            .catch { emit(SettingsUiState()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState()
            )

    init {
        syncSystemLocale()
    }

    /**
     * Sincroniza el idioma actual del sistema (p. ej. si el usuario lo cambió desde
     * los ajustes de Android) con el repositorio de preferencias.
     */
    fun syncSystemLocale() {
        viewModelScope.launch {
            runCatching {
                val systemAppLanguage = appLocaleController.getCurrentLanguage()
                val storedLanguage = settingsRepository.observeSettings().first().language
                if (systemAppLanguage != storedLanguage) {
                    settingsRepository.setLanguage(systemAppLanguage)
                }
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.setLanguage(language)
                appLocaleController.setLanguage(language)
            }
        }
    }

    fun onWeightUnitSelected(unit: WeightUnit) {
        viewModelScope.launch {
            runCatching { settingsRepository.setWeightUnit(unit) }
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            runCatching { settingsRepository.setThemeMode(mode) }
        }
    }

    fun onDecreaseDefaultRest() {
        val current = uiState.value.settings.defaultRestSeconds
        val next = (current - REST_STEP_SECONDS).coerceAtLeast(Settings.MIN_REST_SECONDS)
        if (next != current) {
            viewModelScope.launch {
                runCatching { settingsRepository.setDefaultRestSeconds(next) }
            }
        }
    }

    fun onIncreaseDefaultRest() {
        val current = uiState.value.settings.defaultRestSeconds
        val next = (current + REST_STEP_SECONDS).coerceAtMost(Settings.MAX_REST_SECONDS)
        if (next != current) {
            viewModelScope.launch {
                runCatching { settingsRepository.setDefaultRestSeconds(next) }
            }
        }
    }

    fun onFirstDayOfWeekSelected(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            runCatching { settingsRepository.setFirstDayOfWeek(dayOfWeek) }
        }
    }

    fun onTimerSoundChanged(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsRepository.setTimerSoundEnabled(enabled) }
        }
    }

    fun onTimerVibrationChanged(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsRepository.setTimerVibrationEnabled(enabled) }
        }
    }

    companion object {
        const val REST_STEP_SECONDS = 15
    }
}
