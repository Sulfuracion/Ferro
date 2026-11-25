package com.davidguijo.ferro.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroHaptics
import com.davidguijo.ferro.core.designsystem.theme.FerroMotion
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.feature.routines.formatRestMinSec
import kotlinx.serialization.Serializable

/** Flujo breve de primer arranque (3 pasos: idioma, unidad, descanso), saltable. */
@Serializable
data object OnboardingRoute

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinished()
        }
    }

    OnboardingContent(
        uiState = uiState,
        onLanguageSelected = viewModel::onLanguageSelected,
        onWeightUnitSelected = viewModel::onWeightUnitSelected,
        onDefaultRestSelected = viewModel::onDefaultRestSelected,
        onDecreaseDefaultRest = viewModel::onDecreaseDefaultRest,
        onIncreaseDefaultRest = viewModel::onIncreaseDefaultRest,
        onNextStep = viewModel::onNextStep,
        onPreviousStep = viewModel::onPreviousStep,
        onSkip = viewModel::onSkip,
        modifier = modifier
    )
}

@Composable
internal fun OnboardingContent(
    uiState: OnboardingUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onDefaultRestSelected: (Int) -> Unit,
    onDecreaseDefaultRest: () -> Unit,
    onIncreaseDefaultRest: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val reduceMotion = FerroTheme.reduceMotion
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.space4, vertical = spacing.space4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(
                                    color = colors.iron,
                                    shape = RoundedCornerShape(FerroTheme.radius.sm)
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_splash_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colors.brasa
                        )
                    }
                    Text(
                        text =
                            stringResource(
                                R.string.onboarding_step_indicator,
                                uiState.currentStep + 1,
                                OnboardingViewModel.TOTAL_STEPS
                            ).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted
                    )
                }

                TextButton(
                    onClick = {
                        FerroHaptics.tick(haptic)
                        onSkip()
                    },
                    modifier = Modifier.height(FerroTheme.sizes.touchMin)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = FerroTheme.typography.bodyStrong,
                        color = colors.brasaText
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                repeat(OnboardingViewModel.TOTAL_STEPS) { index ->
                    val active = index <= uiState.currentStep
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (active) colors.brasa else colors.line,
                                    shape = RoundedCornerShape(FerroTheme.radius.sm)
                                )
                    )
                }
            }

            if (uiState.hasError) {
                Spacer(modifier = Modifier.height(spacing.space3))
                FerroCard {
                    Text(
                        text = stringResource(R.string.error_generic_save),
                        style = FerroTheme.typography.body,
                        color = colors.danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.space6))

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(FerroMotion.stateSpec()) togetherWith fadeOut(FerroMotion.stateSpec())
                        } else {
                            val forward = targetState >= initialState
                            val enter =
                                slideInHorizontally(FerroMotion.stateSpec()) { width ->
                                    if (forward) width / 4 else -width / 4
                                } + fadeIn(FerroMotion.stateSpec())
                            val exit =
                                slideOutHorizontally(FerroMotion.stateSpec()) { width ->
                                    if (forward) -width / 4 else width / 4
                                } + fadeOut(FerroMotion.stateSpec())
                            enter togetherWith exit
                        }
                    },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        0 ->
                            OnboardingLanguageStep(
                                selected = uiState.language,
                                onSelect = onLanguageSelected
                            )

                        1 ->
                            OnboardingUnitStep(
                                selected = uiState.weightUnit,
                                onSelect = onWeightUnitSelected
                            )

                        else ->
                            OnboardingRestStep(
                                restSeconds = uiState.defaultRestSeconds,
                                onSelectPreset = onDefaultRestSelected,
                                onDecrease = onDecreaseDefaultRest,
                                onIncrease = onIncreaseDefaultRest
                            )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                if (uiState.currentStep > 0) {
                    FerroButton(
                        text = stringResource(R.string.onboarding_back),
                        onClick = onPreviousStep,
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                val isLastStep = uiState.currentStep == OnboardingViewModel.TOTAL_STEPS - 1
                FerroButton(
                    text =
                        stringResource(
                            if (isLastStep) R.string.onboarding_finish else R.string.onboarding_next
                        ),
                    onClick = {
                        if (isLastStep) {
                            FerroHaptics.confirm(view, haptic)
                        }
                        onNextStep()
                    },
                    variant = FerroButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingLanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    Column {
        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = FerroTheme.typography.title,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.onboarding_language_subtitle),
            style = FerroTheme.typography.body,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space5))
        FerroCard {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                    AppLanguage.ES to stringResource(R.string.settings_language_es),
                    AppLanguage.EN to stringResource(R.string.settings_language_en)
                ).forEach { (option, label) ->
                    FerroChip(
                        label = label,
                        selected = option == selected,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingUnitStep(selected: WeightUnit, onSelect: (WeightUnit) -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    Column {
        Text(
            text = stringResource(R.string.onboarding_unit_title),
            style = FerroTheme.typography.title,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.onboarding_unit_subtitle),
            style = FerroTheme.typography.body,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space5))
        FerroCard {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                listOf(
                    WeightUnit.KG to stringResource(R.string.settings_unit_kg),
                    WeightUnit.LB to stringResource(R.string.settings_unit_lb)
                ).forEach { (option, label) ->
                    FerroChip(
                        label = label,
                        selected = option == selected,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingRestStep(
    restSeconds: Int,
    onSelectPreset: (Int) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val restFormatted = formatRestMinSec(restSeconds)
    val restValueDescription = pluralStringResource(R.plurals.rest_seconds, restSeconds, restSeconds)

    Column {
        Text(
            text = stringResource(R.string.onboarding_rest_title),
            style = FerroTheme.typography.title,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.onboarding_rest_subtitle),
            style = FerroTheme.typography.body,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space5))
        FerroCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.settings_rest_decrease_cd),
                    onClick = onDecrease,
                    enabled = restSeconds > Settings.MIN_REST_SECONDS,
                    containerColor = colors.surfaceRaised
                )
                Text(
                    text = restFormatted,
                    style = FerroTheme.typography.numberXl,
                    color = colors.ink,
                    modifier =
                        Modifier.semantics {
                            contentDescription = restValueDescription
                        }
                )
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.settings_rest_increase_cd),
                    onClick = onIncrease,
                    enabled = restSeconds < Settings.MAX_REST_SECONDS,
                    containerColor = colors.surfaceRaised
                )
            }
            Spacer(modifier = Modifier.height(spacing.space4))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                listOf(60, 90, 120, 180).forEach { seconds ->
                    FerroChip(
                        label = formatRestMinSec(seconds),
                        selected = restSeconds == seconds,
                        onClick = { onSelectPreset(seconds) }
                    )
                }
            }
        }
    }
}

@FerroPreviews
@Composable
private fun OnboardingPreview() {
    FerroTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentStep = 2),
            onLanguageSelected = {},
            onWeightUnitSelected = {},
            onDefaultRestSelected = {},
            onDecreaseDefaultRest = {},
            onIncreaseDefaultRest = {},
            onNextStep = {},
            onPreviousStep = {},
            onSkip = {}
        )
    }
}
