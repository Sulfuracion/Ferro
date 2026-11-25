package com.davidguijo.ferro.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** Calendario mensual de sesiones y racha semanal. */
@Serializable
data object CalendarRoute

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenSessionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarContent(
        uiState = uiState,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
        onSelectDate = viewModel::onSelectDate,
        onOpenSessionDetail = onOpenSessionDetail,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarContent(
    uiState: CalendarUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSessionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
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
                actions = {
                    if (!uiState.isCurrentMonth) {
                        TextButton(onClick = onTodayClick) {
                            Text(
                                text = stringResource(R.string.calendar_today),
                                style = FerroTheme.typography.bodyStrong,
                                color = colors.brasaText
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            item(key = "calendar_card") {
                FerroCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.calendar_prev_month_cd),
                            onClick = onPreviousMonth
                        )
                        Text(
                            text = uiState.monthTitle,
                            style = FerroTheme.typography.heading,
                            color = colors.ink
                        )
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = stringResource(R.string.calendar_next_month_cd),
                            onClick = onNextMonth
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.space2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space1)
                    ) {
                        uiState.weekdayLabels.forEach { label ->
                            Text(
                                text = label,
                                style = FerroTheme.typography.label,
                                color = colors.inkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space2))

                    val weeks = uiState.dayCells.chunked(7)
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                        weeks.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.space1)
                            ) {
                                week.forEach { cell ->
                                    CalendarDayCell(
                                        cell = cell,
                                        onSelectDate = onSelectDate,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "selected_day_header") {
                Text(
                    text = uiState.selectedDateLabel.uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
            }

            if (uiState.selectedDaySessions.isEmpty()) {
                item(key = "selected_day_empty") {
                    FerroCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.calendar_day_no_sessions),
                            style = FerroTheme.typography.body,
                            color = colors.inkMuted
                        )
                    }
                }
            } else {
                items(
                    items = uiState.selectedDaySessions,
                    key = { it.workoutId }
                ) { session ->
                    FerroCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenSessionDetail(session.workoutId) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.name,
                                    style = FerroTheme.typography.heading,
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(spacing.space1))
                                val subtitle =
                                    if (session.durationText != null) {
                                        "${session.timeText} · ${session.durationText}"
                                    } else {
                                        session.timeText
                                    }
                                Text(
                                    text = subtitle,
                                    style = FerroTheme.typography.caption,
                                    color = colors.inkMuted
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(FerroTheme.sizes.iconMd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(cell: CalendarDayCellUi, onSelectDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val date = cell.date
    val dayNumber = cell.dayNumber
    if (date == null || dayNumber == null) {
        Spacer(
            modifier =
                modifier
                    .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                    .aspectRatio(1f)
        )
        return
    }

    val backgroundColor =
        when {
            cell.isSelected -> colors.brasa
            cell.hasWorkouts -> colors.brasaSoft
            else -> colors.surface
        }

    val textColor =
        when {
            cell.isSelected -> colors.onBrasa
            cell.hasWorkouts -> colors.brasaText
            else -> colors.ink
        }

    val borderColor =
        when {
            cell.isSelected -> colors.brasa
            cell.isToday -> colors.lineStrong
            else -> colors.surface
        }

    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(FerroTheme.radius.sm))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(FerroTheme.radius.sm)
                )
                .clickable(role = Role.Button) { onSelectDate(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                style = FerroTheme.typography.number,
                color = textColor
            )
            if (cell.hasWorkouts) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .background(
                                color = if (cell.isSelected) colors.onBrasa else colors.brasa,
                                shape = RoundedCornerShape(2.dp)
                            )
                )
            }
        }
    }
}
