package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta semántica de Ferro derivada de docs/design/tokens.json.
 * Todos los colores de la UI salen de aquí a través de [FerroTheme.colors].
 */
@Immutable
data class FerroColors(
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkSubtle: Color,
    val brasa: Color,
    val brasaPressed: Color,
    val onBrasa: Color,
    val brasaText: Color,
    val brasaSoft: Color,
    val iron: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val setWarmup: Color,
    val setFailure: Color,
    val setDrop: Color,
    val focus: Color
) {
    /** Mapa con los nombres exactos de tokens.json para validación en tests. */
    internal fun asTokenMap(): Map<String, Color> = mapOf(
        "bg" to bg,
        "surface" to surface,
        "surface-raised" to surfaceRaised,
        "line" to line,
        "line-strong" to lineStrong,
        "ink" to ink,
        "ink-muted" to inkMuted,
        "ink-subtle" to inkSubtle,
        "brasa" to brasa,
        "brasa-pressed" to brasaPressed,
        "on-brasa" to onBrasa,
        "brasa-text" to brasaText,
        "brasa-soft" to brasaSoft,
        "iron" to iron,
        "success" to success,
        "warning" to warning,
        "danger" to danger,
        "set-warmup" to setWarmup,
        "set-failure" to setFailure,
        "set-drop" to setDrop,
        "focus" to focus
    )
}

private val DarkBrasa = Color(0xFFFF6B1F)
private val DarkDanger = Color(0xFFFF8F99)

val DarkFerroColors: FerroColors =
    FerroColors(
        bg = Color(0xFF0E0F11),
        surface = Color(0xFF17181B),
        surfaceRaised = Color(0xFF212328),
        line = Color(0xFF2D3036),
        lineStrong = Color(0xFF6A6F78),
        ink = Color(0xFFF3F1ED),
        inkMuted = Color(0xFFA8ACB3),
        inkSubtle = Color(0xFF8A8F98),
        brasa = DarkBrasa,
        brasaPressed = Color(0xFFE85A12),
        onBrasa = Color(0xFF170A02),
        brasaText = DarkBrasa,
        brasaSoft = Color(0xFF3A1E10),
        iron = Color(0xFF1C1E22),
        success = Color(0xFF3DD68C),
        warning = Color(0xFFF2B53A),
        danger = DarkDanger,
        setWarmup = Color(0xFF6EA8FF),
        setFailure = DarkDanger,
        setDrop = Color(0xFFB794F6),
        focus = DarkBrasa
    )

private val LightBrasaText = Color(0xFFB8440A)
private val LightDanger = Color(0xFFC81E3A)

val LightFerroColors: FerroColors =
    FerroColors(
        bg = Color(0xFFF4F3F0),
        surface = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFFFFFFF),
        line = Color(0xFFE2E0DB),
        lineStrong = Color(0xFF8C8881),
        ink = Color(0xFF141518),
        inkMuted = Color(0xFF50545B),
        inkSubtle = Color(0xFF6A6E76),
        brasa = Color(0xFFEA580C),
        brasaPressed = Color(0xFFD94F0A),
        onBrasa = Color(0xFF170A02),
        brasaText = LightBrasaText,
        brasaSoft = Color(0xFFFDE6D6),
        iron = Color(0xFF1C1E22),
        success = Color(0xFF15803D),
        warning = Color(0xFF9A5B06),
        danger = LightDanger,
        setWarmup = Color(0xFF2058D8),
        setFailure = LightDanger,
        setDrop = Color(0xFF7A3BE0),
        focus = LightBrasaText
    )

internal fun FerroColors.toMaterialColorScheme(isDark: Boolean): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = brasa,
        onPrimary = onBrasa,
        primaryContainer = brasaSoft,
        onPrimaryContainer = brasaText,
        secondary = surfaceRaised,
        onSecondary = ink,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = ink,
        tertiary = setWarmup,
        onTertiary = onBrasa,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        onError = onBrasa
    )
} else {
    lightColorScheme(
        primary = brasa,
        onPrimary = onBrasa,
        primaryContainer = brasaSoft,
        onPrimaryContainer = brasaText,
        secondary = surfaceRaised,
        onSecondary = ink,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = ink,
        tertiary = setWarmup,
        onTertiary = onBrasa,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        onError = onBrasa
    )
}

internal val LocalFerroColors = staticCompositionLocalOf { DarkFerroColors }
