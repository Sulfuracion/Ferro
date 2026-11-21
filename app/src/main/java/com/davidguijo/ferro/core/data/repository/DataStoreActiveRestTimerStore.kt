package com.davidguijo.ferro.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.repository.ActiveRestTimerStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

@Singleton
class DataStoreActiveRestTimerStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    ActiveRestTimerStore {

    override suspend fun load(): RestTimerState? {
        val prefs =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .first()
        val workoutId = prefs[KEY_WORKOUT_ID] ?: return null
        val startedAt = prefs[KEY_STARTED_AT] ?: return null
        val endAt = prefs[KEY_END_AT] ?: return null
        val totalSeconds = prefs[KEY_TOTAL_SECONDS] ?: return null
        if (totalSeconds <= 0) return null
        return RestTimerState(
            workoutId = workoutId,
            exerciseId = prefs[KEY_EXERCISE_ID],
            startedAtMillis = startedAt,
            endAtMillis = endAt,
            totalDurationSeconds = totalSeconds
        )
    }

    override suspend fun save(state: RestTimerState?) {
        dataStore.edit { prefs ->
            if (state == null) {
                prefs.remove(KEY_WORKOUT_ID)
                prefs.remove(KEY_EXERCISE_ID)
                prefs.remove(KEY_STARTED_AT)
                prefs.remove(KEY_END_AT)
                prefs.remove(KEY_TOTAL_SECONDS)
            } else {
                prefs[KEY_WORKOUT_ID] = state.workoutId
                if (state.exerciseId != null) {
                    prefs[KEY_EXERCISE_ID] = state.exerciseId
                } else {
                    prefs.remove(KEY_EXERCISE_ID)
                }
                prefs[KEY_STARTED_AT] = state.startedAtMillis
                prefs[KEY_END_AT] = state.endAtMillis
                prefs[KEY_TOTAL_SECONDS] = state.totalDurationSeconds
            }
        }
    }

    private companion object {
        val KEY_WORKOUT_ID = stringPreferencesKey("active_rest_workout_id")
        val KEY_EXERCISE_ID = stringPreferencesKey("active_rest_exercise_id")
        val KEY_STARTED_AT = longPreferencesKey("active_rest_started_at")
        val KEY_END_AT = longPreferencesKey("active_rest_end_at")
        val KEY_TOTAL_SECONDS = intPreferencesKey("active_rest_total_seconds")
    }
}
