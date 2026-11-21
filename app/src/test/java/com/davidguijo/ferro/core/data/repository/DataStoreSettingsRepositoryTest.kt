package com.davidguijo.ferro.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import java.io.File
import java.io.IOException
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `emite valores por defecto inicialmente`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())

        repository.observeSettings().test {
            assertEquals(Settings(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `actualiza y persiste todas las preferencias`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())

        repository.observeSettings().test {
            assertEquals(Settings(), awaitItem())

            repository.setLanguage(AppLanguage.EN)
            assertEquals(AppLanguage.EN, awaitItem().language)

            repository.setWeightUnit(WeightUnit.LB)
            assertEquals(WeightUnit.LB, awaitItem().weightUnit)

            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            repository.setDefaultRestSeconds(120)
            assertEquals(120, awaitItem().defaultRestSeconds)

            repository.setFirstDayOfWeek(DayOfWeek.SUNDAY)
            assertEquals(DayOfWeek.SUNDAY, awaitItem().firstDayOfWeek)

            repository.setTimerSoundEnabled(false)
            assertEquals(false, awaitItem().timerSoundEnabled)

            repository.setTimerVibrationEnabled(false)
            assertEquals(false, awaitItem().timerVibrationEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `descanso por defecto se acota entre minimo y maximo`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSettingsRepository(InMemoryPreferencesDataStore())

        repository.setDefaultRestSeconds(5)
        repository.observeSettings().test {
            assertEquals(Settings.MIN_REST_SECONDS, awaitItem().defaultRestSeconds)
            cancelAndIgnoreRemainingEvents()
        }

        repository.setDefaultRestSeconds(9999)
        repository.observeSettings().test {
            assertEquals(Settings.MAX_REST_SECONDS, awaitItem().defaultRestSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recupera valores por defecto si DataStore lanza IOException`() = runTest(UnconfinedTestDispatcher()) {
        val failingDataStore =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow { throw IOException("disk error") }

                override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                    emptyPreferences()
            }
        val repository = DataStoreSettingsRepository(failingDataStore)

        repository.observeSettings().test {
            assertEquals(Settings(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `persiste en PreferenceDataStoreFactory real`() = runTest(UnconfinedTestDispatcher()) {
        val file = File(tempFolder.newFolder(), "real_settings.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file }
            )
        val repository = DataStoreSettingsRepository(dataStore)

        repository.setWeightUnit(WeightUnit.LB)
        repository.observeSettings().test {
            assertEquals(WeightUnit.LB, awaitItem().weightUnit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                val updated = transform(state.value.toPreferences())
                state.value = updated
                updated
            }
    }
}
