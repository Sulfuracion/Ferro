package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Radios de esquina de Ferro (tokens radius-sm, radius-md, radius-lg). */
@Immutable
data class FerroRadius(val sm: Dp = 4.dp, val md: Dp = 8.dp, val lg: Dp = 12.dp)

internal val LocalFerroRadius = staticCompositionLocalOf { FerroRadius() }
