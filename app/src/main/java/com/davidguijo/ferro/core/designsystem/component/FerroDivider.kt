package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/** Divisor horizontal de 1 dp usando el token `line`. */
@Composable
fun FerroDivider(modifier: Modifier = Modifier, color: Color = FerroTheme.colors.line) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = color
    )
}

@FerroPreviews
@Composable
private fun FerroDividerPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .background(FerroTheme.colors.surface)
                    .padding(FerroTheme.spacing.space4)
        ) {
            FerroDivider()
        }
    }
}
