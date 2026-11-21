package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Tarjeta base de Ferro: radio `radius.lg` (12 dp), superficie `surface` o `surfaceRaised`,
 * sin elevación en oscuro y con elevación `small` en claro (equivalente a shadow-sm).
 */
@Composable
fun FerroCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(FerroTheme.spacing.space4),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = FerroTheme.colors
    val shape = RoundedCornerShape(FerroTheme.radius.lg)
    val containerColor = if (raised) colors.surfaceRaised else colors.surface
    val shadowElevation = FerroTheme.elevation.small
    val border =
        if (!FerroTheme.isDark) {
            BorderStroke(1.dp, colors.line)
        } else {
            null
        }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            contentColor = colors.ink,
            shadowElevation = shadowElevation,
            border = border
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            contentColor = colors.ink,
            shadowElevation = shadowElevation,
            border = border
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@FerroPreviews
@Composable
private fun FerroCardPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .background(FerroTheme.colors.bg)
                    .padding(FerroTheme.spacing.space4)
        ) {
            FerroCard {
                Text(
                    text = "Sentadilla con barra",
                    style = FerroTheme.typography.heading,
                    color = FerroTheme.colors.ink
                )
                Text(
                    text = "3 series · 100 kg",
                    style = FerroTheme.typography.caption,
                    color = FerroTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = FerroTheme.spacing.space1)
                )
            }
        }
    }
}
