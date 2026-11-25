package com.davidguijo.ferro.feature.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroLoadingState
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroHaptics
import com.davidguijo.ferro.core.designsystem.theme.FerroMotion
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.Records
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.feature.routines.formatDistanceMeters
import com.davidguijo.ferro.feature.routines.supersetAccentColor
import com.davidguijo.ferro.feature.routines.toFerroSetType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Resumen de entreno al finalizar una sesión. */
@Serializable
data class WorkoutSummaryRoute(val workoutId: String)

@Composable
fun WorkoutSummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutSummaryContent(
        uiState = uiState,
        onBack = onBack,
        onUpdateRoutine = viewModel::onUpdateRoutineWithWorkoutValues,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutSummaryContent(
    uiState: WorkoutSummaryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateRoutine: () -> Unit = {}
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val reduceMotion = FerroTheme.reduceMotion

    var hasCelebratedRecords by rememberSaveable(uiState.workoutId) { mutableStateOf(false) }
    val recordCelebrationProgress = remember { Animatable(0f) }

    LaunchedEffect(uiState.brokenRecords.size, uiState.isLoaded) {
        if (uiState.isLoaded && uiState.brokenRecords.isNotEmpty()) {
            if (!hasCelebratedRecords) {
                hasCelebratedRecords = true
                FerroHaptics.confirm(view, haptic)
                recordCelebrationProgress.snapTo(0f)
                launch {
                    recordCelebrationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = FerroMotion.stateSpec()
                    )
                }
            } else {
                recordCelebrationProgress.snapTo(1f)
            }
        }
    }

    val slideOffsetPx = with(density) { PR_SLIDE_OFFSET_DP.dp.toPx() }
    val locale = if (uiState.isSpanish) Locale.forLanguageTag("es-ES") else Locale.US

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_summary_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                navigationIcon = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        },
        bottomBar = {
            if (uiState.isLoaded && uiState.workoutName.isNotEmpty()) {
                Surface(
                    color = colors.surface,
                    shadowElevation = FerroTheme.elevation.large
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                    ) {
                        FerroDivider()
                        FerroButton(
                            text = stringResource(R.string.workout_summary_done),
                            onClick = onBack,
                            variant = FerroButtonVariant.Primary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.space4)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoaded) {
            FerroLoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (uiState.workoutName.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = stringResource(R.string.workout_summary_empty_title),
                    description = stringResource(R.string.workout_summary_empty_description),
                    icon = painterResource(R.drawable.ic_trophy)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.space4),
            contentPadding = PaddingValues(top = spacing.space2, bottom = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            item(key = "summary_header") {
                FerroCard {
                    Text(
                        text = uiState.workoutName.uppercase(),
                        style = FerroTheme.typography.display,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(spacing.space4))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryStat(
                            label = stringResource(R.string.workout_summary_duration),
                            value = uiState.durationText,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStat(
                            label = stringResource(R.string.workout_summary_volume),
                            value = uiState.volumeText,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStat(
                            label = stringResource(R.string.workout_summary_sets),
                            value = uiState.completedSetsCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (uiState.routineComparison != null) {
                item(key = "routine_comparison") {
                    RoutineComparisonCard(
                        comparison = uiState.routineComparison,
                        weightUnit = uiState.weightUnit,
                        locale = locale
                    )
                }
            }

            if (uiState.canUpdateRoutineValues || uiState.routineUpdated) {
                item(key = "routine_update_offer") {
                    RoutineUpdateCard(
                        routineName = uiState.routineName.ifBlank { uiState.workoutName },
                        isUpdated = uiState.routineUpdated,
                        onUpdateRoutine = onUpdateRoutine
                    )
                }
            }

            if (uiState.brokenRecords.isNotEmpty()) {
                item(key = "broken_records_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trophy),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconSm),
                            tint = colors.brasaText
                        )
                        Text(
                            text = stringResource(R.string.workout_summary_records_section).uppercase(),
                            style = FerroTheme.typography.label,
                            color = colors.brasaText
                        )
                    }
                }
                items(
                    items = uiState.brokenRecords,
                    key = { it.id }
                ) { record ->
                    BrokenRecordCard(
                        record = record,
                        weightUnit = uiState.weightUnit,
                        isSpanish = uiState.isSpanish,
                        locale = locale,
                        celebrationProgress = recordCelebrationProgress.value,
                        reduceMotion = reduceMotion,
                        slideOffsetPx = slideOffsetPx
                    )
                }
            }

            if (uiState.exercises.isNotEmpty()) {
                item(key = "exercises_header") {
                    Text(
                        text = stringResource(R.string.workout_summary_exercises_section).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted
                    )
                }
                items(
                    items = uiState.exercises,
                    key = { it.id }
                ) { exercise ->
                    SummaryExerciseCard(
                        exercise = exercise,
                        weightUnit = uiState.weightUnit,
                        isSpanish = uiState.isSpanish,
                        locale = locale
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = FerroTheme.colors.inkMuted
        )
        Text(
            text = value,
            style = FerroTheme.typography.number,
            color = FerroTheme.colors.ink
        )
    }
}

@Composable
private fun RoutineComparisonCard(comparison: WorkoutSummaryComparisonUi, weightUnit: WeightUnit, locale: Locale) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val equalLabel = stringResource(R.string.workout_summary_comparison_equal)

    val durationDeltaText =
        when {
            abs(comparison.durationDeltaMillis) < 1_000L -> equalLabel

            comparison.durationDeltaMillis > 0L ->
                "+${formatElapsedDuration(comparison.durationDeltaMillis)}"

            else -> "−${formatElapsedDuration(abs(comparison.durationDeltaMillis))}"
        }

    val volumeDeltaText =
        when {
            abs(comparison.volumeDeltaKg) <= Records.EPSILON -> equalLabel

            comparison.volumeDeltaKg > 0.0 ->
                "+${
                    Units.formatWeight(
                        weightKg = comparison.volumeDeltaKg,
                        unit = weightUnit,
                        locale = locale,
                        roundingStep = null,
                        includeUnit = true
                    )
                }"

            else ->
                "−${
                    Units.formatWeight(
                        weightKg = abs(comparison.volumeDeltaKg),
                        unit = weightUnit,
                        locale = locale,
                        roundingStep = null,
                        includeUnit = true
                    )
                }"
        }

    val setsDeltaText =
        when {
            comparison.completedSetsDelta == 0 -> equalLabel
            comparison.completedSetsDelta > 0 -> "+${comparison.completedSetsDelta}"
            else -> "−${abs(comparison.completedSetsDelta)}"
        }

    FerroCard {
        Text(
            text = stringResource(R.string.workout_summary_comparison_section).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ComparisonDeltaCell(
                label = stringResource(R.string.workout_summary_duration),
                deltaText = durationDeltaText,
                highlightPositive = false,
                isPositive = comparison.durationDeltaMillis > 0L,
                modifier = Modifier.weight(1f)
            )
            ComparisonDeltaCell(
                label = stringResource(R.string.workout_summary_volume),
                deltaText = volumeDeltaText,
                highlightPositive = true,
                isPositive = comparison.volumeDeltaKg > Records.EPSILON,
                modifier = Modifier.weight(1f)
            )
            ComparisonDeltaCell(
                label = stringResource(R.string.workout_summary_sets),
                deltaText = setsDeltaText,
                highlightPositive = true,
                isPositive = comparison.completedSetsDelta > 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ComparisonDeltaCell(
    label: String,
    deltaText: String,
    highlightPositive: Boolean,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        Text(
            text = deltaText,
            style = FerroTheme.typography.number,
            color = if (highlightPositive && isPositive) colors.brasaText else colors.ink
        )
    }
}

@Composable
private fun RoutineUpdateCard(routineName: String, isUpdated: Boolean, onUpdateRoutine: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard {
        Text(
            text = stringResource(R.string.workout_summary_update_routine_title).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        if (isUpdated) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.brasaText
                )
                Text(
                    text = stringResource(R.string.workout_summary_update_routine_done),
                    style = FerroTheme.typography.bodyStrong,
                    color = colors.brasaText
                )
            }
        } else {
            Text(
                text = stringResource(R.string.workout_summary_update_routine_description, routineName),
                style = FerroTheme.typography.body,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(spacing.space3))
            FerroButton(
                text = stringResource(R.string.workout_summary_update_routine_action),
                onClick = onUpdateRoutine,
                variant = FerroButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BrokenRecordCard(
    record: WorkoutSummaryRecordUi,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    locale: Locale,
    celebrationProgress: Float,
    reduceMotion: Boolean,
    slideOffsetPx: Float
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val shape = RoundedCornerShape(FerroTheme.radius.md)

    val categoryLabel = recordCategoryLabel(record.recordType, record.trackingType)
    val valueText =
        formatRecordHeadlineValue(
            record = record,
            weightUnit = weightUnit,
            isSpanish = isSpanish,
            locale = locale
        )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.brasaSoft, shape)
                .border(1.dp, colors.brasa, shape)
                .padding(spacing.space4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(colors.brasa, RoundedCornerShape(FerroTheme.radius.sm)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.onBrasa
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.exerciseName} · $categoryLabel".uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.brasaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = stringResource(R.string.workout_summary_new_record_headline, valueText),
                    style = FerroTheme.typography.number,
                    color = colors.ink,
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = celebrationProgress.coerceIn(0f, 1f)
                            translationY =
                                if (reduceMotion) {
                                    0f
                                } else {
                                    (1f - celebrationProgress.coerceIn(0f, 1f)) * slideOffsetPx
                                }
                        }
                )
            }
        }
    }
}

@Composable
private fun SummaryExerciseCard(
    exercise: WorkoutSummaryExerciseUi,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    locale: Locale
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (exercise.supersetGroup != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.routine_superset_badge_format,
                                exercise.supersetGroup
                            ).uppercase(),
                        style = FerroTheme.typography.label,
                        color = supersetAccentColor(exercise.supersetGroup)
                    )
                    Spacer(modifier = Modifier.height(spacing.space1))
                }
                Text(
                    text = exercise.name,
                    style = FerroTheme.typography.heading,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(spacing.space2))
            Text(
                text =
                    pluralStringResource(
                        R.plurals.routine_sets_count,
                        exercise.completedSetsCount,
                        exercise.completedSetsCount
                    ),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }

        if (exercise.sets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.space3))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                exercise.sets.forEach { set ->
                    SummarySetRow(
                        set = set,
                        trackingType = exercise.trackingType,
                        weightUnit = weightUnit,
                        isSpanish = isSpanish,
                        locale = locale
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySetRow(
    set: WorkoutSummarySetUi,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    locale: Locale
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val rowShape = RoundedCornerShape(FerroTheme.radius.md)

    val rowModifier =
        if (set.isRecord) {
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = FerroTheme.sizes.rowHeight)
                .background(colors.brasaSoft, rowShape)
                .border(1.dp, colors.brasa, rowShape)
                .padding(horizontal = spacing.space2, vertical = spacing.space1)
        } else {
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = FerroTheme.sizes.rowHeight)
                .padding(horizontal = spacing.space1, vertical = spacing.space1)
        }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
    ) {
        SetTypeBadge(
            type = set.setType.toFerroSetType(),
            setNumber = set.normalSetNumber
        )

        Text(
            text =
                formatSummarySetPerformance(
                    set = set,
                    trackingType = trackingType,
                    weightUnit = weightUnit,
                    isSpanish = isSpanish,
                    locale = locale
                ),
            style = FerroTheme.typography.number,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )

        if (set.rpe != null) {
            val rpeFormatter =
                remember(locale) {
                    DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale)).apply {
                        isGroupingUsed = false
                    }
                }
            Text(
                text = stringResource(R.string.workout_summary_set_rpe_format, rpeFormatter.format(set.rpe)),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }

        if (set.isRecord) {
            PrBadge()
        }
    }
}

@Composable
private fun PrBadge() {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    Row(
        modifier =
            Modifier
                .background(
                    color = colors.brasa,
                    shape = RoundedCornerShape(FerroTheme.radius.sm)
                )
                .padding(horizontal = spacing.space2, vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space1)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            modifier = Modifier.size(FerroTheme.sizes.iconSm),
            tint = colors.onBrasa
        )
        Text(
            text = stringResource(R.string.workout_summary_record_badge),
            style = FerroTheme.typography.label,
            color = colors.onBrasa
        )
    }
}

@Composable
internal fun recordCategoryLabel(recordType: RecordType, trackingType: TrackingType): String = when (recordType) {
    RecordType.MAX_WEIGHT ->
        when (trackingType) {
            TrackingType.WEIGHTED_BODYWEIGHT ->
                stringResource(R.string.workout_summary_record_max_extra_weight)

            TrackingType.ASSISTED_BODYWEIGHT ->
                stringResource(R.string.workout_summary_record_min_assistance)

            TrackingType.DISTANCE_DURATION ->
                stringResource(R.string.workout_summary_record_max_distance)

            else -> stringResource(R.string.workout_summary_record_max_weight)
        }

    RecordType.ESTIMATED_1RM -> stringResource(R.string.workout_summary_record_estimated_1rm)

    RecordType.BEST_SET_VOLUME ->
        when (trackingType) {
            TrackingType.DURATION,
            TrackingType.DISTANCE_DURATION ->
                stringResource(R.string.workout_summary_record_max_duration)

            else -> stringResource(R.string.workout_summary_record_best_volume)
        }

    RecordType.MAX_REPS_AT_WEIGHT ->
        when (trackingType) {
            TrackingType.BODYWEIGHT_REPS ->
                stringResource(R.string.workout_summary_record_max_reps)

            else -> stringResource(R.string.workout_summary_record_max_reps_at_weight)
        }
}

@Composable
private fun formatRecordHeadlineValue(
    record: WorkoutSummaryRecordUi,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    locale: Locale
): String {
    val formattedSetWeight =
        record.setWeightKg?.let { kg ->
            Units.formatWeight(
                weightKg = kg,
                unit = weightUnit,
                locale = locale,
                includeUnit = true
            )
        }
    return when (record.recordType) {
        RecordType.MAX_WEIGHT ->
            when (record.trackingType) {
                TrackingType.DISTANCE_DURATION ->
                    stringResource(
                        R.string.workout_summary_set_distance_m,
                        formatDistanceMeters(record.value, isSpanish)
                    )

                TrackingType.WEIGHTED_BODYWEIGHT ->
                    if (formattedSetWeight != null && record.setReps != null) {
                        stringResource(
                            R.string.workout_summary_set_extra_weight_reps,
                            formattedSetWeight,
                            record.setReps
                        )
                    } else {
                        "+${Units.formatWeight(record.value, weightUnit, locale, includeUnit = true)}"
                    }

                TrackingType.ASSISTED_BODYWEIGHT ->
                    if (formattedSetWeight != null && record.setReps != null) {
                        stringResource(
                            R.string.workout_summary_set_assist_weight_reps,
                            formattedSetWeight,
                            record.setReps
                        )
                    } else {
                        "-${Units.formatWeight(record.value, weightUnit, locale, includeUnit = true)}"
                    }

                else ->
                    if (formattedSetWeight != null && record.setReps != null) {
                        stringResource(
                            R.string.workout_summary_set_weight_reps,
                            formattedSetWeight,
                            record.setReps
                        )
                    } else {
                        Units.formatWeight(record.value, weightUnit, locale, includeUnit = true)
                    }
            }

        RecordType.ESTIMATED_1RM ->
            Units.formatWeight(
                weightKg = record.value,
                unit = weightUnit,
                locale = locale,
                includeUnit = true
            )

        RecordType.BEST_SET_VOLUME ->
            when (record.trackingType) {
                TrackingType.DURATION,
                TrackingType.DISTANCE_DURATION ->
                    stringResource(R.string.workout_summary_set_duration_s, record.value.roundToInt())

                else ->
                    Units.formatWeight(
                        weightKg = record.value,
                        unit = weightUnit,
                        locale = locale,
                        roundingStep = null,
                        includeUnit = true
                    )
            }

        RecordType.MAX_REPS_AT_WEIGHT -> {
            val reps = record.value.roundToInt()
            when (record.trackingType) {
                TrackingType.WEIGHT_REPS ->
                    if (formattedSetWeight != null) {
                        stringResource(R.string.workout_summary_set_weight_reps, formattedSetWeight, reps)
                    } else {
                        pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
                    }

                TrackingType.WEIGHTED_BODYWEIGHT ->
                    if (formattedSetWeight != null) {
                        stringResource(R.string.workout_summary_set_extra_weight_reps, formattedSetWeight, reps)
                    } else {
                        pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
                    }

                TrackingType.ASSISTED_BODYWEIGHT ->
                    if (formattedSetWeight != null) {
                        stringResource(R.string.workout_summary_set_assist_weight_reps, formattedSetWeight, reps)
                    } else {
                        pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
                    }

                else -> pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
            }
        }
    }
}

@Composable
private fun formatSummarySetPerformance(
    set: WorkoutSummarySetUi,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    locale: Locale
): String {
    val weightText =
        set.weightKg?.let { kg ->
            Units.formatWeight(
                weightKg = kg,
                unit = weightUnit,
                locale = locale,
                includeUnit = true
            )
        }
    val reps = set.reps
    return when (trackingType) {
        TrackingType.WEIGHT_REPS ->
            when {
                weightText != null && reps != null ->
                    stringResource(R.string.workout_summary_set_weight_reps, weightText, reps)

                weightText != null -> weightText

                reps != null -> pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)

                else -> "—"
            }

        TrackingType.WEIGHTED_BODYWEIGHT ->
            when {
                weightText != null && reps != null ->
                    stringResource(R.string.workout_summary_set_extra_weight_reps, weightText, reps)

                reps != null -> pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)

                else -> "—"
            }

        TrackingType.ASSISTED_BODYWEIGHT ->
            when {
                weightText != null && reps != null ->
                    stringResource(R.string.workout_summary_set_assist_weight_reps, weightText, reps)

                reps != null -> pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)

                else -> "—"
            }

        TrackingType.BODYWEIGHT_REPS ->
            if (reps != null) {
                pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
            } else {
                "—"
            }

        TrackingType.DURATION ->
            if (set.durationS != null) {
                stringResource(R.string.workout_summary_set_duration_s, set.durationS)
            } else {
                "—"
            }

        TrackingType.DISTANCE_DURATION ->
            when {
                set.distanceM != null && set.durationS != null ->
                    stringResource(
                        R.string.workout_summary_set_distance_duration,
                        formatDistanceMeters(set.distanceM, isSpanish),
                        set.durationS
                    )

                set.distanceM != null ->
                    stringResource(
                        R.string.workout_summary_set_distance_m,
                        formatDistanceMeters(set.distanceM, isSpanish)
                    )

                set.durationS != null ->
                    stringResource(R.string.workout_summary_set_duration_s, set.durationS)

                else -> "—"
            }
    }
}

private const val PR_CELEBRATION_ANIM_MS = 200
private const val PR_SLIDE_OFFSET_DP = 10
