package com.davidguijo.ferro.feature.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChartEntry
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroLineChart
import com.davidguijo.ferro.core.designsystem.component.FerroSetType
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.Records
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.localizedInstructions
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.ExerciseChartMetric
import com.davidguijo.ferro.core.domain.model.ExerciseChartPoint
import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.ExerciseRecordDetail
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.RepMaxEntry
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

/** Detalle de un ejercicio de la biblioteca. */
@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

private enum class ExerciseDetailTab(@StringRes val labelRes: Int) {
    INFO(R.string.exercise_tab_info),
    HISTORY(R.string.exercise_tab_history),
    CHARTS(R.string.exercise_tab_charts),
    RECORDS(R.string.exercise_tab_records)
}

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSessionDetail: (String) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExerciseDetailContent(
        uiState = uiState,
        onBack = onBack,
        onOpenSessionDetail = onOpenSessionDetail,
        onSelectMetric = viewModel::onSelectMetric,
        onSelectRange = viewModel::onSelectRange,
        onOpenEditSheet = viewModel::onOpenEditSheet,
        onDismissEditSheet = viewModel::onDismissEditSheet,
        onUpdateCustomExercise = viewModel::onUpdateCustomExercise,
        onRequestToggleArchive = viewModel::onRequestToggleArchive,
        onDismissArchiveDialog = viewModel::onDismissArchiveDialog,
        onConfirmToggleArchive = viewModel::onConfirmToggleArchive,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseDetailContent(
    uiState: ExerciseDetailUiState,
    onBack: () -> Unit,
    onOpenSessionDetail: (String) -> Unit,
    onSelectMetric: (ExerciseChartMetric) -> Unit,
    onSelectRange: (ProgressTimeRange) -> Unit,
    onOpenEditSheet: () -> Unit,
    onDismissEditSheet: () -> Unit,
    onUpdateCustomExercise: (CustomExerciseDraft) -> Unit,
    onRequestToggleArchive: () -> Unit,
    onDismissArchiveDialog: () -> Unit,
    onConfirmToggleArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val exercise = uiState.exercise
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val topBarTitle =
        exercise?.localizedName(uiState.isSpanish) ?: stringResource(R.string.exercise_detail_title)

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = FerroTheme.typography.title,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        }
    ) { innerPadding ->
        if (uiState.isLoaded && exercise == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = stringResource(R.string.exercise_detail_empty_title),
                    description = stringResource(R.string.exercise_detail_empty_description),
                    icon = painterResource(R.drawable.ic_fitness_center)
                )
            }
            return@Scaffold
        }

        if (exercise == null) return@Scaffold

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = colors.bg,
                contentColor = colors.ink,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = colors.brasaText
                    )
                },
                divider = { HorizontalDivider(color = colors.line) }
            ) {
                ExerciseDetailTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    Tab(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = FerroTheme.typography.bodyStrong,
                                color = if (selected) colors.ink else colors.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (ExerciseDetailTab.entries[selectedTabIndex]) {
                ExerciseDetailTab.INFO ->
                    ExerciseInfoTab(
                        exercise = exercise,
                        isSpanish = uiState.isSpanish,
                        onOpenEditSheet = onOpenEditSheet,
                        onRequestToggleArchive = onRequestToggleArchive
                    )

                ExerciseDetailTab.HISTORY ->
                    ExerciseHistoryTab(
                        trackingType = exercise.trackingType,
                        weightUnit = uiState.weightUnit,
                        historyEntries = uiState.historyEntries,
                        recordSetIds = uiState.recordSetIds,
                        onOpenSessionDetail = onOpenSessionDetail
                    )

                ExerciseDetailTab.CHARTS ->
                    ExerciseChartsTab(
                        selectedMetric = uiState.selectedMetric,
                        selectedRange = uiState.selectedRange,
                        weightUnit = uiState.weightUnit,
                        chartPoints = uiState.chartPoints,
                        onSelectMetric = onSelectMetric,
                        onSelectRange = onSelectRange
                    )

                ExerciseDetailTab.RECORDS ->
                    ExerciseRecordsTab(
                        trackingType = exercise.trackingType,
                        weightUnit = uiState.weightUnit,
                        bestRecords = uiState.bestRecords,
                        repMaxTable = uiState.repMaxTable
                    )
            }
        }
    }

    if (uiState.isEditSheetVisible && exercise != null) {
        ExerciseEditorSheet(
            title = stringResource(R.string.exercise_editor_edit_title),
            initialDraft =
                CustomExerciseDraft(
                    name = exercise.localizedName(uiState.isSpanish),
                    primaryMuscle = exercise.primaryMuscle,
                    secondaryMuscles = exercise.secondaryMuscles,
                    equipment = exercise.equipment,
                    trackingType = exercise.trackingType,
                    instructions = exercise.localizedInstructions(uiState.isSpanish)
                ),
            onSave = onUpdateCustomExercise,
            onDismiss = onDismissEditSheet
        )
    }

    if (uiState.isArchiveDialogVisible && exercise != null) {
        val archiving = !exercise.archived
        ConfirmDialog(
            title =
                stringResource(
                    if (archiving) {
                        R.string.exercise_archive_confirm_title
                    } else {
                        R.string.exercise_unarchive_confirm_title
                    }
                ),
            message =
                stringResource(
                    if (archiving) {
                        R.string.exercise_archive_confirm_message
                    } else {
                        R.string.exercise_unarchive_confirm_message
                    }
                ),
            confirmLabel =
                stringResource(
                    if (archiving) {
                        R.string.exercise_archive_confirm_action
                    } else {
                        R.string.exercise_unarchive_confirm_action
                    }
                ),
            destructive = archiving,
            onConfirm = onConfirmToggleArchive,
            onDismiss = onDismissArchiveDialog
        )
    }
}

@Composable
private fun ExerciseInfoTab(
    exercise: Exercise,
    isSpanish: Boolean,
    onOpenEditSheet: () -> Unit,
    onRequestToggleArchive: () -> Unit
) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val instructions = exercise.localizedInstructions(isSpanish)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        FerroCard {
            MetadataRow(
                label = stringResource(R.string.exercise_detail_primary_muscle),
                value = stringResource(exercise.primaryMuscle.labelRes())
            )
            if (exercise.secondaryMuscles.isNotEmpty()) {
                FerroDivider(modifier = Modifier.padding(vertical = spacing.space3))
                val secondaryText =
                    exercise.secondaryMuscles
                        .map { stringResource(it.labelRes()) }
                        .joinToString(", ")
                MetadataRow(
                    label = stringResource(R.string.exercise_detail_secondary_muscles),
                    value = secondaryText
                )
            }
            FerroDivider(modifier = Modifier.padding(vertical = spacing.space3))
            MetadataRow(
                label = stringResource(R.string.exercise_detail_equipment),
                value = stringResource(exercise.equipment.labelRes())
            )
            FerroDivider(modifier = Modifier.padding(vertical = spacing.space3))
            MetadataRow(
                label = stringResource(R.string.exercise_detail_tracking_type),
                value = stringResource(exercise.trackingType.labelRes())
            )
        }

        FerroCard {
            Text(
                text = stringResource(R.string.exercise_detail_instructions).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text =
                    instructions.ifBlank {
                        stringResource(R.string.exercise_detail_no_instructions)
                    },
                style = FerroTheme.typography.body,
                color = if (instructions.isNotBlank()) colors.ink else colors.inkMuted
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            if (exercise.isCustom) {
                FerroButton(
                    text = stringResource(R.string.exercise_detail_edit_cd),
                    onClick = onOpenEditSheet,
                    variant = FerroButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FerroButton(
                text =
                    stringResource(
                        if (exercise.archived) {
                            R.string.exercise_detail_unarchive
                        } else {
                            R.string.exercise_detail_archive
                        }
                    ),
                onClick = onRequestToggleArchive,
                variant =
                    if (exercise.archived) {
                        FerroButtonVariant.Secondary
                    } else {
                        FerroButtonVariant.Danger
                    },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExerciseHistoryTab(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    historyEntries: List<ExerciseHistoryEntry>,
    recordSetIds: Set<String>,
    onOpenSessionDetail: (String) -> Unit
) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val zoneId = remember { ZoneId.systemDefault() }

    if (historyEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.exercise_history_empty_title),
                description = stringResource(R.string.exercise_history_empty_description),
                icon = painterResource(R.drawable.ic_calendar_month)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        items(items = historyEntries, key = { it.workoutExercise.id }) { entry ->
            val dateText =
                Instant.ofEpochMilli(entry.workoutStartedAt).atZone(zoneId).toLocalDate().format(dateFormatter)
            val completedSets = entry.sets.sortedBy { it.position }

            FerroCard(onClick = { onOpenSessionDetail(entry.workoutId) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.workoutName,
                        style = FerroTheme.typography.heading,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateText,
                        style = FerroTheme.typography.caption,
                        color = colors.inkMuted
                    )
                }

                Spacer(modifier = Modifier.height(spacing.space2))
                FerroDivider()
                Spacer(modifier = Modifier.height(spacing.space2))

                var normalIndex = 0
                completedSets.forEachIndexed { idx, set ->
                    if (set.setType != SetType.WARMUP) {
                        normalIndex++
                    }
                    ExerciseHistorySetRow(
                        set = set,
                        normalNumber = normalIndex.coerceAtLeast(1),
                        trackingType = trackingType,
                        weightUnit = weightUnit,
                        isRecord = set.id in recordSetIds,
                        locale = locale
                    )
                    if (idx < completedSets.lastIndex) {
                        Spacer(modifier = Modifier.height(spacing.space1))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseHistorySetRow(
    set: WorkoutSet,
    normalNumber: Int,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    isRecord: Boolean,
    locale: Locale
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val badgeType =
        when (set.setType) {
            SetType.WARMUP -> FerroSetType.Warmup
            SetType.NORMAL -> FerroSetType.Normal
            SetType.FAILURE -> FerroSetType.Failure
            SetType.DROP -> FerroSetType.Drop
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
    ) {
        SetTypeBadge(type = badgeType, setNumber = normalNumber)
        Text(
            text = formatSetSummary(set = set, trackingType = trackingType, weightUnit = weightUnit, locale = locale),
            style = FerroTheme.typography.number,
            color = if (set.completed) colors.ink else colors.inkMuted,
            modifier = Modifier.weight(1f)
        )
        if (set.rpe != null) {
            Text(
                text = stringResource(R.string.session_detail_rpe_badge, formatDecimal(set.rpe, locale)),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
        }
        if (isRecord) {
            Box(
                modifier =
                    Modifier
                        .background(
                            color = colors.brasaSoft,
                            shape = RoundedCornerShape(FerroTheme.radius.sm)
                        )
                        .padding(horizontal = spacing.space2, vertical = spacing.space1)
            ) {
                Text(
                    text = stringResource(R.string.workout_summary_record_badge),
                    style = FerroTheme.typography.label,
                    color = colors.brasaText
                )
            }
        }
    }
}

@Composable
private fun ExerciseChartsTab(
    selectedMetric: ExerciseChartMetric,
    selectedRange: ProgressTimeRange,
    weightUnit: WeightUnit,
    chartPoints: List<ExerciseChartPoint>,
    onSelectMetric: (ExerciseChartMetric) -> Unit,
    onSelectRange: (ProgressTimeRange) -> Unit
) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val shortDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val fullDateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    val unitSuffix =
        stringResource(
            if (weightUnit == WeightUnit.KG) R.string.settings_unit_kg else R.string.settings_unit_lb
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            items(ExerciseChartMetric.entries) { metric ->
                FerroChip(
                    label = stringResource(metric.labelRes()),
                    selected = metric == selectedMetric,
                    onClick = { onSelectMetric(metric) }
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            items(ProgressTimeRange.entries) { range ->
                FerroChip(
                    label = stringResource(range.labelRes()),
                    selected = range == selectedRange,
                    onClick = { onSelectRange(range) }
                )
            }
        }

        if (chartPoints.isEmpty()) {
            FerroCard {
                EmptyState(
                    title = stringResource(R.string.exercise_chart_insufficient_title),
                    description = stringResource(R.string.exercise_chart_insufficient_description),
                    icon = painterResource(R.drawable.ic_bar_chart)
                )
            }
            return
        }

        val chartEntries =
            remember(chartPoints, weightUnit, locale) {
                chartPoints.map { point ->
                    val localDate = Instant.ofEpochMilli(point.timestampMillis).atZone(zoneId).toLocalDate()
                    val displayValue = Units.fromKg(point.valueKg, weightUnit)
                    FerroChartEntry(
                        xLabel = localDate.format(shortDateFormatter),
                        tooltipTitle = localDate.format(fullDateFormatter),
                        value = displayValue,
                        formattedValue = Units.formatWeight(
                            weightKg = point.valueKg,
                            unit = weightUnit,
                            locale = locale,
                            includeUnit = true
                        )
                    )
                }
            }

        val latestPoint = chartPoints.last()
        FerroCard {
            Text(
                text = stringResource(selectedMetric.labelRes()).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text =
                    Units.formatWeight(
                        weightKg = latestPoint.valueKg,
                        unit = weightUnit,
                        locale = locale,
                        includeUnit = true
                    ),
                style = FerroTheme.typography.title,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(spacing.space3))

            FerroLineChart(
                entries = chartEntries,
                yAxisValueFormatter = { axisVal ->
                    "${formatAxisNumber(axisVal, locale)} $unitSuffix"
                }
            )

            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text =
                    stringResource(
                        if (chartPoints.size == 1) {
                            R.string.exercise_chart_single_point_note
                        } else {
                            R.string.exercise_chart_tap_hint
                        }
                    ),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }
    }
}

@Composable
private fun ExerciseRecordsTab(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    bestRecords: List<ExerciseRecordDetail>,
    repMaxTable: Map<Int, RepMaxEntry>
) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    if (bestRecords.isEmpty() && repMaxTable.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.exercise_records_empty_title),
                description = stringResource(R.string.exercise_records_empty_description),
                icon = painterResource(R.drawable.ic_trophy)
            )
        }
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space5)
    ) {
        if (bestRecords.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(
                    text = stringResource(R.string.exercise_records_best_section).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
                FerroCard {
                    bestRecords.forEachIndexed { index, detail ->
                        val dateText =
                            Instant.ofEpochMilli(detail.record.achievedAt)
                                .atZone(zoneId)
                                .toLocalDate()
                                .format(dateFormatter)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(recordTypeLabelRes(detail.record.recordType, trackingType)),
                                    style = FerroTheme.typography.bodyStrong,
                                    color = colors.ink
                                )
                                Text(
                                    text = dateText,
                                    style = FerroTheme.typography.caption,
                                    color = colors.inkMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text =
                                        formatRecordValue(
                                            detail = detail,
                                            trackingType = trackingType,
                                            weightUnit = weightUnit,
                                            locale = locale
                                        ),
                                    style = FerroTheme.typography.number,
                                    color = colors.brasaText
                                )
                                val contextText = formatRecordSetContext(detail, weightUnit, locale)
                                if (contextText != null) {
                                    Text(
                                        text = contextText,
                                        style = FerroTheme.typography.caption,
                                        color = colors.inkMuted
                                    )
                                }
                            }
                        }
                        if (index < bestRecords.lastIndex) {
                            FerroDivider(modifier = Modifier.padding(vertical = spacing.space3))
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(
                text = stringResource(R.string.exercise_records_rep_max_section).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
            FerroCard {
                if (repMaxTable.isEmpty()) {
                    Text(
                        text = stringResource(R.string.exercise_records_rep_max_empty),
                        style = FerroTheme.typography.caption,
                        color = colors.inkMuted
                    )
                } else {
                    for (reps in 1..Records.MAX_EPLEY_REPS) {
                        val entry = repMaxTable[reps]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps),
                                style = FerroTheme.typography.bodyStrong,
                                color = colors.ink,
                                modifier = Modifier.weight(1f)
                            )
                            if (entry != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text =
                                            Units.formatWeight(
                                                weightKg = entry.weightKg,
                                                unit = weightUnit,
                                                locale = locale,
                                                includeUnit = true
                                            ),
                                        style = FerroTheme.typography.number,
                                        color = colors.ink
                                    )
                                    val dateText =
                                        Instant.ofEpochMilli(entry.achievedAt)
                                            .atZone(zoneId)
                                            .toLocalDate()
                                            .format(dateFormatter)
                                    Text(
                                        text = dateText,
                                        style = FerroTheme.typography.caption,
                                        color = colors.inkMuted
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.exercise_records_no_mark),
                                    style = FerroTheme.typography.number,
                                    color = colors.inkSubtle
                                )
                            }
                        }
                        if (reps < Records.MAX_EPLEY_REPS) {
                            FerroDivider(modifier = Modifier.padding(vertical = spacing.space2))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FerroTheme.typography.caption,
            color = FerroTheme.colors.inkMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = FerroTheme.typography.bodyStrong,
            color = FerroTheme.colors.ink
        )
    }
}

@StringRes
private fun ExerciseChartMetric.labelRes(): Int = when (this) {
    ExerciseChartMetric.ESTIMATED_1RM -> R.string.exercise_chart_metric_1rm
    ExerciseChartMetric.MAX_WEIGHT -> R.string.exercise_chart_metric_max_weight
    ExerciseChartMetric.BEST_SET_VOLUME -> R.string.exercise_chart_metric_best_set_volume
    ExerciseChartMetric.SESSION_VOLUME -> R.string.exercise_chart_metric_session_volume
}

@StringRes
private fun ProgressTimeRange.labelRes(): Int = when (this) {
    ProgressTimeRange.ONE_MONTH -> R.string.exercise_chart_range_1m
    ProgressTimeRange.THREE_MONTHS -> R.string.exercise_chart_range_3m
    ProgressTimeRange.ONE_YEAR -> R.string.exercise_chart_range_1y
    ProgressTimeRange.ALL -> R.string.exercise_chart_range_all
}

@StringRes
private fun recordTypeLabelRes(recordType: RecordType, trackingType: TrackingType): Int = when (recordType) {
    RecordType.MAX_WEIGHT ->
        when (trackingType) {
            TrackingType.WEIGHTED_BODYWEIGHT -> R.string.workout_summary_record_max_extra_weight
            TrackingType.ASSISTED_BODYWEIGHT -> R.string.workout_summary_record_min_assistance
            TrackingType.DISTANCE_DURATION -> R.string.workout_summary_record_max_distance
            else -> R.string.workout_summary_record_max_weight
        }

    RecordType.ESTIMATED_1RM -> R.string.workout_summary_record_estimated_1rm

    RecordType.BEST_SET_VOLUME ->
        if (trackingType == TrackingType.DURATION || trackingType == TrackingType.DISTANCE_DURATION) {
            R.string.workout_summary_record_max_duration
        } else {
            R.string.workout_summary_record_best_volume
        }

    RecordType.MAX_REPS_AT_WEIGHT ->
        if (trackingType == TrackingType.BODYWEIGHT_REPS) {
            R.string.workout_summary_record_max_reps
        } else {
            R.string.workout_summary_record_max_reps_at_weight
        }
}

@Composable
private fun formatRecordValue(
    detail: ExerciseRecordDetail,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    locale: Locale
): String {
    val value = detail.record.value
    return when (detail.record.recordType) {
        RecordType.MAX_WEIGHT ->
            if (trackingType == TrackingType.DISTANCE_DURATION) {
                formatDistance(value, locale)
            } else {
                Units.formatWeight(weightKg = value, unit = weightUnit, locale = locale, includeUnit = true)
            }

        RecordType.ESTIMATED_1RM ->
            Units.formatWeight(weightKg = value, unit = weightUnit, locale = locale, includeUnit = true)

        RecordType.BEST_SET_VOLUME ->
            if (trackingType == TrackingType.DURATION || trackingType == TrackingType.DISTANCE_DURATION) {
                formatElapsedDuration(value.roundToInt() * 1_000L)
            } else {
                Units.formatWeight(weightKg = value, unit = weightUnit, locale = locale, includeUnit = true)
            }

        RecordType.MAX_REPS_AT_WEIGHT -> {
            val reps = value.roundToInt()
            pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
        }
    }
}

private fun formatRecordSetContext(detail: ExerciseRecordDetail, weightUnit: WeightUnit, locale: Locale): String? {
    val w = detail.weightKg
    val r = detail.reps
    return if (w != null && r != null && r > 0) {
        "${Units.formatWeight(weightKg = w, unit = weightUnit, locale = locale, includeUnit = true)} × $r"
    } else {
        null
    }
}

@Composable
private fun formatSetSummary(
    set: WorkoutSet,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    locale: Locale
): String = when (trackingType) {
    TrackingType.WEIGHT_REPS,
    TrackingType.WEIGHTED_BODYWEIGHT,
    TrackingType.ASSISTED_BODYWEIGHT -> {
        val w = set.weightKg
        val r = set.reps
        when {
            w != null && r != null ->
                "${Units.formatWeight(weightKg = w, unit = weightUnit, locale = locale, includeUnit = true)} × $r"

            r != null -> pluralStringResource(R.plurals.routine_set_summary_reps_fixed, r, r)

            w != null -> Units.formatWeight(weightKg = w, unit = weightUnit, locale = locale, includeUnit = true)

            else -> stringResource(R.string.exercise_records_no_mark)
        }
    }

    TrackingType.BODYWEIGHT_REPS -> {
        val r = set.reps
        if (r != null) {
            pluralStringResource(R.plurals.routine_set_summary_reps_fixed, r, r)
        } else {
            stringResource(R.string.exercise_records_no_mark)
        }
    }

    TrackingType.DURATION -> {
        val d = set.durationS
        if (d != null) {
            formatElapsedDuration(d * 1_000L)
        } else {
            stringResource(R.string.exercise_records_no_mark)
        }
    }

    TrackingType.DISTANCE_DURATION -> {
        val dist = set.distanceM
        val dur = set.durationS
        when {
            dist != null && dur != null -> "${formatDistance(dist, locale)} · ${formatElapsedDuration(dur * 1_000L)}"
            dist != null -> formatDistance(dist, locale)
            dur != null -> formatElapsedDuration(dur * 1_000L)
            else -> stringResource(R.string.exercise_records_no_mark)
        }
    }
}

private fun formatDistance(distanceM: Double, locale: Locale): String = if (distanceM >= 1_000.0) {
    "${formatDecimal(distanceM / 1_000.0, locale)} km"
} else {
    "${distanceM.roundToInt()} m"
}

private fun formatDecimal(value: Double, locale: Locale): String = if (value % 1.0 == 0.0) {
    value.roundToInt().toString()
} else {
    String.format(locale, "%.1f", value)
}

private fun formatAxisNumber(value: Double, locale: Locale): String = when {
    value >= 10_000.0 -> "${formatDecimal(value / 1_000.0, locale)}k"
    value % 1.0 == 0.0 -> value.roundToInt().toString()
    else -> String.format(locale, "%.1f", value)
}
