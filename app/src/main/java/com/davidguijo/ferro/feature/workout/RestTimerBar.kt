package com.davidguijo.ferro.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroHaptics
import com.davidguijo.ferro.core.designsystem.theme.FerroMotion
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import kotlinx.coroutines.delay

/**
 * Barra fija inferior de cuenta atrás del descanso según docs/design/components/RestTimer/README.md.
 * Parpadea brevemente al llegar a cero antes de ocultarse.
 */
@Composable
fun RestTimerBar(
    restTimer: ActiveRestTimerUi?,
    completedRestEventId: Long,
    showExactAlarmHint: Boolean,
    onOpenExactAlarmSettings: () -> Unit,
    onAdjustSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = FerroTheme.reduceMotion
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isFlashingCompletion by remember { mutableStateOf(false) }
    var lastSeenCompletedId by remember { mutableLongStateOf(completedRestEventId) }

    LaunchedEffect(completedRestEventId) {
        if (completedRestEventId > lastSeenCompletedId) {
            lastSeenCompletedId = completedRestEventId
            FerroHaptics.confirm(view, haptic)
            isFlashingCompletion = true
            delay(COMPLETION_FLASH_DURATION_MS)
            isFlashingCompletion = false
        }
    }

    val isVisible = restTimer != null || isFlashingCompletion
    val displayedTimer =
        restTimer ?: if (isFlashingCompletion) {
            ActiveRestTimerUi(
                remainingSeconds = 0,
                remainingText = "0:00",
                progress = 0f
            )
        } else {
            null
        }

    AnimatedVisibility(
        visible = isVisible && displayedTimer != null,
        enter =
            if (reduceMotion) {
                fadeIn(FerroMotion.stateSpec())
            } else {
                fadeIn(FerroMotion.stateSpec()) + slideInVertically(FerroMotion.stateSpec()) { it / 2 }
            },
        exit =
            if (reduceMotion) {
                fadeOut(FerroMotion.stateSpec())
            } else {
                fadeOut(FerroMotion.stateSpec()) + slideOutVertically(FerroMotion.stateSpec()) { it / 2 }
            },
        modifier = modifier
    ) {
        val timer = displayedTimer ?: return@AnimatedVisibility
        val colors = FerroTheme.colors
        val spacing = FerroTheme.spacing

        val containerColor by
            animateColorAsState(
                targetValue = if (isFlashingCompletion) colors.brasaSoft else colors.surfaceRaised,
                animationSpec = tween(durationMillis = COMPLETION_FLASH_DURATION_MS.toInt()),
                label = "rest_timer_flash"
            )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    topStart = FerroTheme.radius.lg,
                    topEnd = FerroTheme.radius.lg
                ),
            color = containerColor,
            contentColor = colors.ink,
            shadowElevation = FerroTheme.elevation.large
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                                .exclude(WindowInsets.ime)
                                .only(WindowInsetsSides.Bottom)
                        )
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(colors.line)
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(timer.progress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(colors.brasa)
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = spacing.space4,
                                vertical = spacing.space3
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.rest_timer_label).uppercase(),
                                style = FerroTheme.typography.label,
                                color = colors.inkMuted
                            )
                            Text(
                                text = timer.remainingText,
                                style = FerroTheme.typography.numberXl,
                                color = if (isFlashingCompletion) colors.brasaText else colors.ink
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                        ) {
                            FerroButton(
                                text = stringResource(R.string.rest_timer_minus_15),
                                onClick = { onAdjustSeconds(-REST_STEP_SECONDS) },
                                variant = FerroButtonVariant.Secondary,
                                enabled = !isFlashingCompletion
                            )
                            FerroButton(
                                text = stringResource(R.string.rest_timer_plus_15),
                                onClick = { onAdjustSeconds(REST_STEP_SECONDS) },
                                variant = FerroButtonVariant.Secondary,
                                enabled = !isFlashingCompletion
                            )
                            FerroButton(
                                text = stringResource(R.string.rest_timer_skip),
                                onClick = onSkip,
                                variant = FerroButtonVariant.Ghost,
                                enabled = !isFlashingCompletion
                            )
                        }
                    }

                    if (showExactAlarmHint) {
                        Spacer(modifier = Modifier.height(spacing.space1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.active_workout_exact_alarm_hint),
                                style = FerroTheme.typography.caption,
                                color = colors.warning,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(spacing.space2))
                            TextButton(onClick = onOpenExactAlarmSettings) {
                                Text(
                                    text = stringResource(R.string.active_workout_exact_alarm_settings_action),
                                    style = FerroTheme.typography.bodyStrong,
                                    color = colors.brasaText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val REST_STEP_SECONDS = 15
private const val ANIM_DURATION_MS = 200
private const val COMPLETION_FLASH_DURATION_MS = 200L

@FerroPreviews
@Composable
private fun RestTimerBarPreview() {
    FerroTheme {
        RestTimerBar(
            restTimer = ActiveRestTimerUi(remainingSeconds = 72, remainingText = "1:12", progress = 0.8f),
            completedRestEventId = 0L,
            showExactAlarmHint = false,
            onOpenExactAlarmSettings = {},
            onAdjustSeconds = {},
            onSkip = {}
        )
    }
}
