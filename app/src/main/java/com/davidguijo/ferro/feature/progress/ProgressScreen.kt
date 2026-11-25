package com.davidguijo.ferro.feature.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChartEntry
import com.davidguijo.ferro.core.designsystem.component.FerroColumnChart
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroErrorState
import com.davidguijo.ferro.core.designsystem.component.FerroLoadingState
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.MuscleRecoveryItem
import com.davidguijo.ferro.core.domain.model.MuscleVolumeComparison
import com.davidguijo.ferro.core.domain.model.ProgressSummaryStats
import com.davidguijo.ferro.core.domain.model.RecoveryStatus
import com.davidguijo.ferro.core.domain.model.WeeklyVolumePoint
import com.davidguijo.ferro.feature.exercises.labelRes
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val RecoveryIndicatorSize = 10.dp

@Composable
fun ProgressScreen(modifier: Modifier = Modifier, viewModel: ProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(uiState = uiState, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgressContent(uiState: ProgressUiState, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        if (!uiState.isLoaded) {
            FerroLoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        if (uiState.hasError) {
            FerroErrorState(
                message = stringResource(R.string.error_generic_load),
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        if (!uiState.hasAnyData) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = stringResource(R.string.progress_empty_title),
                    description = stringResource(R.string.progress_empty_description),
                    icon = painterResource(R.drawable.ic_bar_chart)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space5)
        ) {
            item(key = "summary") {
                ProgressSummarySection(stats = uiState.summaryStats)
            }

            item(key = "weekly_volume") {
                WeeklyVolumeSection(
                    weeklyVolume = uiState.weeklyVolume,
                    hasVolumeData = uiState.hasWeeklyVolumeData,
                    weightUnit = uiState.weightUnit
                )
            }

            item(key = "muscle_sets") {
                MuscleSetsComparisonSection(items = uiState.muscleVolumeComparison)
            }

            item(key = "muscle_recovery") {
                MuscleRecoverySection(items = uiState.muscleRecovery)
            }
        }
    }
}

@Composable
private fun ProgressSummarySection(stats: ProgressSummaryStats) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.progress_summary_section).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            SummaryMetricCard(
                label = stringResource(R.string.progress_workouts_per_week_label),
                value = formatOneDecimal(stats.avgWorkoutsPerWeek, locale),
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = stringResource(R.string.progress_this_week_label),
                value = stats.workoutsThisWeek.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = stringResource(R.string.progress_avg_duration_label),
                value =
                    stats.avgDurationMinutes?.let {
                        stringResource(R.string.progress_avg_duration_value, it)
                    } ?: stringResource(R.string.exercise_records_no_mark),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = value,
            style = FerroTheme.typography.title,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeeklyVolumeSection(weeklyVolume: List<WeeklyVolumePoint>, hasVolumeData: Boolean, weightUnit: WeightUnit) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale
    val shortFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val unitSuffix =
        stringResource(
            if (weightUnit == WeightUnit.KG) R.string.settings_unit_kg else R.string.settings_unit_lb
        )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.progress_weekly_volume_section).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        FerroCard {
            if (!hasVolumeData) {
                EmptyState(
                    title = stringResource(R.string.progress_weekly_volume_empty_title),
                    description = stringResource(R.string.progress_weekly_volume_empty_description),
                    icon = painterResource(R.drawable.ic_bar_chart)
                )
                return@FerroCard
            }

            Text(
                text = stringResource(R.string.progress_weekly_volume_subtitle, unitSuffix),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(spacing.space3))

            val entries =
                remember(weeklyVolume, weightUnit, locale) {
                    weeklyVolume.map { point ->
                        val startText = point.weekStart.format(shortFormatter)
                        val endText = point.weekEnd.format(shortFormatter)
                        FerroChartEntry(
                            xLabel = startText,
                            tooltipTitle = "$startText – $endText",
                            value = Units.fromKg(point.volumeKg, weightUnit),
                            formattedValue =
                                Units.formatWeight(
                                    weightKg = point.volumeKg,
                                    unit = weightUnit,
                                    locale = locale,
                                    roundingStep = 1.0,
                                    includeUnit = true
                                )
                        )
                    }
                }

            FerroColumnChart(
                entries = entries,
                yAxisValueFormatter = { axisVal ->
                    "${formatCompactVolume(axisVal, locale)} $unitSuffix"
                }
            )
        }
    }
}

@Composable
private fun MuscleSetsComparisonSection(items: List<MuscleVolumeComparison>) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors
    val locale = LocalLocale.current.platformLocale

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.progress_muscle_sets_section).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        FerroCard {
            if (items.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.progress_muscle_sets_empty_title),
                    description = stringResource(R.string.progress_muscle_sets_empty_description),
                    icon = painterResource(R.drawable.ic_fitness_center)
                )
                return@FerroCard
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_muscle_sets_header_muscle).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = stringResource(R.string.progress_muscle_sets_header_this_week).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = stringResource(R.string.progress_muscle_sets_header_prev_avg).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.8f)
                )
            }

            FerroDivider(modifier = Modifier.padding(vertical = spacing.space2))

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(item.muscle.labelRes()),
                        style = FerroTheme.typography.bodyStrong,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = formatOneDecimal(item.thisWeekSets, locale),
                        style = FerroTheme.typography.number,
                        color = colors.ink,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = formatOneDecimal(item.previous4WeeksAvgSets, locale),
                        style = FerroTheme.typography.number,
                        color = colors.inkMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                if (index < items.lastIndex) {
                    FerroDivider(modifier = Modifier.padding(vertical = spacing.space2))
                }
            }

            Spacer(modifier = Modifier.height(spacing.space3))
            Text(
                text = stringResource(R.string.progress_muscle_sets_rule),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }
    }
}

@Composable
private fun MuscleRecoverySection(items: List<MuscleRecoveryItem>) {
    val spacing = FerroTheme.spacing
    val colors = FerroTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.progress_recovery_section).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        FerroCard {
            if (items.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.progress_recovery_empty_title),
                    description = stringResource(R.string.progress_recovery_empty_description),
                    icon = painterResource(R.drawable.ic_timer)
                )
                return@FerroCard
            }

            items.forEachIndexed { index, item ->
                val statusColor =
                    when (item.status) {
                        RecoveryStatus.RECENT -> colors.danger
                        RecoveryStatus.RECOVERING_48_72H -> colors.warning
                        RecoveryStatus.READY -> colors.success
                    }
                val statusLabel =
                    stringResource(
                        when (item.status) {
                            RecoveryStatus.RECENT -> R.string.progress_recovery_status_recent
                            RecoveryStatus.RECOVERING_48_72H -> R.string.progress_recovery_status_moderate
                            RecoveryStatus.READY -> R.string.progress_recovery_status_ready
                        }
                    )
                val daysText =
                    if (item.daysSinceLastTrained == 0) {
                        stringResource(R.string.progress_recovery_today)
                    } else {
                        pluralStringResource(
                            R.plurals.progress_recovery_days_ago,
                            item.daysSinceLastTrained,
                            item.daysSinceLastTrained
                        )
                    }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3)
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(RecoveryIndicatorSize)
                                .background(
                                    color = statusColor,
                                    shape = RoundedCornerShape(FerroTheme.radius.sm)
                                )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.muscle.labelRes()),
                            style = FerroTheme.typography.bodyStrong,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = daysText,
                            style = FerroTheme.typography.caption,
                            color = colors.inkMuted
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = FerroTheme.typography.caption,
                        color = statusColor
                    )
                }

                if (index < items.lastIndex) {
                    FerroDivider(modifier = Modifier.padding(vertical = spacing.space2))
                }
            }

            Spacer(modifier = Modifier.height(spacing.space3))
            Text(
                text = stringResource(R.string.progress_recovery_disclaimer),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }
    }
}

private fun formatOneDecimal(value: Double, locale: Locale): String = if (value % 1.0 == 0.0) {
    value.roundToInt().toString()
} else {
    String.format(locale, "%.1f", value)
}

private fun formatCompactVolume(value: Double, locale: Locale): String = when {
    value >= 10_000.0 -> "${formatOneDecimal(value / 1_000.0, locale)}k"
    else -> value.roundToInt().toString()
}
