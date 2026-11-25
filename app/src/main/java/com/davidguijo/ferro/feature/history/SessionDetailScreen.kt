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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.feature.routines.formatDistanceMeters
import com.davidguijo.ferro.feature.routines.supersetAccentColor
import com.davidguijo.ferro.feature.routines.toFerroSetType
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.serialization.Serializable

/** Detalle de una sesión pasada del historial. */
@Serializable
data class SessionDetailRoute(val workoutId: String)

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(uiState.shouldOpenActiveWorkout) {
        if (uiState.shouldOpenActiveWorkout) {
            viewModel.onNavigatedToActiveWorkout()
            onOpenActiveWorkout()
        }
    }

    SessionDetailContent(
        uiState = uiState,
        onBack = onBack,
        onEnterEdit = viewModel::onEnterEditMode,
        onCancelEdit = viewModel::onCancelEdit,
        onSaveEdit = viewModel::onSaveEdit,
        onEditNameChange = viewModel::onEditNameChange,
        onEditDateSelected = viewModel::onEditDateSelected,
        onEditDurationMinutesChange = viewModel::onEditDurationMinutesChange,
        onEditNotesChange = viewModel::onEditNotesChange,
        onEditExerciseNotesChange = viewModel::onEditExerciseNotesChange,
        onCycleEditSetType = viewModel::onCycleEditSetType,
        onEditSetWeightChange = viewModel::onEditSetWeightChange,
        onEditSetRepsChange = viewModel::onEditSetRepsChange,
        onEditSetRpeChange = viewModel::onEditSetRpeChange,
        onEditSetDurationChange = viewModel::onEditSetDurationChange,
        onEditSetDistanceChange = viewModel::onEditSetDistanceChange,
        onToggleEditSetCompleted = viewModel::onToggleEditSetCompleted,
        onAddEditSet = viewModel::onAddEditSet,
        onRemoveEditSet = viewModel::onRemoveEditSet,
        onRequestDelete = viewModel::onRequestDelete,
        onDismissDelete = viewModel::onDismissDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        onRepeatWorkout = viewModel::onRepeatWorkoutClick,
        onDismissRepeatConflict = viewModel::onDismissRepeatConflict,
        onResumeActiveWorkout = viewModel::onResumeActiveWorkout,
        onDiscardActiveAndRepeat = viewModel::onDiscardActiveAndRepeat,
        onOpenSaveRoutineDialog = viewModel::onOpenSaveRoutineDialog,
        onDismissSaveRoutineDialog = viewModel::onDismissSaveRoutineDialog,
        onSaveRoutineNameChange = viewModel::onSaveRoutineNameChange,
        onConfirmSaveRoutine = viewModel::onConfirmSaveRoutine,
        onConsumeSavedRoutineMessage = viewModel::onConsumeSavedRoutineMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionDetailContent(
    uiState: SessionDetailUiState,
    onBack: () -> Unit,
    onEnterEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditDateSelected: (Long) -> Unit,
    onEditDurationMinutesChange: (String) -> Unit,
    onEditNotesChange: (String) -> Unit,
    onEditExerciseNotesChange: (String, String) -> Unit,
    onCycleEditSetType: (String, String) -> Unit,
    onEditSetWeightChange: (String, String, String) -> Unit,
    onEditSetRepsChange: (String, String, String) -> Unit,
    onEditSetRpeChange: (String, String, String) -> Unit,
    onEditSetDurationChange: (String, String, String) -> Unit,
    onEditSetDistanceChange: (String, String, String) -> Unit,
    onToggleEditSetCompleted: (String, String) -> Unit,
    onAddEditSet: (String) -> Unit,
    onRemoveEditSet: (String, String) -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRepeatWorkout: () -> Unit,
    onDismissRepeatConflict: () -> Unit,
    onResumeActiveWorkout: () -> Unit,
    onDiscardActiveAndRepeat: () -> Unit,
    onOpenSaveRoutineDialog: () -> Unit,
    onDismissSaveRoutineDialog: () -> Unit,
    onSaveRoutineNameChange: (String) -> Unit,
    onConfirmSaveRoutine: () -> Unit,
    onConsumeSavedRoutineMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    val savedRoutineMessage =
        uiState.savedRoutineName?.let { name ->
            stringResource(R.string.session_detail_routine_saved, name)
        }
    LaunchedEffect(savedRoutineMessage) {
        if (savedRoutineMessage != null) {
            snackbarHostState.showSnackbar(savedRoutineMessage)
            onConsumeSavedRoutineMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            if (uiState.workoutFound) {
                                uiState.name
                            } else {
                                stringResource(R.string.session_detail_title)
                            },
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
                        onClick = if (uiState.isEditing) onCancelEdit else onBack
                    )
                },
                actions = {
                    if (uiState.workoutFound && !uiState.isEditing) {
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.session_detail_edit_cd),
                            onClick = onEnterEdit
                        )
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.session_detail_delete_cd),
                            onClick = onRequestDelete,
                            tint = colors.danger
                        )
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
        when {
            !uiState.isLoaded -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.brasa)
                }
            }

            !uiState.workoutFound -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = stringResource(R.string.session_detail_empty_title),
                        description = stringResource(R.string.session_detail_empty_description),
                        icon = painterResource(R.drawable.ic_calendar_month)
                    )
                }
            }

            uiState.editDraft != null -> {
                EditSessionBody(
                    draft = uiState.editDraft,
                    weightUnit = uiState.weightUnit,
                    isSpanish = uiState.isSpanish,
                    isSaving = uiState.isSavingEdit,
                    onNameChange = onEditNameChange,
                    onDateSelected = onEditDateSelected,
                    onDurationMinutesChange = onEditDurationMinutesChange,
                    onNotesChange = onEditNotesChange,
                    onExerciseNotesChange = onEditExerciseNotesChange,
                    onCycleSetType = onCycleEditSetType,
                    onWeightChange = onEditSetWeightChange,
                    onRepsChange = onEditSetRepsChange,
                    onRpeChange = onEditSetRpeChange,
                    onDurationChange = onEditSetDurationChange,
                    onDistanceChange = onEditSetDistanceChange,
                    onToggleCompleted = onToggleEditSetCompleted,
                    onAddSet = onAddEditSet,
                    onRemoveSet = onRemoveEditSet,
                    onCancel = onCancelEdit,
                    onSave = onSaveEdit,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding =
                        PaddingValues(
                            horizontal = spacing.space4,
                            vertical = spacing.space3
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4)
                ) {
                    item(key = "session_header") {
                        SessionOverviewCard(uiState = uiState)
                    }

                    item(key = "session_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                        ) {
                            FerroButton(
                                text = stringResource(R.string.session_detail_repeat),
                                onClick = onRepeatWorkout,
                                variant = FerroButtonVariant.Primary,
                                modifier = Modifier.weight(1f)
                            )
                            FerroButton(
                                text = stringResource(R.string.session_detail_save_routine),
                                onClick = onOpenSaveRoutineDialog,
                                variant = FerroButtonVariant.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    items(
                        items = uiState.exercises,
                        key = { it.id }
                    ) { exercise ->
                        SessionExerciseCard(
                            exercise = exercise,
                            weightUnit = uiState.weightUnit,
                            isSpanish = uiState.isSpanish
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.session_detail_delete_title),
            message = stringResource(R.string.session_detail_delete_message),
            confirmLabel = stringResource(R.string.session_detail_delete_confirm),
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
            destructive = true
        )
    }

    if (uiState.showSaveRoutineDialog) {
        SaveRoutineNameDialog(
            name = uiState.saveRoutineNameDraft,
            onNameChange = onSaveRoutineNameChange,
            onConfirm = onConfirmSaveRoutine,
            onDismiss = onDismissSaveRoutineDialog
        )
    }

    if (uiState.showRepeatConflictDialog) {
        RepeatWorkoutConflictDialog(
            activeWorkoutName = uiState.activeWorkoutName.orEmpty(),
            onResume = onResumeActiveWorkout,
            onDiscardAndStart = onDiscardActiveAndRepeat,
            onDismiss = onDismissRepeatConflict
        )
    }
}

@Composable
private fun SessionOverviewCard(uiState: SessionDetailUiState) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = uiState.dateTimeText,
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            OverviewStat(
                label = stringResource(R.string.history_stat_duration),
                value = uiState.durationText ?: stringResource(R.string.history_duration_unknown),
                modifier = Modifier.weight(1f)
            )
            OverviewStat(
                label = stringResource(R.string.history_stat_volume),
                value = uiState.volumeText,
                modifier = Modifier.weight(1f)
            )
            OverviewStat(
                label = stringResource(R.string.history_stat_sets),
                value = uiState.completedSetsCount.toString(),
                modifier = Modifier.weight(0.7f)
            )
            if (uiState.totalRecordsCount > 0) {
                OverviewStat(
                    label = stringResource(R.string.history_stat_records),
                    value = uiState.totalRecordsCount.toString(),
                    highlight = true,
                    modifier = Modifier.weight(0.7f)
                )
            }
        }
        if (uiState.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.space3))
            FerroDivider()
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text = stringResource(R.string.session_detail_notes_label).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkSubtle
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = uiState.notes,
                style = FerroTheme.typography.body,
                color = colors.ink
            )
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val colors = FerroTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = FerroTheme.typography.number,
            color = if (highlight) colors.brasaText else colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionExerciseCard(exercise: SessionDetailExerciseUi, weightUnit: WeightUnit, isSpanish: Boolean) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val supersetColor = exercise.supersetGroup?.let { supersetAccentColor(it) }

    FerroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            if (supersetColor != null && exercise.supersetGroup != null) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                color = supersetColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                            .padding(horizontal = spacing.space2, vertical = spacing.space1)
                ) {
                    Text(
                        text = exercise.supersetGroup,
                        style = FerroTheme.typography.label,
                        color = supersetColor
                    )
                }
            }
            Text(
                text = exercise.name,
                style = FerroTheme.typography.heading,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
        }

        if (exercise.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = exercise.notes,
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }

        Spacer(modifier = Modifier.height(spacing.space2))

        exercise.sets.forEachIndexed { index, set ->
            if (index > 0) {
                FerroDivider()
            }
            SessionSetRow(
                set = set,
                trackingType = exercise.trackingType,
                weightUnit = weightUnit,
                isSpanish = isSpanish
            )
        }
    }
}

@Composable
private fun SessionSetRow(
    set: SessionDetailSetUi,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    isSpanish: Boolean
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH

    val mainValueText =
        formatDetailSetMetric(
            set = set,
            trackingType = trackingType,
            weightUnit = weightUnit,
            locale = locale
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.space1)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = FerroTheme.sizes.rowHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            SetTypeBadge(
                type = set.setType.toFerroSetType(),
                setNumber = set.normalSetNumber
            )

            Text(
                text = mainValueText,
                style = FerroTheme.typography.number,
                color = if (set.completed) colors.ink else colors.inkMuted,
                modifier = Modifier.weight(1f)
            )

            if (set.rpe != null) {
                val rpeText =
                    if (set.rpe % 1.0 == 0.0) {
                        set.rpe.toInt().toString()
                    } else {
                        set.rpe.toString()
                    }
                Text(
                    text = stringResource(R.string.session_detail_rpe_badge, rpeText),
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted,
                    modifier =
                        Modifier
                            .background(
                                color = colors.surfaceRaised,
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                            .padding(horizontal = spacing.space2, vertical = spacing.space1)
                )
            }

            if (!set.completed) {
                Text(
                    text = stringResource(R.string.session_detail_set_incomplete),
                    style = FerroTheme.typography.caption,
                    color = colors.inkSubtle
                )
            } else if (set.isRecord) {
                Row(
                    modifier =
                        Modifier
                            .background(
                                color = colors.brasaSoft,
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                            .padding(horizontal = spacing.space2, vertical = spacing.space1),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = colors.brasaText,
                        modifier = Modifier.size(FerroTheme.sizes.iconSm)
                    )
                    Text(
                        text = set.recordTypes.joinToString(" · ") { shortRecordLabel(it, isSpanish) },
                        style = FerroTheme.typography.caption,
                        color = colors.brasaText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionBody(
    draft: EditSessionDraft,
    weightUnit: WeightUnit,
    isSpanish: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onDurationMinutesChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onExerciseNotesChange: (String, String) -> Unit,
    onCycleSetType: (String, String) -> Unit,
    onWeightChange: (String, String, String) -> Unit,
    onRepsChange: (String, String, String) -> Unit,
    onRpeChange: (String, String, String) -> Unit,
    onDurationChange: (String, String, String) -> Unit,
    onDistanceChange: (String, String, String) -> Unit,
    onToggleCompleted: (String, String) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            item(key = "edit_session_meta") {
                FerroCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.session_detail_workout_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ferroOutlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FerroCard(
                            modifier = Modifier.weight(1.3f),
                            onClick = { showDatePicker = true }
                        ) {
                            Text(
                                text = stringResource(R.string.session_detail_date_label).uppercase(),
                                style = FerroTheme.typography.label,
                                color = colors.inkSubtle
                            )
                            Spacer(modifier = Modifier.height(spacing.space1))
                            Text(
                                text = formatSessionDateTime(draft.startedAtMillis, isSpanish, ZoneId.systemDefault()),
                                style = FerroTheme.typography.bodyStrong,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedTextField(
                            value = draft.durationMinutesText,
                            onValueChange = onDurationMinutesChange,
                            label = { Text(stringResource(R.string.session_detail_duration_minutes_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = ferroOutlinedTextFieldColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.space3))
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = onNotesChange,
                        label = { Text(stringResource(R.string.session_detail_notes_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = ferroOutlinedTextFieldColors()
                    )
                }
            }

            items(
                items = draft.exercises,
                key = { it.workoutExerciseId }
            ) { exercise ->
                EditExerciseCard(
                    exercise = exercise,
                    weightUnit = weightUnit,
                    onNotesChange = { onExerciseNotesChange(exercise.workoutExerciseId, it) },
                    onCycleSetType = { setId -> onCycleSetType(exercise.workoutExerciseId, setId) },
                    onWeightChange = { setId, v -> onWeightChange(exercise.workoutExerciseId, setId, v) },
                    onRepsChange = { setId, v -> onRepsChange(exercise.workoutExerciseId, setId, v) },
                    onRpeChange = { setId, v -> onRpeChange(exercise.workoutExerciseId, setId, v) },
                    onDurationChange = { setId, v -> onDurationChange(exercise.workoutExerciseId, setId, v) },
                    onDistanceChange = { setId, v -> onDistanceChange(exercise.workoutExerciseId, setId, v) },
                    onToggleCompleted = { setId -> onToggleCompleted(exercise.workoutExerciseId, setId) },
                    onAddSet = { onAddSet(exercise.workoutExerciseId) },
                    onRemoveSet = { setId -> onRemoveSet(exercise.workoutExerciseId, setId) }
                )
            }
        }

        FerroDivider()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            FerroButton(
                text = stringResource(R.string.session_detail_cancel_edit),
                onClick = onCancel,
                variant = FerroButtonVariant.Secondary,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            )
            FerroButton(
                text = stringResource(R.string.session_detail_save_changes),
                onClick = onSave,
                variant = FerroButtonVariant.Primary,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDatePicker) {
        val localDate = Instant.ofEpochMilli(draft.startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val initialUtcMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.session_detail_save_changes), color = colors.brasaText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = colors.inkMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EditExerciseCard(
    exercise: EditExerciseDraft,
    weightUnit: WeightUnit,
    onNotesChange: (String) -> Unit,
    onCycleSetType: (String) -> Unit,
    onWeightChange: (String, String) -> Unit,
    onRepsChange: (String, String) -> Unit,
    onRpeChange: (String, String) -> Unit,
    onDurationChange: (String, String) -> Unit,
    onDistanceChange: (String, String) -> Unit,
    onToggleCompleted: (String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = exercise.name,
            style = FerroTheme.typography.heading,
            color = colors.ink
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        OutlinedTextField(
            value = exercise.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.session_detail_exercise_notes_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = ferroOutlinedTextFieldColors()
        )
        Spacer(modifier = Modifier.height(spacing.space3))

        var normalCounter = 0
        exercise.sets.forEach { set ->
            if (set.setType == SetType.NORMAL) {
                normalCounter++
            }
            val displayNumber = if (set.setType == SetType.NORMAL) normalCounter else 1
            EditSetRow(
                set = set,
                normalSetNumber = displayNumber,
                trackingType = exercise.trackingType,
                weightUnit = weightUnit,
                onCycleSetType = { onCycleSetType(set.id) },
                onWeightChange = { onWeightChange(set.id, it) },
                onRepsChange = { onRepsChange(set.id, it) },
                onRpeChange = { onRpeChange(set.id, it) },
                onDurationChange = { onDurationChange(set.id, it) },
                onDistanceChange = { onDistanceChange(set.id, it) },
                onToggleCompleted = { onToggleCompleted(set.id) },
                onRemove = { onRemoveSet(set.id) }
            )
            Spacer(modifier = Modifier.height(spacing.space2))
        }

        FerroButton(
            text = stringResource(R.string.session_detail_add_set),
            onClick = onAddSet,
            variant = FerroButtonVariant.Secondary,
            leadingIcon = painterResource(R.drawable.ic_add),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditSetRow(
    set: EditSetDraft,
    normalSetNumber: Int,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    onCycleSetType: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRpeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onToggleCompleted: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val unitSuffix = if (weightUnit == WeightUnit.KG) "kg" else "lb"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space1)
    ) {
        SetTypeBadge(
            type = set.setType.toFerroSetType(),
            setNumber = normalSetNumber,
            onClick = onCycleSetType
        )

        when (trackingType) {
            TrackingType.WEIGHT_REPS,
            TrackingType.WEIGHTED_BODYWEIGHT,
            TrackingType.ASSISTED_BODYWEIGHT -> {
                OutlinedTextField(
                    value = set.weightText,
                    onValueChange = onWeightChange,
                    label = { Text(unitSuffix, maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = ferroOutlinedTextFieldColors()
                )
                OutlinedTextField(
                    value = set.repsText,
                    onValueChange = onRepsChange,
                    label = { Text("Reps", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.85f),
                    colors = ferroOutlinedTextFieldColors()
                )
            }

            TrackingType.BODYWEIGHT_REPS -> {
                OutlinedTextField(
                    value = set.repsText,
                    onValueChange = onRepsChange,
                    label = { Text("Reps", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.85f),
                    colors = ferroOutlinedTextFieldColors()
                )
            }

            TrackingType.DURATION -> {
                OutlinedTextField(
                    value = set.durationText,
                    onValueChange = onDurationChange,
                    label = { Text("s", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.85f),
                    colors = ferroOutlinedTextFieldColors()
                )
            }

            TrackingType.DISTANCE_DURATION -> {
                OutlinedTextField(
                    value = set.distanceText,
                    onValueChange = onDistanceChange,
                    label = { Text("m", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = ferroOutlinedTextFieldColors()
                )
                OutlinedTextField(
                    value = set.durationText,
                    onValueChange = onDurationChange,
                    label = { Text("s", maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.85f),
                    colors = ferroOutlinedTextFieldColors()
                )
            }
        }

        OutlinedTextField(
            value = set.rpeText,
            onValueChange = onRpeChange,
            label = { Text("RPE", maxLines = 1) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(0.75f),
            colors = ferroOutlinedTextFieldColors()
        )

        Box(
            modifier =
                Modifier
                    .size(FerroTheme.sizes.touchMin)
                    .clip(RoundedCornerShape(FerroTheme.radius.sm))
                    .clickable(role = Role.Checkbox, onClick = onToggleCompleted),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(
                            color = if (set.completed) colors.brasa else colors.surfaceRaised,
                            shape = RoundedCornerShape(FerroTheme.radius.sm)
                        )
                        .border(
                            width = 1.dp,
                            color = if (set.completed) colors.brasa else colors.lineStrong,
                            shape = RoundedCornerShape(FerroTheme.radius.sm)
                        ),
                contentAlignment = Alignment.Center
            ) {
                if (set.completed) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.session_detail_completed_cd),
                        tint = colors.onBrasa,
                        modifier = Modifier.size(FerroTheme.sizes.iconSm)
                    )
                }
            }
        }

        FerroIconButton(
            painter = painterResource(R.drawable.ic_remove),
            contentDescription = stringResource(R.string.session_detail_remove_set_cd),
            onClick = onRemove,
            tint = colors.inkMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveRoutineNameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
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
                    text = stringResource(R.string.session_detail_save_routine_title),
                    style = FerroTheme.typography.heading,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(spacing.space3))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.session_detail_save_routine_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ferroOutlinedTextFieldColors()
                )
                Spacer(modifier = Modifier.height(spacing.space5))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    FerroButton(
                        text = stringResource(R.string.dialog_cancel),
                        onClick = onDismiss,
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    FerroButton(
                        text = stringResource(R.string.session_detail_save_routine_confirm),
                        onClick = onConfirm,
                        variant = FerroButtonVariant.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatWorkoutConflictDialog(
    activeWorkoutName: String,
    onResume: () -> Unit,
    onDiscardAndStart: () -> Unit,
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
                    text = stringResource(R.string.train_resume_active_title),
                    style = FerroTheme.typography.heading,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(spacing.space2))
                Text(
                    text = stringResource(R.string.train_resume_active_message, activeWorkoutName),
                    style = FerroTheme.typography.body,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.height(spacing.space5))
                FerroButton(
                    text = stringResource(R.string.train_resume_active_resume),
                    onClick = onResume,
                    variant = FerroButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(spacing.space2))
                FerroButton(
                    text = stringResource(R.string.train_resume_active_discard_new),
                    onClick = onDiscardAndStart,
                    variant = FerroButtonVariant.Danger,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ferroOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FerroTheme.colors.focus,
    unfocusedBorderColor = FerroTheme.colors.lineStrong,
    focusedTextColor = FerroTheme.colors.ink,
    unfocusedTextColor = FerroTheme.colors.ink,
    focusedLabelColor = FerroTheme.colors.brasaText,
    unfocusedLabelColor = FerroTheme.colors.inkMuted,
    cursorColor = FerroTheme.colors.brasa
)

@Composable
private fun formatDetailSetMetric(
    set: SessionDetailSetUi,
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    locale: Locale
): String = when (trackingType) {
    TrackingType.WEIGHT_REPS -> {
        val weight =
            set.weightKg?.let { Units.formatWeight(it, weightUnit, locale = locale, includeUnit = true) } ?: "--"
        val reps = set.reps?.toString() ?: "--"
        "$weight × $reps"
    }

    TrackingType.BODYWEIGHT_REPS -> {
        val reps = set.reps ?: 0
        pluralStringResource(R.plurals.routine_set_summary_reps_fixed, reps, reps)
    }

    TrackingType.WEIGHTED_BODYWEIGHT -> {
        val weight =
            set.weightKg?.let {
                "+" + Units.formatWeight(it, weightUnit, locale = locale, includeUnit = true)
            } ?: "--"
        val reps = set.reps?.toString() ?: "--"
        "$weight × $reps"
    }

    TrackingType.ASSISTED_BODYWEIGHT -> {
        val weight =
            set.weightKg?.let {
                "-" + Units.formatWeight(it, weightUnit, locale = locale, includeUnit = true)
            } ?: "--"
        val reps = set.reps?.toString() ?: "--"
        "$weight × $reps"
    }

    TrackingType.DURATION ->
        set.durationS?.let { formatElapsedDuration(it * 1000L) } ?: "--"

    TrackingType.DISTANCE_DURATION -> {
        val dist = set.distanceM?.let { formatDistanceMeters(it, locale.language == "es") } ?: "--"
        val dur = set.durationS?.let { formatElapsedDuration(it * 1000L) } ?: "--"
        "$dist · $dur"
    }
}

private fun shortRecordLabel(recordType: RecordType, isSpanish: Boolean): String = when (recordType) {
    RecordType.MAX_WEIGHT -> if (isSpanish) "Máx" else "Max"
    RecordType.ESTIMATED_1RM -> "1RM"
    RecordType.BEST_SET_VOLUME -> "Vol"
    RecordType.MAX_REPS_AT_WEIGHT -> "Reps"
}
