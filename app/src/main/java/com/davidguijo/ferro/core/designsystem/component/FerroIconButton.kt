package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Botón de icono con objetivo táctil mínimo de `touchMin` (48 dp).
 * Requiere [contentDescription] para accesibilidad.
 */
@Composable
fun FerroIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = FerroTheme.colors.ink,
    containerColor: Color = Color.Transparent,
    iconSize: Dp = FerroTheme.sizes.iconLg
) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        modifier =
            modifier
                .size(FerroTheme.sizes.touchMin)
                .background(containerColor, RoundedCornerShape(FerroTheme.radius.md))
                .alpha(if (enabled) 1f else 0.40f),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = tint,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = tint
            )
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

@FerroPreviews
@Composable
private fun FerroIconButtonPreview() {
    FerroTheme {
        Row(
            modifier =
                Modifier
                    .background(FerroTheme.colors.bg)
                    .padding(FerroTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
        ) {
            FerroIconButton(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Añadir",
                onClick = {}
            )
            FerroIconButton(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = "Temporizador",
                tint = FerroTheme.colors.brasaText,
                onClick = {}
            )
            FerroIconButton(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Borrar",
                tint = FerroTheme.colors.danger,
                enabled = false,
                onClick = {}
            )
        }
    }
}
