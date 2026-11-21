package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Chip de filtro o selección de Ferro: radio `radius.sm` (4 dp, sin pastillas)
 * y objetivo táctil mínimo de `touchMin` (48 dp).
 */
@Composable
fun FerroChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null
) {
    val colors = FerroTheme.colors
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (selected) colors.brasaSoft else colors.surfaceRaised
    val contentColor = if (selected) colors.brasaText else colors.ink
    val borderColor = if (selected) colors.brasaText else colors.lineStrong

    Box(
        modifier = modifier.defaultMinSize(minHeight = FerroTheme.sizes.touchMin),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            modifier =
                Modifier.semantics {
                    role = Role.Checkbox
                    this.selected = selected
                },
            shape = RoundedCornerShape(FerroTheme.radius.sm),
            color = backgroundColor,
            contentColor = contentColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = FerroTheme.spacing.space3,
                        vertical = FerroTheme.spacing.space2
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        painter = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconSm),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(FerroTheme.spacing.space1))
                }
                Text(
                    text = label,
                    style = FerroTheme.typography.caption,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@FerroPreviews
@Composable
private fun FerroChipPreview() {
    FerroTheme {
        Row(
            modifier =
                Modifier
                    .background(FerroTheme.colors.bg)
                    .padding(FerroTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
        ) {
            FerroChip(
                label = "Pecho",
                selected = true,
                onClick = {},
                leadingIcon = painterResource(R.drawable.ic_check)
            )
            FerroChip(
                label = "Espalda",
                selected = false,
                onClick = {}
            )
            FerroChip(
                label = "Pierna",
                selected = false,
                onClick = {}
            )
        }
    }
}
