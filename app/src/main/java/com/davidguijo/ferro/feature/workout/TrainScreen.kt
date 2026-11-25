package com.davidguijo.ferro.feature.workout

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import kotlinx.serialization.Serializable

/** Pestaña «Entrenar». */
@Serializable
data object TrainRoute

@Composable
fun TrainScreen(
    onOpenActiveWorkout: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultWorkoutName = stringResource(R.string.active_workout_default_name)

    LaunchedEffect(uiState.shouldOpenActiveWorkout) {
        if (uiState.shouldOpenActiveWorkout) {
            viewModel.onNavigatedToActiveWorkout()
            onOpenActiveWorkout()
        }
    }

    TrainContent(
        uiState = uiState,
        onStartEmptyWorkout = { viewModel.onStartEmptyWorkoutClick(defaultWorkoutName) },
        onStartRoutine = viewModel::onStartRoutineClick,
        onResumeActiveWorkout = viewModel::onResumeActiveWorkout,
        onDiscardAndStartNew = viewModel::onDiscardAndStartPending,
        onDismissConflict = viewModel::onDismissConflictDialog,
        onOpenHistory = onOpenHistory,
        onOpenCalendar = onOpenCalendar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainContent(
    uiState: TrainUiState,
    onStartEmptyWorkout: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onResumeActiveWorkout: () -> Unit,
    onDiscardAndStartNew: () -> Unit,
    onDismissConflict: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
    onOpenCalendar: () -> Unit = {}
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.train_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                actions = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_calendar_month),
                        contentDescription = stringResource(R.string.history_open_calendar_cd),
                        onClick = onOpenCalendar
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_timer),
                        contentDescription = stringResource(R.string.history_open_history_cd),
                        onClick = onOpenHistory
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.space4)
        ) {
            Spacer(modifier = Modifier.height(spacing.space2))
            WeeklyStreakCard(
                consecutiveWeeks = uiState.consecutiveWeeks,
                workoutsThisWeek = uiState.workoutsThisWeek,
                onClick = onOpenHistory
            )
            Spacer(modifier = Modifier.height(spacing.space4))
            FerroButton(
                text = stringResource(R.string.train_start_empty),
                onClick = onStartEmptyWorkout,
                variant = FerroButtonVariant.Primary,
                leadingIcon = painterResource(R.drawable.ic_add),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.space6))
            Text(
                text = stringResource(R.string.train_routines_section).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(spacing.space2))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.space6),
                verticalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                if (uiState.routines.isEmpty()) {
                    item(key = "empty_routines") {
                        EmptyState(
                            title = stringResource(R.string.train_routines_empty_title),
                            description = stringResource(R.string.train_routines_empty_description),
                            icon = painterResource(R.drawable.ic_calendar_month)
                        )
                    }
                } else {
                    items(
                        items = uiState.routines,
                        key = { it.id }
                    ) { routine ->
                        TrainRoutineCard(
                            routine = routine,
                            onClick = { onStartRoutine(routine.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.pendingConflict != null) {
        val activeName =
            uiState.activeWorkoutName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.active_workout_default_name)
        ActiveWorkoutConflictDialog(
            title = stringResource(R.string.train_resume_active_title),
            message = stringResource(R.string.train_resume_active_message, activeName),
            resumeLabel = stringResource(R.string.train_resume_active_resume),
            discardLabel = stringResource(R.string.train_resume_active_discard_new),
            onResume = onResumeActiveWorkout,
            onDiscardAndStartNew = onDiscardAndStartNew,
            onDismiss = onDismissConflict
        )
    }
}

@Composable
private fun TrainRoutineCard(routine: TrainRoutineItemUi, onClick: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val exercisesCountText =
        pluralStringResource(
            R.plurals.routine_exercises_count,
            routine.exerciseCount,
            routine.exerciseCount
        )
    val setsCountText =
        pluralStringResource(
            R.plurals.routine_sets_count,
            routine.totalSetCount,
            routine.totalSetCount
        )

    FerroCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = FerroTheme.typography.heading,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$exercisesCountText · $setsCountText",
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
                if (routine.exerciseNamesSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(spacing.space1))
                    Text(
                        text = routine.exerciseNamesSummary,
                        style = FerroTheme.typography.caption,
                        color = colors.inkSubtle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.train_routine_start_cd, routine.name),
                modifier = Modifier.size(FerroTheme.sizes.iconMd),
                tint = colors.brasaText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveWorkoutConflictDialog(
    title: String,
    message: String,
    resumeLabel: String,
    discardLabel: String,
    onResume: () -> Unit,
    onDiscardAndStartNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(FerroTheme.radius.lg),
            color = colors.surface,
            contentColor = colors.ink,
            shadowElevation = FerroTheme.elevation.large
        ) {
            Column(modifier = Modifier.padding(spacing.space5)) {
                Text(
                    text = title,
                    style = FerroTheme.typography.heading,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(spacing.space2))
                Text(
                    text = message,
                    style = FerroTheme.typography.body,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.height(spacing.space5))
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    FerroButton(
                        text = resumeLabel,
                        onClick = onResume,
                        variant = FerroButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FerroButton(
                        text = discardLabel,
                        onClick = onDiscardAndStartNew,
                        variant = FerroButtonVariant.Danger,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FerroButton(
                        text = stringResource(R.string.dialog_cancel),
                        onClick = onDismiss,
                        variant = FerroButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStreakCard(consecutiveWeeks: Int, workoutsThisWeek: Int, onClick: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.train_streak_title).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = consecutiveWeeks.toString(),
                    style = FerroTheme.typography.numberXl,
                    color = if (consecutiveWeeks > 0) colors.brasaText else colors.ink
                )
                Text(
                    text = pluralStringResource(R.plurals.train_streak_weeks, consecutiveWeeks, consecutiveWeeks),
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.train_streak_this_week_label).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = workoutsThisWeek.toString(),
                    style = FerroTheme.typography.numberXl,
                    color = colors.ink
                )
                Text(
                    text = pluralStringResource(R.plurals.train_workouts_this_week, workoutsThisWeek, workoutsThisWeek),
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.train_history_action),
                tint = colors.inkMuted,
                modifier = Modifier.size(FerroTheme.sizes.iconMd)
            )
        }
    }
}

@FerroPreviews
@Composable
private fun TrainScreenPreview() {
    FerroTheme {
        TrainContent(
            uiState =
                TrainUiState(
                    routines =
                        listOf(
                            TrainRoutineItemUi(
                                id = "1",
                                name = "Torso A",
                                exerciseCount = 5,
                                totalSetCount = 16,
                                exerciseNamesSummary = "Press banca · Remo con barra · Press militar"
                            )
                        )
                ),
            onStartEmptyWorkout = {},
            onStartRoutine = {},
            onResumeActiveWorkout = {},
            onDiscardAndStartNew = {},
            onDismissConflict = {}
        )
    }
}
