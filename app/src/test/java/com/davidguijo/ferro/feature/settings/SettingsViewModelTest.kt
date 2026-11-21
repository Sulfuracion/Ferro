package com.davidguijo.ferro.feature.settings

import app.cash.turbine.test
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al seleccionar idioma actualiza repositorio y AppLocaleController`() = runTest {
        val repo = FakeSettingsRepository()
        val localeController = FakeAppLocaleController()
        val viewModel = SettingsViewModel(repo, localeController)

        viewModel.uiState.test {
            assertEquals(Settings(), awaitItem().settings)

            viewModel.onLanguageSelected(AppLanguage.EN)
            assertEquals(AppLanguage.EN, awaitItem().settings.language)
            assertEquals(AppLanguage.EN, localeController.current)

            viewModel.onLanguageSelected(AppLanguage.SYSTEM)
            assertEquals(AppLanguage.SYSTEM, awaitItem().settings.language)
            assertEquals(AppLanguage.SYSTEM, localeController.current)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `actualiza unidad, tema, descanso, inicio de semana y temporizador`() = runTest {
        val repo = FakeSettingsRepository()
        val localeController = FakeAppLocaleController()
        val viewModel = SettingsViewModel(repo, localeController)

        viewModel.uiState.test {
            assertEquals(Settings(), awaitItem().settings)

            viewModel.onWeightUnitSelected(WeightUnit.LB)
            assertEquals(WeightUnit.LB, awaitItem().settings.weightUnit)

            viewModel.onThemeModeSelected(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().settings.themeMode)

            viewModel.onIncreaseDefaultRest()
            assertEquals(105, awaitItem().settings.defaultRestSeconds)

            viewModel.onDecreaseDefaultRest()
            assertEquals(90, awaitItem().settings.defaultRestSeconds)

            viewModel.onFirstDayOfWeekSelected(DayOfWeek.SUNDAY)
            assertEquals(DayOfWeek.SUNDAY, awaitItem().settings.firstDayOfWeek)

            viewModel.onTimerSoundChanged(false)
            assertEquals(false, awaitItem().settings.timerSoundEnabled)

            viewModel.onTimerVibrationChanged(false)
            assertEquals(false, awaitItem().settings.timerVibrationEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncSystemLocale sincroniza idioma del sistema si difiere del guardado`() = runTest {
        val repo = FakeSettingsRepository(Settings(language = AppLanguage.SYSTEM))
        val localeController = FakeAppLocaleController(initial = AppLanguage.ES)

        val viewModel = SettingsViewModel(repo, localeController)

        viewModel.uiState.test {
            assertEquals(AppLanguage.ES, awaitItem().settings.language)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<Settings> = state

        override suspend fun setLanguage(language: AppLanguage) {
            state.update { it.copy(language = language) }
        }

        override suspend fun setWeightUnit(unit: WeightUnit) {
            state.update { it.copy(weightUnit = unit) }
        }

        override suspend fun setThemeMode(mode: ThemeMode) {
            state.update { it.copy(themeMode = mode) }
        }

        override suspend fun setDefaultRestSeconds(seconds: Int) {
            state.update {
                it.copy(
                    defaultRestSeconds =
                        seconds.coerceIn(Settings.MIN_REST_SECONDS, Settings.MAX_REST_SECONDS)
                )
            }
        }

        override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
            state.update { it.copy(firstDayOfWeek = dayOfWeek) }
        }

        override suspend fun setTimerSoundEnabled(enabled: Boolean) {
            state.update { it.copy(timerSoundEnabled = enabled) }
        }

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) {
            state.update { it.copy(timerVibrationEnabled = enabled) }
        }
    }

    private class FakeAppLocaleController(initial: AppLanguage = AppLanguage.SYSTEM) : AppLocaleController {
        var current: AppLanguage = initial

        override fun getCurrentLanguage(): AppLanguage = current

        override fun setLanguage(language: AppLanguage) {
            current = language
        }
    }
}
