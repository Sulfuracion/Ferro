package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/** Variantes del botón Ferro según docs/design/components/Button/README.md. */
enum class FerroButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Danger
}

/**
 * Botón estándar de Ferro: alto `buttonHeight` (48 dp), radio `radius.md` (8 dp),
 * texto `bodyStrong` en una línea e icono opcional de `iconMd` (20 dp) a la izquierda.
 */
@Composable
fun FerroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FerroButtonVariant = FerroButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: Painter? = null
) {
    val colors = FerroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val haptic = LocalHapticFeedback.current

    val containerColor =
        when (variant) {
            FerroButtonVariant.Primary -> if (isPressed) colors.brasaPressed else colors.brasa
            FerroButtonVariant.Secondary -> if (isPressed) colors.surfaceRaised else Color.Transparent
            FerroButtonVariant.Ghost -> if (isPressed) colors.brasaSoft else Color.Transparent
            FerroButtonVariant.Danger -> if (isPressed) colors.surfaceRaised else Color.Transparent
        }

    val contentColor =
        when (variant) {
            FerroButtonVariant.Primary -> colors.onBrasa
            FerroButtonVariant.Secondary -> colors.ink
            FerroButtonVariant.Ghost -> colors.brasaText
            FerroButtonVariant.Danger -> colors.danger
        }

    val borderStroke =
        when (variant) {
            FerroButtonVariant.Secondary, FerroButtonVariant.Danger ->
                BorderStroke(1.dp, colors.lineStrong)

            FerroButtonVariant.Primary, FerroButtonVariant.Ghost -> null
        }

    val shape = RoundedCornerShape(FerroTheme.radius.md)
    val focusModifier =
        if (isFocused) {
            Modifier
                .padding(2.dp)
                .border(2.dp, colors.focus, shape)
                .padding(2.dp)
        } else {
            Modifier
        }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier =
            modifier
                .then(focusModifier)
                .defaultMinSize(
                    minWidth = FerroTheme.sizes.touchMin,
                    minHeight = FerroTheme.sizes.buttonHeight
                )
                .alpha(if (enabled) 1f else 0.40f),
        enabled = enabled,
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor
            ),
        border = borderStroke,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding =
            PaddingValues(
                horizontal = FerroTheme.spacing.space4,
                vertical = FerroTheme.spacing.space2
            ),
        interactionSource = interactionSource
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(FerroTheme.sizes.iconMd),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(FerroTheme.spacing.space1))
        }
        Text(
            text = text,
            style = FerroTheme.typography.bodyStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@FerroPreviews
@Composable
private fun FerroButtonPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .background(FerroTheme.colors.bg)
                    .padding(FerroTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
        ) {
            FerroButton(
                text = "Empezar entreno",
                onClick = {},
                variant = FerroButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
            FerroButton(
                text = "Añadir ejercicio",
                onClick = {},
                variant = FerroButtonVariant.Secondary,
                leadingIcon = painterResource(R.drawable.ic_add),
                modifier = Modifier.fillMaxWidth()
            )
            FerroButton(
                text = "Añadir serie",
                onClick = {},
                variant = FerroButtonVariant.Ghost,
                leadingIcon = painterResource(R.drawable.ic_add)
            )
            FerroButton(
                text = "Descartar entreno",
                onClick = {},
                variant = FerroButtonVariant.Danger,
                leadingIcon = painterResource(R.drawable.ic_delete)
            )
            FerroButton(
                text = "Desactivado",
                onClick = {},
                enabled = false,
                variant = FerroButtonVariant.Primary
            )
        }
    }
}
