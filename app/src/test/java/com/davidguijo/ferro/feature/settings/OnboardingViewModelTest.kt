package com.davidguijo.ferro.feature.settings

import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completing 3 steps saves preferences and marks onboarding completed`() = runTest {
        val repo = FakeSettingsRepository()
        val localeController = FakeAppLocaleController()
        val viewModel = OnboardingViewModel(repo, localeController)
        val collectJob = backgroundScope.launch { viewModel.uiState.collect() }

        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStep)

        viewModel.onLanguageSelected(AppLanguage.EN)
        viewModel.onNextStep()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)
        assertEquals(AppLanguage.EN, repo.settings.value.language)
        assertEquals(AppLanguage.EN, localeController.appliedLanguage)

        viewModel.onWeightUnitSelected(WeightUnit.LB)
        viewModel.onNextStep()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)
        assertEquals(WeightUnit.LB, repo.settings.value.weightUnit)

        viewModel.onDefaultRestSelected(120)
        viewModel.onNextStep()
        advanceUntilIdle()
        assertEquals(120, repo.settings.value.defaultRestSeconds)
        assertTrue(repo.onboardingCompleted.value)
        assertTrue(viewModel.uiState.value.isCompleted)

        collectJob.cancel()
    }

    @Test
    fun `skip marks onboarding completed immediately`() = runTest {
        val repo = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(repo, FakeAppLocaleController())
        val collectJob = backgroundScope.launch { viewModel.uiState.collect() }

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCompleted)

        viewModel.onSkip()
        advanceUntilIdle()

        assertTrue(repo.onboardingCompleted.value)
        assertTrue(viewModel.uiState.value.isCompleted)

        collectJob.cancel()
    }

    private class FakeSettingsRepository : SettingsRepository {
        val settings = MutableStateFlow(Settings())
        val onboardingCompleted = MutableStateFlow(false)

        override fun observeSettings(): Flow<Settings> = settings

        override fun observeOnboardingCompleted(): Flow<Boolean> = onboardingCompleted

        override suspend fun setLanguage(language: AppLanguage) {
            settings.value = settings.value.copy(language = language)
        }

        override suspend fun setWeightUnit(unit: WeightUnit) {
            settings.value = settings.value.copy(weightUnit = unit)
        }

        override suspend fun setThemeMode(mode: ThemeMode) {
            settings.value = settings.value.copy(themeMode = mode)
        }

        override suspend fun setDefaultRestSeconds(seconds: Int) {
            settings.value = settings.value.copy(defaultRestSeconds = seconds)
        }

        override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
            settings.value = settings.value.copy(firstDayOfWeek = dayOfWeek)
        }

        override suspend fun setTimerSoundEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(timerSoundEnabled = enabled)
        }

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(timerVibrationEnabled = enabled)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingCompleted.value = completed
        }
    }

    private class FakeAppLocaleController : AppLocaleController {
        var appliedLanguage: AppLanguage = AppLanguage.SYSTEM

        override fun getCurrentLanguage(): AppLanguage = appliedLanguage

        override fun setLanguage(language: AppLanguage) {
            appliedLanguage = language
        }
    }
}
