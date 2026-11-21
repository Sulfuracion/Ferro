package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevaciones de Ferro: ninguna en oscuro (manda el escalón de superficie);
 * pequeña en claro equivalente a shadow-sm.
 */
@Immutable
data class FerroElevation(val small: Dp = 0.dp, val large: Dp = 8.dp)

internal val DarkFerroElevation = FerroElevation(small = 0.dp, large = 8.dp)
internal val LightFerroElevation = FerroElevation(small = 2.dp, large = 8.dp)

internal val LocalFerroElevation = staticCompositionLocalOf { DarkFerroElevation }
