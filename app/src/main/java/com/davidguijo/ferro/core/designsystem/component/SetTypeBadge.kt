package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/** Tipo de serie según docs/design/components/SetTypeBadge/README.md. */
enum class FerroSetType {
    Normal,
    Warmup,
    Failure,
    Drop
}

/**
 * Insignia de tipo de serie (cuadrado de 28 dp sobre `surfaceRaised`, radio `radius.sm`)
 * con objetivo táctil completo de 48 dp (`touchMin`).
 */
@Composable
fun SetTypeBadge(type: FerroSetType, modifier: Modifier = Modifier, setNumber: Int = 1, onClick: (() -> Unit)? = null) {
    val colors = FerroTheme.colors
    val haptic = LocalHapticFeedback.current

    val labelText =
        when (type) {
            FerroSetType.Normal -> setNumber.toString()
            FerroSetType.Warmup -> "W"
            FerroSetType.Failure -> "F"
            FerroSetType.Drop -> "D"
        }

    val textColor =
        when (type) {
            FerroSetType.Normal -> colors.inkMuted
            FerroSetType.Warmup -> colors.setWarmup
            FerroSetType.Failure -> colors.setFailure
            FerroSetType.Drop -> colors.setDrop
        }

    val accessibilityLabel =
        when (type) {
            FerroSetType.Normal -> stringResource(R.string.set_type_normal_cd, setNumber)
            FerroSetType.Warmup -> stringResource(R.string.set_type_warmup_cd)
            FerroSetType.Failure -> stringResource(R.string.set_type_failure_cd)
            FerroSetType.Drop -> stringResource(R.string.set_type_drop_cd)
        }

    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .size(FerroTheme.sizes.touchMin)
                .clip(RoundedCornerShape(FerroTheme.radius.sm))
                .then(clickModifier)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .background(
                        color = colors.surfaceRaised,
                        shape = RoundedCornerShape(FerroTheme.radius.sm)
                    ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                style = FerroTheme.typography.number,
                color = textColor
            )
        }
    }
}

@FerroPreviews
@Composable
private fun SetTypeBadgePreview() {
    FerroTheme {
        Row(
            modifier =
                Modifier
                    .background(FerroTheme.colors.surface)
                    .padding(FerroTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
        ) {
            SetTypeBadge(type = FerroSetType.Warmup, onClick = {})
            SetTypeBadge(type = FerroSetType.Normal, setNumber = 1, onClick = {})
            SetTypeBadge(type = FerroSetType.Normal, setNumber = 2, onClick = {})
            SetTypeBadge(type = FerroSetType.Failure, onClick = {})
            SetTypeBadge(type = FerroSetType.Drop, onClick = {})
        }
    }
}
