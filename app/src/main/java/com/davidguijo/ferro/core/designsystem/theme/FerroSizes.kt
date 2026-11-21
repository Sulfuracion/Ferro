package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tamaños mínimos e iconos de Ferro (tokens size.* de tokens.json). */
@Immutable
data class FerroSizes(
    val touchMin: Dp = 48.dp,
    val rowHeight: Dp = 52.dp,
    val buttonHeight: Dp = 48.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp
)

internal val LocalFerroSizes = staticCompositionLocalOf { FerroSizes() }
