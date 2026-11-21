package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Estado vacío de Ferro con aire (`space7` = 48 dp), icono opcional, título,
 * descripción y acción principal o secundaria.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: Painter? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(FerroTheme.spacing.space7),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = FerroTheme.colors.inkMuted
            )
            Spacer(modifier = Modifier.height(FerroTheme.spacing.space4))
        }
        Text(
            text = title,
            style = FerroTheme.typography.heading,
            color = FerroTheme.colors.ink,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(FerroTheme.spacing.space2))
            Text(
                text = description,
                style = FerroTheme.typography.body,
                color = FerroTheme.colors.inkMuted,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(FerroTheme.spacing.space5))
            FerroButton(
                text = actionLabel,
                onClick = onActionClick,
                variant = FerroButtonVariant.Primary
            )
        }
    }
}

@FerroPreviews
@Composable
private fun EmptyStatePreview() {
    FerroTheme {
        Column(modifier = Modifier.background(FerroTheme.colors.bg)) {
            EmptyState(
                title = "Aún no hay entrenos",
                description = "Empieza el primero.",
                icon = painterResource(R.drawable.ic_fitness_center),
                actionLabel = "Empezar entreno",
                onActionClick = {}
            )
        }
    }
}
