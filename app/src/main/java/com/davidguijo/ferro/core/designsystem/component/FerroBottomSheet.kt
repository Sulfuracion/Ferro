package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Hoja inferior modal de Ferro: fondo `surface`, esquinas superiores `radius.lg` (12 dp)
 * y asa de arrastre en `lineStrong`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerroBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = FerroTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape =
            RoundedCornerShape(
                topStart = FerroTheme.radius.lg,
                topEnd = FerroTheme.radius.lg
            ),
        containerColor = colors.surface,
        contentColor = colors.ink,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = FerroTheme.spacing.space3)
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = colors.lineStrong,
                            shape = RoundedCornerShape(FerroTheme.radius.sm)
                        )
            )
        }
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = FerroTheme.spacing.space5)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = FerroTheme.typography.heading,
                    color = colors.ink,
                    modifier =
                        Modifier.padding(
                            horizontal = FerroTheme.spacing.space4,
                            vertical = FerroTheme.spacing.space2
                        )
                )
            }
            content()
        }
    }
}

@FerroPreviews
@Composable
private fun FerroBottomSheetContentPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = FerroTheme.colors.surface,
                        shape =
                            RoundedCornerShape(
                                topStart = FerroTheme.radius.lg,
                                topEnd = FerroTheme.radius.lg
                            )
                    )
                    .padding(vertical = FerroTheme.spacing.space4)
        ) {
            Text(
                text = "Tipo de serie",
                style = FerroTheme.typography.heading,
                color = FerroTheme.colors.ink,
                modifier = Modifier.padding(horizontal = FerroTheme.spacing.space4)
            )
            FerroListItem(title = "Normal", onClick = {})
            FerroDivider()
            FerroListItem(title = "Calentamiento (W)", onClick = {})
        }
    }
}
