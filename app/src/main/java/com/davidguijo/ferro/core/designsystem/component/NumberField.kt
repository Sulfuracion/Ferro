package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Campo numérico de Ferro para peso, reps o duración.
 * Usa teclado decimal, acepta coma o punto según el idioma del sistema y cifras tabulares.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    allowDecimal: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    showBorder: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    textAlign: TextAlign = TextAlign.Center,
    locale: Locale = Locale.getDefault()
) {
    val colors = FerroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val decimalSeparator =
        remember(locale) {
            DecimalFormatSymbols.getInstance(locale).decimalSeparator
        }

    val shape = RoundedCornerShape(FerroTheme.radius.md)
    val borderColor =
        when {
            isError -> colors.danger
            isFocused -> colors.focus
            showBorder -> colors.lineStrong
            else -> null
        }
    val borderWidth = if (isFocused) 2.dp else 1.dp

    val borderModifier =
        if (borderColor != null) {
            Modifier.border(borderWidth, borderColor, shape)
        } else {
            Modifier
        }

    val textStyle: TextStyle =
        FerroTheme.typography.number.copy(
            color = colors.ink,
            textAlign = textAlign
        )

    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val sanitized =
                sanitizeNumericInput(
                    input = raw,
                    allowDecimal = allowDecimal,
                    decimalSeparator = decimalSeparator
                )
            onValueChange(sanitized)
        },
        modifier =
            modifier
                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                .background(colors.surfaceRaised, shape)
                .then(borderModifier)
                .padding(
                    horizontal = FerroTheme.spacing.space2,
                    vertical = FerroTheme.spacing.space2
                ),
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.focus),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = imeAction
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = FerroTheme.typography.number.copy(textAlign = textAlign),
                        color = colors.inkSubtle,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

internal fun sanitizeNumericInput(input: String, allowDecimal: Boolean, decimalSeparator: Char): String {
    val builder = StringBuilder(input.length)
    var hasSeparator = false
    for (ch in input) {
        when {
            ch.isDigit() -> builder.append(ch)

            allowDecimal && (ch == '.' || ch == ',') && !hasSeparator -> {
                builder.append(decimalSeparator)
                hasSeparator = true
            }
        }
    }
    return builder.toString()
}

@FerroPreviews
@Composable
private fun NumberFieldPreview() {
    FerroTheme {
        Column(
            modifier =
                Modifier
                    .background(FerroTheme.colors.surface)
                    .padding(FerroTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space3)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
            ) {
                NumberField(
                    value = "102,5",
                    onValueChange = {},
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = "",
                    onValueChange = {},
                    placeholder = "8-10",
                    allowDecimal = false,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = "80",
                    onValueChange = {},
                    isError = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
