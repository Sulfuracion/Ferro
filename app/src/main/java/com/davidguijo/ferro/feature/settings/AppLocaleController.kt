package com.davidguijo.ferro.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.davidguijo.ferro.core.domain.model.AppLanguage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Abstracción sobre AppCompatDelegate para cambiar y leer el idioma por app. */
interface AppLocaleController {
    fun getCurrentLanguage(): AppLanguage

    fun setLanguage(language: AppLanguage)
}

@Singleton
class AndroidAppLocaleController @Inject constructor() : AppLocaleController {
    override fun getCurrentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0]?.language) {
            TAG_ES -> AppLanguage.ES
            TAG_EN -> AppLanguage.EN
            else -> AppLanguage.SYSTEM
        }
    }

    override fun setLanguage(language: AppLanguage) {
        val locales =
            when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.ES -> LocaleListCompat.forLanguageTags(TAG_ES)
                AppLanguage.EN -> LocaleListCompat.forLanguageTags(TAG_EN)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private companion object {
        const val TAG_ES = "es"
        const val TAG_EN = "en"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindAppLocaleController(impl: AndroidAppLocaleController): AppLocaleController
}
