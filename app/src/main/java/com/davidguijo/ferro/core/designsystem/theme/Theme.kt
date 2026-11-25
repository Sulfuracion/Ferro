package com.davidguijo.ferro.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.davidguijo.ferro.core.domain.model.ThemeMode

/** Modo de tema en memoria (se persistirá en Ajustes en el prompt 4). */
enum class FerroThemeMode {
    System,
    Light,
    Dark
}

/** Controlador en memoria para forzar claro u oscuro en la sesión actual. */
class FerroThemeController(initialMode: FerroThemeMode = FerroThemeMode.System) {
    var mode: FerroThemeMode by mutableStateOf(initialMode)

    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        FerroThemeMode.System -> systemDark
        FerroThemeMode.Light -> false
        FerroThemeMode.Dark -> true
    }
}

val LocalFerroThemeController = staticCompositionLocalOf { FerroThemeController() }

@Immutable
private data class FerroThemeDarkFlag(val isDark: Boolean)

private val LocalFerroIsDark = staticCompositionLocalOf { FerroThemeDarkFlag(true) }

/**
 * Tema Ferro que lee el [ThemeMode] elegido en Ajustes.
 */
@Composable
fun FerroTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    FerroTheme(darkTheme = darkTheme, content = content)
}

/**
 * Tema Ferro. Por defecto sigue al sistema; [darkTheme] permite forzar el modo manual.
 * Sin color dinámico: la marca manda. Configura además los iconos de las barras del sistema
 * para edge-to-edge según el tema activo.
 */
@Composable
fun FerroTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkFerroColors else LightFerroColors
    val typography = FerroTypography()
    val elevation = if (darkTheme) DarkFerroElevation else LightFerroElevation

    val view = LocalView.current
    val reduceMotion = if (view.isInEditMode) false else rememberSystemReduceMotion()
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFerroColors provides colors,
        LocalFerroTypography provides typography,
        LocalFerroSpacing provides FerroSpacing(),
        LocalFerroRadius provides FerroRadius(),
        LocalFerroSizes provides FerroSizes(),
        LocalFerroElevation provides elevation,
        LocalFerroIsDark provides FerroThemeDarkFlag(darkTheme),
        LocalFerroReduceMotion provides FerroReduceMotionFlag(reduceMotion)
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(isDark = darkTheme),
            typography = typography.toMaterialTypography(),
            content = content
        )
    }
}

/** Acceso a todos los tokens de diseño Ferro desde Compose. */
object FerroTheme {
    val colors: FerroColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroColors.current

    val typography: FerroTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroTypography.current

    val spacing: FerroSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroSpacing.current

    val radius: FerroRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroRadius.current

    val sizes: FerroSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroSizes.current

    val elevation: FerroElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroElevation.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroIsDark.current.isDark

    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalFerroReduceMotion.current.reduceMotion
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
