package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<Settings>

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setWeightUnit(unit: WeightUnit)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultRestSeconds(seconds: Int)

    suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek)

    suspend fun setTimerSoundEnabled(enabled: Boolean)

    suspend fun setTimerVibrationEnabled(enabled: Boolean)
}
