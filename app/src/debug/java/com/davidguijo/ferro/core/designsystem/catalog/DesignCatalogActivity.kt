package com.davidguijo.ferro.core.designsystem.catalog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.designsystem.theme.FerroThemeController
import com.davidguijo.ferro.core.designsystem.theme.LocalFerroThemeController

/** Activity solo de debug para inspeccionar el sistema de diseño Ferro. */
class DesignCatalogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = remember { FerroThemeController() }
            val systemDark = isSystemInDarkTheme()
            CompositionLocalProvider(LocalFerroThemeController provides controller) {
                FerroTheme(darkTheme = controller.isDark(systemDark)) {
                    DesignCatalogScreen()
                }
            }
        }
    }
}
