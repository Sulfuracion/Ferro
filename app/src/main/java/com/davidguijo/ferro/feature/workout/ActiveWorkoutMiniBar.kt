package com.davidguijo.ferro.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

data class ActiveWorkoutMiniBarUiState(
    val workoutTitle: String,
    val elapsedText: String,
    val restRemainingText: String? = null,
    val restProgress: Float? = null
)

/**
 * Barra mini del entreno activo sobre la barra inferior.
 * Muestra nombre del entreno, cronómetro de sesión y, si hay descanso activo, la cuenta atrás.
 */
@Composable
fun ActiveWorkoutMiniBar(
    visible: Boolean,
    workoutTitle: String,
    elapsedText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    restRemainingText: String? = null,
    restProgress: Float? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val colors = FerroTheme.colors
        val spacing = FerroTheme.spacing
        val displayTitle = workoutTitle.ifBlank { stringResource(R.string.active_workout_default_name) }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (restProgress != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.line)
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(restProgress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(colors.brasa)
                    )
                }
            } else {
                FerroDivider()
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceRaised)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.active_workout_mini_bar_cd),
                            onClick = onClick
                        )
                        .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                        .padding(horizontal = spacing.space4, vertical = spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fitness_center),
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconMd),
                        tint = colors.brasaText
                    )
                    Spacer(modifier = Modifier.width(spacing.space3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = FerroTheme.typography.bodyStrong,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (restRemainingText != null) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.active_workout_mini_bar_rest_format,
                                        restRemainingText
                                    ),
                                style = FerroTheme.typography.caption,
                                color = colors.brasaText,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(spacing.space3))
                Text(
                    text = elapsedText,
                    style = FerroTheme.typography.number,
                    color = colors.ink
                )
            }
        }
    }
}

@FerroPreviews
@Composable
private fun ActiveWorkoutMiniBarPreview() {
    FerroTheme {
        ActiveWorkoutMiniBar(
            visible = true,
            workoutTitle = "Torso A",
            elapsedText = "24:15",
            restRemainingText = "1:12",
            restProgress = 0.8f,
            onClick = {}
        )
    }
}
