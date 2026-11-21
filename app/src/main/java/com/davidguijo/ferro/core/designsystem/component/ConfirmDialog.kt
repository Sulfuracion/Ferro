package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Diálogo de confirmación de Ferro. Usa variante `Danger` por defecto para acciones destructivas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = stringResource(R.string.dialog_cancel),
    destructive: Boolean = true
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        ConfirmDialogContent(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel,
            destructive = destructive,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun ConfirmDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(FerroTheme.radius.lg),
        color = FerroTheme.colors.surface,
        contentColor = FerroTheme.colors.ink,
        shadowElevation = FerroTheme.elevation.large
    ) {
        Column(modifier = Modifier.padding(FerroTheme.spacing.space5)) {
            Text(
                text = title,
                style = FerroTheme.typography.heading,
                color = FerroTheme.colors.ink
            )
            Spacer(modifier = Modifier.height(FerroTheme.spacing.space2))
            Text(
                text = message,
                style = FerroTheme.typography.body,
                color = FerroTheme.colors.inkMuted
            )
            Spacer(modifier = Modifier.height(FerroTheme.spacing.space5))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
            ) {
                FerroButton(
                    text = cancelLabel,
                    onClick = onDismiss,
                    variant = FerroButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FerroButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    variant =
                        if (destructive) {
                            FerroButtonVariant.Danger
                        } else {
                            FerroButtonVariant.Primary
                        },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@FerroPreviews
@Composable
private fun ConfirmDialogPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .background(FerroTheme.colors.bg)
                    .padding(FerroTheme.spacing.space4)
        ) {
            ConfirmDialogContent(
                title = "¿Borrar este entreno?",
                message = "No se puede deshacer.",
                confirmLabel = "Borrar",
                cancelLabel = "Cancelar",
                destructive = true,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}
