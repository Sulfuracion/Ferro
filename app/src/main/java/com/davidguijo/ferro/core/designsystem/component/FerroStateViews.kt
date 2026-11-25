package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/** Indicador de carga centrado con el color `brasa` del tema Ferro. */
@Composable
fun FerroLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = FerroTheme.colors.brasa)
    }
}

/** Estado de error de pantalla completa con acción opcional de reintento. */
@Composable
fun FerroErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.space7)
                .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = FerroTheme.typography.body,
            color = colors.danger,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(spacing.space4))
            FerroButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                variant = FerroButtonVariant.Secondary
            )
        }
    }
}

/** Aviso no bloqueante de error al guardar o actualizar datos. */
@Composable
fun FerroErrorBanner(message: String, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    FerroCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            Text(
                text = message,
                style = FerroTheme.typography.caption,
                color = colors.danger,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(FerroTheme.sizes.touchMin)
                ) {
                    Text(
                        text = stringResource(R.string.action_dismiss),
                        style = FerroTheme.typography.bodyStrong,
                        color = colors.ink
                    )
                }
            }
        }
    }
}
