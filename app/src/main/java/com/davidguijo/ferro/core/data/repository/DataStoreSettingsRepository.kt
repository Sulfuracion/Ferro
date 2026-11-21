package com.davidguijo.ferro.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override fun observeSettings(): Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toSettings() }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { prefs ->
            prefs[KEY_WEIGHT_UNIT] = unit.name
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(Settings.MIN_REST_SECONDS, Settings.MAX_REST_SECONDS)
        dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REST_SECONDS] = clamped
        }
    }

    override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_DAY_OF_WEEK] = dayOfWeek.name
        }
    }

    override suspend fun setTimerSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_SOUND_ENABLED] = enabled
        }
    }

    override suspend fun setTimerVibrationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_VIBRATION_ENABLED] = enabled
        }
    }

    private fun Preferences.toSettings(): Settings = Settings(
        language = enumValueOrDefault(this[KEY_LANGUAGE], AppLanguage.SYSTEM),
        weightUnit = enumValueOrDefault(this[KEY_WEIGHT_UNIT], WeightUnit.KG),
        themeMode = enumValueOrDefault(this[KEY_THEME_MODE], ThemeMode.SYSTEM),
        defaultRestSeconds =
            (this[KEY_DEFAULT_REST_SECONDS] ?: Settings.DEFAULT_REST_SECONDS)
                .coerceIn(Settings.MIN_REST_SECONDS, Settings.MAX_REST_SECONDS),
        firstDayOfWeek = enumValueOrDefault(this[KEY_FIRST_DAY_OF_WEEK], DayOfWeek.MONDAY),
        timerSoundEnabled = this[KEY_TIMER_SOUND_ENABLED] ?: true,
        timerVibrationEnabled = this[KEY_TIMER_VIBRATION_ENABLED] ?: true
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val KEY_FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val KEY_TIMER_SOUND_ENABLED = booleanPreferencesKey("timer_sound_enabled")
        val KEY_TIMER_VIBRATION_ENABLED = booleanPreferencesKey("timer_vibration_enabled")
    }
}
