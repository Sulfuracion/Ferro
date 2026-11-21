package com.davidguijo.ferro.feature.workout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroBottomSheet
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.IncompleteSetsStrategy
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.resolveRestDurationSeconds
import com.davidguijo.ferro.feature.routines.formatRestMinSec
import com.davidguijo.ferro.feature.routines.toFerroSetType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.serialization.Serializable

/** Entreno activo a pantalla completa (sin barra inferior). */
@Serializable
data class ActiveWorkoutRoute(val routineId: String? = null)

@Composable
fun ActiveWorkoutScreen(
    onMinimize: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onPickExercisesToAdd: () -> Unit,
    onPickExerciseToReplace: () -> Unit,
    onWorkoutFinished: (String) -> Unit,
    onWorkoutDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockState by viewModel.clockState.collectAsStateWithLifecycle()
    val defaultWorkoutName = stringResource(R.string.active_workout_default_name)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAskedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var showNotificationRationale by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.onNotificationPermissionGranted()
            }
        }

    LaunchedEffect(defaultWorkoutName) {
        viewModel.initializeIfNeeded(defaultWorkoutName)
    }

    LaunchedEffect(uiState.hasActiveWorkout) {
        if (uiState.hasActiveWorkout &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasAskedNotificationPermission
        ) {
            val isGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                hasAskedNotificationPermission = true
                showNotificationRationale = true
            }
        }
    }

    LaunchedEffect(uiState.completedWorkoutIdForSummary) {
        val workoutId = uiState.completedWorkoutIdForSummary ?: return@LaunchedEffect
        viewModel.onNavigatedAfterFinishOrDiscard()
        onWorkoutFinished(workoutId)
    }

    LaunchedEffect(uiState.shouldNavigateBackAfterDiscard) {
        if (uiState.shouldNavigateBackAfterDiscard) {
            viewModel.onNavigatedAfterFinishOrDiscard()
            onWorkoutDiscarded()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        viewModel.setScreenVisible(true)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        viewModel.setScreenVisible(false)
                    }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.setScreenVisible(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        onMinimize()
    }

    ActiveWorkoutContent(
        uiState = uiState,
        clockState = clockState,
        onMinimize = onMinimize,
        onFinishClick = viewModel::onRequestFinishWorkout,
        onOpenWorkoutMenu = viewModel::onOpenWorkoutMenu,
        onDismissWorkoutMenu = viewModel::onDismissWorkoutMenu,
        onOpenRenameDialog = viewModel::onOpenRenameDialog,
        onDismissRenameDialog = viewModel::onDismissRenameDialog,
        onSaveWorkoutName = viewModel::onWorkoutNameChanged,
        onOpenNotesDialog = viewModel::onOpenNotesDialog,
        onDismissNotesDialog = viewModel::onDismissNotesDialog,
        onSaveWorkoutNotes = viewModel::onWorkoutNotesChanged,
        onStartManualRest = viewModel::onStartManualRestTimer,
        onRequestDiscard = viewModel::onRequestDiscardWorkout,
        onDismissDiscard = viewModel::onDismissDiscardDialog,
        onConfirmDiscard = viewModel::onConfirmDiscardWorkout,
        onOpenExerciseDetail = onOpenExerciseDetail,
        onOpenExerciseMenu = viewModel::onOpenExerciseMenu,
        onDismissExerciseMenu = viewModel::onDismissExerciseMenu,
        onReplaceExerciseClick = { workoutExerciseId ->
            viewModel.onPrepareReplaceExercise(workoutExerciseId)
            onPickExerciseToReplace()
        },
        onOpenExerciseRestDialog = viewModel::onOpenExerciseRestDialog,
        onDismissExerciseRestDialog = viewModel::onDismissExerciseRestDialog,
        onSaveExerciseRest = viewModel::onSetExerciseRestSeconds,
        onMoveExercise = viewModel::onMoveExercise,
        onPairSuperset = viewModel::onPairSuperset,
        onRemoveFromSuperset = viewModel::onRemoveFromSuperset,
        onUngroupSuperset = viewModel::onUngroupSuperset,
        onRemoveExercise = viewModel::onRemoveExercise,
        onExerciseNotesChanged = viewModel::onExerciseNotesChanged,
        onToggleExerciseRpe = viewModel::onToggleExerciseRpeColumn,
        onOpenSetTypePicker = viewModel::onOpenSetTypePicker,
        onDismissSetTypePicker = viewModel::onDismissSetTypePicker,
        onSelectSetType = viewModel::onSelectSetType,
        onCopyPrevious = viewModel::onCopyPreviousToSet,
        onSetWeightChanged = viewModel::onSetWeightChanged,
        onSetRepsChanged = viewModel::onSetRepsChanged,
        onSetDurationChanged = viewModel::onSetDurationChanged,
        onSetDistanceChanged = viewModel::onSetDistanceChanged,
        onOpenRpePicker = viewModel::onOpenRpePicker,
        onDismissRpePicker = viewModel::onDismissRpePicker,
        onSelectRpe = viewModel::onSelectRpe,
        onToggleSetCompleted = viewModel::onToggleSetCompleted,
        onRemoveSet = viewModel::onRemoveSet,
        onAddSet = viewModel::onAddSet,
        onAddExercisesClick = {
            viewModel.onPrepareAddExercises()
            onPickExercisesToAdd()
        },
        onAdjustRestTimer = viewModel::onAdjustRestTimer,
        onSkipRestTimer = viewModel::onSkipRestTimer,
        onOpenExactAlarmSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    val intent =
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                }
            }
        },
        onDismissFinishIncomplete = viewModel::onDismissFinishIncompleteDialog,
        onConfirmFinishIncomplete = viewModel::onConfirmFinishIncomplete,
        onResumeConflict = viewModel::onResumeExistingInConflict,
        onDiscardConflictAndStartRoutine = viewModel::onDiscardAndStartConflictRoutine,
        modifier = modifier
    )

    if (showNotificationRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ConfirmDialog(
            title = stringResource(R.string.active_workout_notif_permission_title),
            message = stringResource(R.string.active_workout_notif_permission_message),
            confirmLabel = stringResource(R.string.active_workout_notif_permission_allow),
            cancelLabel = stringResource(R.string.active_workout_notif_permission_later),
            destructive = false,
            onConfirm = {
                showNotificationRationale = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { showNotificationRationale = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    clockState: ActiveWorkoutClockUiState,
    onMinimize: () -> Unit,
    onFinishClick: () -> Unit,
    onOpenWorkoutMenu: () -> Unit,
    onDismissWorkoutMenu: () -> Unit,
    onOpenRenameDialog: () -> Unit,
    onDismissRenameDialog: () -> Unit,
    onSaveWorkoutName: (String) -> Unit,
    onOpenNotesDialog: () -> Unit,
    onDismissNotesDialog: () -> Unit,
    onSaveWorkoutNotes: (String) -> Unit,
    onStartManualRest: () -> Unit,
    onRequestDiscard: () -> Unit,
    onDismissDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenExerciseMenu: (String) -> Unit,
    onDismissExerciseMenu: () -> Unit,
    onReplaceExerciseClick: (String) -> Unit,
    onOpenExerciseRestDialog: (String) -> Unit,
    onDismissExerciseRestDialog: () -> Unit,
    onSaveExerciseRest: (String, Int?) -> Unit,
    onMoveExercise: (String, MoveDirection) -> Unit,
    onPairSuperset: (String, MoveDirection) -> Unit,
    onRemoveFromSuperset: (String) -> Unit,
    onUngroupSuperset: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onExerciseNotesChanged: (String, String) -> Unit,
    onToggleExerciseRpe: (String) -> Unit,
    onOpenSetTypePicker: (String, String, SetType) -> Unit,
    onDismissSetTypePicker: () -> Unit,
    onSelectSetType: (SetType) -> Unit,
    onCopyPrevious: (String, String, com.davidguijo.ferro.core.domain.model.TrackingType) -> Unit,
    onSetWeightChanged: (String, String, String) -> Unit,
    onSetRepsChanged: (String, String, String) -> Unit,
    onSetDurationChanged: (String, String, String) -> Unit,
    onSetDistanceChanged: (String, String, String) -> Unit,
    onOpenRpePicker: (String, String, Double?) -> Unit,
    onDismissRpePicker: () -> Unit,
    onSelectRpe: (Double?) -> Unit,
    onToggleSetCompleted: (String, String) -> Unit,
    onRemoveSet: (String, String) -> Unit,
    onAddSet: (String) -> Unit,
    onAddExercisesClick: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onSkipRestTimer: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onDismissFinishIncomplete: () -> Unit,
    onConfirmFinishIncomplete: (IncompleteSetsStrategy) -> Unit,
    onResumeConflict: () -> Unit,
    onDiscardConflictAndStartRoutine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val listState = rememberLazyListState()
    val displayTitle =
        uiState.workoutName.ifBlank { stringResource(R.string.active_workout_default_name) }

    val screenModifier =
        modifier
            .imePadding()
            .then(if (uiState.hasActiveWorkout) Modifier.keepScreenOn() else Modifier)

    Scaffold(
        modifier = screenModifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier =
                            Modifier
                                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                                .clip(RoundedCornerShape(FerroTheme.radius.sm))
                                .clickable(enabled = uiState.hasActiveWorkout, onClick = onOpenRenameDialog)
                                .padding(horizontal = spacing.space1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            style = FerroTheme.typography.title,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (uiState.hasActiveWorkout) {
                            Spacer(modifier = Modifier.width(spacing.space1))
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.active_workout_rename_action),
                                modifier = Modifier.size(FerroTheme.sizes.iconSm),
                                tint = colors.inkMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = stringResource(R.string.active_workout_minimize_cd),
                        onClick = onMinimize
                    )
                },
                actions = {
                    if (uiState.hasActiveWorkout) {
                        FerroButton(
                            text = stringResource(R.string.active_workout_finish),
                            onClick = onFinishClick,
                            variant = FerroButtonVariant.Primary
                        )
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.active_workout_menu_cd),
                            onClick = onOpenWorkoutMenu
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        },
        bottomBar = {
            RestTimerBar(
                restTimer = clockState.restTimer,
                completedRestEventId = uiState.lastCompletedRestEventId,
                showExactAlarmHint = !uiState.canScheduleExactAlarms,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onAdjustSeconds = onAdjustRestTimer,
                onSkip = onSkipRestTimer
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            if (uiState.hasActiveWorkout) {
                LiveWorkoutStatsHeader(
                    elapsedText = clockState.elapsedText,
                    volumeText = uiState.volumeText,
                    completedSetsCount = uiState.completedSetsCount,
                    totalSetsCount = uiState.totalSetsCount,
                    workoutNotes = uiState.workoutNotes,
                    onEditNotes = onOpenNotesDialog
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = spacing.space4,
                        vertical = spacing.space4
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.space5)
            ) {
                if (uiState.isReady && uiState.exercises.isEmpty()) {
                    item(key = "empty_exercises") {
                        EmptyState(
                            title = stringResource(R.string.active_workout_empty_title),
                            description = stringResource(R.string.active_workout_empty_description),
                            icon = painterResource(R.drawable.ic_fitness_center)
                        )
                    }
                } else {
                    items(
                        items = uiState.exercises,
                        key = { it.workoutExerciseId }
                    ) { exercise ->
                        ActiveExerciseCard(
                            exercise = exercise,
                            weightUnit = uiState.weightUnit,
                            defaultRestSeconds = uiState.defaultRestSeconds,
                            isSpanish = uiState.isSpanish,
                            onOpenExerciseDetail = onOpenExerciseDetail,
                            onOpenExerciseMenu = { onOpenExerciseMenu(exercise.workoutExerciseId) },
                            onNotesChanged = { notes ->
                                onExerciseNotesChanged(exercise.workoutExerciseId, notes)
                            },
                            onToggleRpeColumn = { onToggleExerciseRpe(exercise.workoutExerciseId) },
                            onOpenSetTypePicker = { setId, currentType ->
                                onOpenSetTypePicker(exercise.workoutExerciseId, setId, currentType)
                            },
                            onCopyPrevious = { setId ->
                                onCopyPrevious(exercise.workoutExerciseId, setId, exercise.trackingType)
                            },
                            onSetWeightChanged = { setId, value ->
                                onSetWeightChanged(exercise.workoutExerciseId, setId, value)
                            },
                            onSetRepsChanged = { setId, value ->
                                onSetRepsChanged(exercise.workoutExerciseId, setId, value)
                            },
                            onSetDurationChanged = { setId, value ->
                                onSetDurationChanged(exercise.workoutExerciseId, setId, value)
                            },
                            onSetDistanceChanged = { setId, value ->
                                onSetDistanceChanged(exercise.workoutExerciseId, setId, value)
                            },
                            onOpenRpePicker = { setId, currentRpe ->
                                onOpenRpePicker(exercise.workoutExerciseId, setId, currentRpe)
                            },
                            onToggleSetCompleted = { setId ->
                                onToggleSetCompleted(exercise.workoutExerciseId, setId)
                            },
                            onRemoveSet = { setId ->
                                onRemoveSet(exercise.workoutExerciseId, setId)
                            },
                            onAddSet = { onAddSet(exercise.workoutExerciseId) }
                        )
                    }
                }

                if (uiState.hasActiveWorkout) {
                    item(key = "add_exercises_button") {
                        FerroButton(
                            text = stringResource(R.string.active_workout_add_exercises),
                            onClick = onAddExercisesClick,
                            variant = FerroButtonVariant.Secondary,
                            leadingIcon = painterResource(R.drawable.ic_add),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (uiState.showWorkoutMenu) {
        WorkoutMenuSheet(
            onRename = onOpenRenameDialog,
            onNotes = onOpenNotesDialog,
            onStartRest = onStartManualRest,
            onDiscard = onRequestDiscard,
            onDismiss = onDismissWorkoutMenu
        )
    }

    if (uiState.showRenameDialog) {
        WorkoutTextInputDialog(
            title = stringResource(R.string.active_workout_rename_title),
            initialValue = uiState.workoutName,
            placeholder = stringResource(R.string.active_workout_rename_placeholder),
            singleLine = true,
            onSave = { raw ->
                val trimmed = raw.trim()
                if (trimmed.isNotEmpty()) {
                    onSaveWorkoutName(trimmed)
                } else {
                    onDismissRenameDialog()
                }
            },
            onDismiss = onDismissRenameDialog
        )
    }

    if (uiState.showNotesDialog) {
        WorkoutTextInputDialog(
            title = stringResource(R.string.active_workout_notes_title),
            initialValue = uiState.workoutNotes,
            placeholder = stringResource(R.string.active_workout_notes_placeholder),
            singleLine = false,
            onSave = onSaveWorkoutNotes,
            onDismiss = onDismissNotesDialog
        )
    }

    val menuExercise =
        uiState.activeExerciseMenuId?.let { id ->
            uiState.exercises.firstOrNull { it.workoutExerciseId == id }
        }
    if (menuExercise != null) {
        ActiveExerciseMenuSheet(
            exercise = menuExercise,
            onReplace = { onReplaceExerciseClick(menuExercise.workoutExerciseId) },
            onRestTimer = { onOpenExerciseRestDialog(menuExercise.workoutExerciseId) },
            onPairAbove = { onPairSuperset(menuExercise.workoutExerciseId, MoveDirection.UP) },
            onPairBelow = { onPairSuperset(menuExercise.workoutExerciseId, MoveDirection.DOWN) },
            onRemoveFromSuperset = { onRemoveFromSuperset(menuExercise.workoutExerciseId) },
            onUngroupSuperset = { group -> onUngroupSuperset(group) },
            onMoveUp = { onMoveExercise(menuExercise.workoutExerciseId, MoveDirection.UP) },
            onMoveDown = { onMoveExercise(menuExercise.workoutExerciseId, MoveDirection.DOWN) },
            onRemove = { onRemoveExercise(menuExercise.workoutExerciseId) },
            onDismiss = onDismissExerciseMenu
        )
    }

    val restExercise =
        uiState.activeExerciseRestDialogId?.let { id ->
            uiState.exercises.firstOrNull { it.workoutExerciseId == id }
        }
    if (restExercise != null) {
        ExerciseRestPickerSheet(
            initialRestSeconds = restExercise.restSeconds,
            defaultRestSeconds = uiState.defaultRestSeconds,
            onSave = { selected -> onSaveExerciseRest(restExercise.workoutExerciseId, selected) },
            onDismiss = onDismissExerciseRestDialog
        )
    }

    val setTypePicker = uiState.activeSetTypePicker
    if (setTypePicker != null) {
        ActiveSetTypePickerSheet(
            currentType = setTypePicker.currentType,
            onSelectSetType = onSelectSetType,
            onDismiss = onDismissSetTypePicker
        )
    }

    val rpePicker = uiState.activeRpePicker
    if (rpePicker != null) {
        RpePickerSheet(
            currentRpe = rpePicker.currentRpe,
            isSpanish = uiState.isSpanish,
            onSelectRpe = onSelectRpe,
            onDismiss = onDismissRpePicker
        )
    }

    if (uiState.showFinishIncompleteDialog) {
        FinishIncompleteSetsDialog(
            incompleteCount = uiState.incompleteSetsCount,
            onDiscardIncompleteAndFinish = {
                onConfirmFinishIncomplete(IncompleteSetsStrategy.DISCARD_INCOMPLETE)
            },
            onMarkAllAndFinish = {
                onConfirmFinishIncomplete(IncompleteSetsStrategy.MARK_ALL_COMPLETED)
            },
            onDismiss = onDismissFinishIncomplete
        )
    }

    if (uiState.showDiscardConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.active_workout_discard_title),
            message = stringResource(R.string.active_workout_discard_message),
            confirmLabel = stringResource(R.string.active_workout_discard_confirm),
            onConfirm = onConfirmDiscard,
            onDismiss = onDismissDiscard
        )
    }

    if (uiState.conflictRoutineId != null) {
        ActiveWorkoutConflictDialog(
            title = stringResource(R.string.active_workout_conflict_title),
            message = stringResource(R.string.active_workout_conflict_message, displayTitle),
            resumeLabel = stringResource(R.string.train_resume_active_resume),
            discardLabel = stringResource(R.string.active_workout_conflict_start_routine),
            onResume = onResumeConflict,
            onDiscardAndStartNew = onDiscardConflictAndStartRoutine,
            onDismiss = onResumeConflict
        )
    }
}

@Composable
private fun LiveWorkoutStatsHeader(
    elapsedText: String,
    volumeText: String,
    completedSetsCount: Int,
    totalSetsCount: Int,
    workoutNotes: String,
    onEditNotes: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Surface(color = colors.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FerroDivider()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space4, vertical = spacing.space3),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCell(
                    label = stringResource(R.string.active_workout_stat_time),
                    value = elapsedText,
                    valueColor = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = stringResource(R.string.active_workout_stat_volume),
                    value = volumeText,
                    valueColor = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = stringResource(R.string.active_workout_stat_sets),
                    value = "$completedSetsCount/$totalSetsCount",
                    valueColor = if (completedSetsCount > 0) colors.brasaText else colors.ink,
                    modifier = Modifier.weight(1f)
                )
            }
            if (workoutNotes.isNotBlank()) {
                FerroDivider()
                Text(
                    text = workoutNotes,
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEditNotes)
                            .padding(horizontal = spacing.space4, vertical = spacing.space2)
                )
            }
            FerroDivider()
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = FerroTheme.colors.inkMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = FerroTheme.typography.number,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutMenuSheet(
    onRename: () -> Unit,
    onNotes: () -> Unit,
    onStartRest: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    FerroBottomSheet(onDismissRequest = onDismiss) {
        FerroListItem(
            title = stringResource(R.string.active_workout_rename_action),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.inkMuted
                )
            },
            onClick = onRename
        )
        FerroDivider()
        FerroListItem(
            title = stringResource(R.string.active_workout_notes_action),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.inkMuted
                )
            },
            onClick = onNotes
        )
        FerroDivider()
        FerroListItem(
            title = stringResource(R.string.active_workout_manual_rest_action),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.inkMuted
                )
            },
            onClick = onStartRest
        )
        FerroDivider()
        FerroListItem(
            title = stringResource(R.string.active_workout_discard_action),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.danger
                )
            },
            onClick = onDiscard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveExerciseMenuSheet(
    exercise: ActiveExerciseUi,
    onReplace: () -> Unit,
    onRestTimer: () -> Unit,
    onPairAbove: () -> Unit,
    onPairBelow: () -> Unit,
    onRemoveFromSuperset: () -> Unit,
    onUngroupSuperset: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = exercise.name
    ) {
        FerroListItem(
            title = stringResource(R.string.active_exercise_replace),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_swap_horiz),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.inkMuted
                )
            },
            onClick = onReplace
        )
        FerroDivider()
        FerroListItem(
            title = stringResource(R.string.active_exercise_rest),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.inkMuted
                )
            },
            onClick = onRestTimer
        )
        if (exercise.canPairAbove) {
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_superset_above),
                onClick = onPairAbove
            )
        }
        if (exercise.canPairBelow) {
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_superset_below),
                onClick = onPairBelow
            )
        }
        val group = exercise.supersetGroup
        if (group != null) {
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_superset_remove),
                onClick = onRemoveFromSuperset
            )
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_superset_ungroup, group),
                onClick = { onUngroupSuperset(group) }
            )
        }
        if (exercise.canMoveUp) {
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_move_up),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_upward),
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconMd),
                        tint = colors.inkMuted
                    )
                },
                onClick = onMoveUp
            )
        }
        if (exercise.canMoveDown) {
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.active_exercise_move_down),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_downward),
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconMd),
                        tint = colors.inkMuted
                    )
                },
                onClick = onMoveDown
            )
        }
        FerroDivider()
        FerroListItem(
            title = stringResource(R.string.active_exercise_remove),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.danger
                )
            },
            onClick = onRemove
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseRestPickerSheet(
    initialRestSeconds: Int?,
    defaultRestSeconds: Int,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    var selectedSeconds by rememberSaveable(initialRestSeconds) {
        mutableIntStateOf(resolveRestDurationSeconds(initialRestSeconds, defaultRestSeconds))
    }

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.active_exercise_rest)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.settings_rest_decrease_cd),
                    enabled = selectedSeconds > Settings.MIN_REST_SECONDS,
                    containerColor = colors.surfaceRaised,
                    onClick = {
                        selectedSeconds =
                            (selectedSeconds - REST_STEP_SECONDS).coerceAtLeast(Settings.MIN_REST_SECONDS)
                    }
                )
                Text(
                    text = formatRestMinSec(selectedSeconds),
                    style = FerroTheme.typography.numberXl,
                    color = colors.ink
                )
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.settings_rest_increase_cd),
                    enabled = selectedSeconds < Settings.MAX_REST_SECONDS,
                    containerColor = colors.surfaceRaised,
                    onClick = {
                        selectedSeconds =
                            (selectedSeconds + REST_STEP_SECONDS).coerceAtMost(Settings.MAX_REST_SECONDS)
                    }
                )
            }

            FerroButton(
                text =
                    stringResource(
                        R.string.active_exercise_rest_default_option,
                        formatRestMinSec(defaultRestSeconds)
                    ),
                onClick = { onSave(null) },
                variant = FerroButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            FerroButton(
                text = stringResource(R.string.active_workout_save_action),
                onClick = { onSave(selectedSeconds) },
                variant = FerroButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSetTypePickerSheet(currentType: SetType, onSelectSetType: (SetType) -> Unit, onDismiss: () -> Unit) {
    val colors = FerroTheme.colors
    val options =
        listOf(
            SetType.WARMUP to R.string.routine_editor_set_type_warmup,
            SetType.NORMAL to R.string.routine_editor_set_type_normal,
            SetType.FAILURE to R.string.routine_editor_set_type_failure,
            SetType.DROP to R.string.routine_editor_set_type_drop
        )

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.routine_editor_set_type_sheet_title)
    ) {
        options.forEachIndexed { index, (type, labelRes) ->
            if (index > 0) FerroDivider()
            FerroListItem(
                title = stringResource(labelRes),
                leadingContent = {
                    SetTypeBadge(type = type.toFerroSetType(), setNumber = 1)
                },
                trailingContent = {
                    if (currentType == type) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconMd),
                            tint = colors.brasaText
                        )
                    }
                },
                onClick = { onSelectSetType(type) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RpePickerSheet(
    currentRpe: Double?,
    isSpanish: Boolean,
    onSelectRpe: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = FerroTheme.spacing
    val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
    val formatter =
        DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale)).apply {
            isGroupingUsed = false
        }

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.active_set_rpe_sheet_title)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                ActiveWorkoutViewModel.RPE_OPTIONS.forEach { rpe ->
                    FerroChip(
                        label = formatter.format(rpe),
                        selected = currentRpe == rpe,
                        onClick = { onSelectRpe(rpe) }
                    )
                }
            }
            if (currentRpe != null) {
                FerroButton(
                    text = stringResource(R.string.active_set_rpe_clear),
                    onClick = { onSelectRpe(null) },
                    variant = FerroButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutTextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    singleLine: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    var text by rememberSaveable(initialValue) { mutableStateOf(initialValue) }

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
                Spacer(modifier = Modifier.height(spacing.space3))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = placeholder, style = FerroTheme.typography.body)
                    },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 3,
                    maxLines = if (singleLine) 1 else 6,
                    textStyle = FerroTheme.typography.body,
                    shape = RoundedCornerShape(FerroTheme.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.ink,
                            unfocusedTextColor = colors.ink,
                            focusedContainerColor = colors.surfaceRaised,
                            unfocusedContainerColor = colors.surfaceRaised,
                            focusedBorderColor = colors.brasaText,
                            unfocusedBorderColor = colors.lineStrong,
                            cursorColor = colors.brasaText,
                            focusedPlaceholderColor = colors.inkSubtle,
                            unfocusedPlaceholderColor = colors.inkSubtle
                        )
                )
                Spacer(modifier = Modifier.height(spacing.space4))
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
                        text = stringResource(R.string.active_workout_save_action),
                        onClick = { onSave(text) },
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
private fun FinishIncompleteSetsDialog(
    incompleteCount: Int,
    onDiscardIncompleteAndFinish: () -> Unit,
    onMarkAllAndFinish: () -> Unit,
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
                    text = stringResource(R.string.active_workout_finish_incomplete_title),
                    style = FerroTheme.typography.heading,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(spacing.space2))
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.active_workout_finish_incomplete_message,
                            incompleteCount,
                            incompleteCount
                        ),
                    style = FerroTheme.typography.body,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.height(spacing.space5))
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    FerroButton(
                        text = stringResource(R.string.active_workout_finish_discard_incomplete),
                        onClick = onDiscardIncompleteAndFinish,
                        variant = FerroButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FerroButton(
                        text = stringResource(R.string.active_workout_finish_mark_all),
                        onClick = onMarkAllAndFinish,
                        variant = FerroButtonVariant.Secondary,
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

private const val REST_STEP_SECONDS = 15
