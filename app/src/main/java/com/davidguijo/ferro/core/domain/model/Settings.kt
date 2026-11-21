package com.davidguijo.ferro.core.domain.model

import com.davidguijo.ferro.core.domain.WeightUnit
import java.time.DayOfWeek

/** Idioma de la interfaz elegido por el usuario en Ajustes. */
enum class AppLanguage {
    SYSTEM,
    ES,
    EN
}

/** Tema visual de la aplicación. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Preferencias de usuario persistidas en DataStore. */
data class Settings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val timerSoundEnabled: Boolean = true,
    val timerVibrationEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
    }
}
