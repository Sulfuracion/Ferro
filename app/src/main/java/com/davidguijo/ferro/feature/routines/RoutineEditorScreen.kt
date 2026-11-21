package com.davidguijo.ferro.feature.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroBottomSheet
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.component.NumberField
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.computeNormalSetNumbers
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.feature.exercises.labelRes
import java.util.Locale
import kotlinx.serialization.Serializable

/** Editor de rutina (creación cuando [routineId] es null o edición cuando no lo es). */
@Serializable
data class RoutineEditorRoute(val routineId: String? = null)

@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onPickExercises: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onBack()
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        viewModel.onRequestBack()
    }

    val duplicateName = stringResource(R.string.routine_copy_name_format, uiState.name.ifBlank { "" })

    RoutineEditorContent(
        uiState = uiState,
        onBackClick = viewModel::onRequestBack,
        onPickExercises = onPickExercises,
        onNameChanged = viewModel::onNameChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onFolderSelected = viewModel::onFolderSelected,
        onMoveExercise = viewModel::onMoveExercise,
        onRemoveExercise = viewModel::onRemoveExercise,
        onExerciseNotesChanged = viewModel::onExerciseNotesChanged,
        onAdjustExerciseRest = viewModel::onAdjustExerciseRest,
        onResetExerciseRestToDefault = viewModel::onResetExerciseRestToDefault,
        onToggleSupersetSelection = viewModel::onToggleSupersetSelection,
        onClearSupersetSelection = viewModel::onClearSupersetSelection,
        onGroupSelectedAsSuperset = viewModel::onGroupSelectedAsSuperset,
        onUngroupSuperset = viewModel::onUngroupSuperset,
        onAddSet = viewModel::onAddSet,
        onRemoveSet = viewModel::onRemoveSet,
        onOpenSetTypePicker = viewModel::onOpenSetTypePicker,
        onDismissSetTypePicker = viewModel::onDismissSetTypePicker,
        onSelectSetType = viewModel::onSelectSetType,
        onSetWeightChanged = viewModel::onSetWeightChanged,
        onSetRepsMinChanged = viewModel::onSetRepsMinChanged,
        onSetRepsMaxChanged = viewModel::onSetRepsMaxChanged,
        onSetDurationChanged = viewModel::onSetDurationChanged,
        onSetDistanceChanged = viewModel::onSetDistanceChanged,
        onDuplicateRoutine = { viewModel.onDuplicateCurrentRoutine(duplicateName) },
        onRequestDeleteRoutine = viewModel::onRequestDeleteRoutine,
        onDismissDeleteRoutine = viewModel::onDismissDeleteRoutineDialog,
        onConfirmDeleteRoutine = viewModel::onConfirmDeleteRoutine,
        onDismissDiscardChanges = viewModel::onDismissDiscardChangesDialog,
        onConfirmDiscardChanges = viewModel::onConfirmDiscardChanges,
        onSaveRoutine = viewModel::onSaveRoutine,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineEditorContent(
    uiState: RoutineEditorUiState,
    onBackClick: () -> Unit,
    onPickExercises: () -> Unit,
    onNameChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onMoveExercise: (String, MoveDirection) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onExerciseNotesChanged: (String, String) -> Unit,
    onAdjustExerciseRest: (String, Int) -> Unit,
    onResetExerciseRestToDefault: (String) -> Unit,
    onToggleSupersetSelection: (String) -> Unit,
    onClearSupersetSelection: () -> Unit,
    onGroupSelectedAsSuperset: () -> Unit,
    onUngroupSuperset: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, String) -> Unit,
    onOpenSetTypePicker: (String, String, SetType) -> Unit,
    onDismissSetTypePicker: () -> Unit,
    onSelectSetType: (SetType) -> Unit,
    onSetWeightChanged: (String, String, String) -> Unit,
    onSetRepsMinChanged: (String, String, String) -> Unit,
    onSetRepsMaxChanged: (String, String, String) -> Unit,
    onSetDurationChanged: (String, String, String) -> Unit,
    onSetDistanceChanged: (String, String, String) -> Unit,
    onDuplicateRoutine: () -> Unit,
    onRequestDeleteRoutine: () -> Unit,
    onDismissDeleteRoutine: () -> Unit,
    onConfirmDeleteRoutine: () -> Unit,
    onDismissDiscardChanges: () -> Unit,
    onConfirmDiscardChanges: () -> Unit,
    onSaveRoutine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    val titleRes =
        if (uiState.isEditingExisting) {
            R.string.routine_editor_edit_title
        } else {
            R.string.routine_editor_create_title
        }

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
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
                        onClick = onBackClick
                    )
                },
                actions = {
                    if (uiState.isEditingExisting) {
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_content_copy),
                            contentDescription = stringResource(R.string.routine_duplicate_action),
                            onClick = onDuplicateRoutine
                        )
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.routine_delete_action),
                            tint = colors.danger,
                            onClick = onRequestDeleteRoutine
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
                        text = stringResource(R.string.routine_editor_save),
                        onClick = onSaveRoutine,
                        variant = FerroButtonVariant.Primary,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(spacing.space4)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.space4),
            contentPadding = PaddingValues(top = spacing.space2, bottom = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            item(key = "routine_metadata") {
                RoutineMetadataCard(
                    name = uiState.name,
                    notes = uiState.notes,
                    folderId = uiState.folderId,
                    folders = uiState.folders,
                    showNameError = uiState.showNameError,
                    onNameChanged = onNameChanged,
                    onNotesChanged = onNotesChanged,
                    onFolderSelected = onFolderSelected
                )
            }

            if (uiState.selectedSupersetExerciseIds.isNotEmpty()) {
                item(key = "superset_selection_bar") {
                    SupersetSelectionBar(
                        canGroup = uiState.canGroupSelectedAsSuperset,
                        onGroup = onGroupSelectedAsSuperset,
                        onClear = onClearSupersetSelection
                    )
                }
            }

            if (uiState.exercises.isEmpty()) {
                item(key = "empty_exercises") {
                    EmptyState(
                        title = stringResource(R.string.routine_editor_empty_title),
                        description = stringResource(R.string.routine_editor_empty_description),
                        icon = painterResource(R.drawable.ic_fitness_center)
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.exercises,
                    key = { _, item -> item.id }
                ) { index, exerciseDraft ->
                    val exercise = uiState.exercisesById[exerciseDraft.exerciseId]
                    RoutineExerciseEditorCard(
                        exerciseDraft = exerciseDraft,
                        exercise = exercise,
                        isSpanish = uiState.isSpanish,
                        weightUnit = uiState.weightUnit,
                        defaultRestSeconds = uiState.defaultRestSeconds,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.exercises.lastIndex,
                        isSelectedForSuperset = exerciseDraft.id in uiState.selectedSupersetExerciseIds,
                        onToggleSupersetSelection = { onToggleSupersetSelection(exerciseDraft.id) },
                        onUngroupSuperset = { group -> onUngroupSuperset(group) },
                        onMoveUp = { onMoveExercise(exerciseDraft.id, MoveDirection.UP) },
                        onMoveDown = { onMoveExercise(exerciseDraft.id, MoveDirection.DOWN) },
                        onRemoveExercise = { onRemoveExercise(exerciseDraft.id) },
                        onNotesChanged = { notes -> onExerciseNotesChanged(exerciseDraft.id, notes) },
                        onAdjustRest = { delta -> onAdjustExerciseRest(exerciseDraft.id, delta) },
                        onResetRest = { onResetExerciseRestToDefault(exerciseDraft.id) },
                        onAddSet = { onAddSet(exerciseDraft.id) },
                        onRemoveSet = { setId -> onRemoveSet(exerciseDraft.id, setId) },
                        onOpenSetTypePicker = { setId, currentType ->
                            onOpenSetTypePicker(exerciseDraft.id, setId, currentType)
                        },
                        onSetWeightChanged = { setId, value ->
                            onSetWeightChanged(exerciseDraft.id, setId, value)
                        },
                        onSetRepsMinChanged = { setId, value ->
                            onSetRepsMinChanged(exerciseDraft.id, setId, value)
                        },
                        onSetRepsMaxChanged = { setId, value ->
                            onSetRepsMaxChanged(exerciseDraft.id, setId, value)
                        },
                        onSetDurationChanged = { setId, value ->
                            onSetDurationChanged(exerciseDraft.id, setId, value)
                        },
                        onSetDistanceChanged = { setId, value ->
                            onSetDistanceChanged(exerciseDraft.id, setId, value)
                        }
                    )
                }
            }

            item(key = "add_exercises_button") {
                FerroButton(
                    text = stringResource(R.string.routine_editor_add_exercises),
                    onClick = onPickExercises,
                    variant = FerroButtonVariant.Secondary,
                    leadingIcon = painterResource(R.drawable.ic_add),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val activePicker = uiState.activeSetTypePicker
    if (activePicker != null) {
        SetTypePickerSheet(
            currentType = activePicker.currentType,
            onSelectSetType = onSelectSetType,
            onDismiss = onDismissSetTypePicker
        )
    }

    if (uiState.showDiscardChangesDialog) {
        ConfirmDialog(
            title = stringResource(R.string.routine_editor_discard_title),
            message = stringResource(R.string.routine_editor_discard_message),
            confirmLabel = stringResource(R.string.routine_editor_discard_confirm),
            onConfirm = onConfirmDiscardChanges,
            onDismiss = onDismissDiscardChanges
        )
    }

    if (uiState.showDeleteRoutineDialog) {
        ConfirmDialog(
            title = stringResource(R.string.routine_delete_confirm_title),
            message = stringResource(R.string.routine_delete_confirm_message),
            confirmLabel = stringResource(R.string.routine_delete_action),
            onConfirm = onConfirmDeleteRoutine,
            onDismiss = onDismissDeleteRoutine
        )
    }
}

@Composable
private fun RoutineMetadataCard(
    name: String,
    notes: String,
    folderId: String?,
    folders: List<com.davidguijo.ferro.core.domain.model.RoutineFolder>,
    showNameError: Boolean,
    onNameChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    val textFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            focusedContainerColor = colors.surfaceRaised,
            unfocusedContainerColor = colors.surfaceRaised,
            focusedBorderColor = colors.brasaText,
            unfocusedBorderColor = colors.lineStrong,
            errorBorderColor = colors.danger,
            cursorColor = colors.brasaText,
            focusedPlaceholderColor = colors.inkSubtle,
            unfocusedPlaceholderColor = colors.inkSubtle
        )

    FerroCard {
        Text(
            text = stringResource(R.string.routine_editor_name_label).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted,
            modifier = Modifier.padding(bottom = spacing.space2)
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.routine_editor_name_placeholder),
                    style = FerroTheme.typography.body
                )
            },
            isError = showNameError,
            singleLine = true,
            textStyle = FerroTheme.typography.body,
            shape = RoundedCornerShape(FerroTheme.radius.md),
            colors = textFieldColors
        )
        if (showNameError) {
            Text(
                text = stringResource(R.string.routine_editor_name_error),
                style = FerroTheme.typography.caption,
                color = colors.danger,
                modifier = Modifier.padding(top = spacing.space1)
            )
        }

        Spacer(modifier = Modifier.height(spacing.space3))

        Text(
            text = stringResource(R.string.routine_editor_notes_label).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted,
            modifier = Modifier.padding(bottom = spacing.space2)
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.routine_editor_notes_placeholder),
                    style = FerroTheme.typography.body
                )
            },
            minLines = 2,
            maxLines = 4,
            textStyle = FerroTheme.typography.body,
            shape = RoundedCornerShape(FerroTheme.radius.md),
            colors = textFieldColors
        )

        if (folders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.space3))
            Text(
                text = stringResource(R.string.routine_editor_folder_label).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = spacing.space2)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                item(key = "folder_none") {
                    FerroChip(
                        label = stringResource(R.string.routines_uncategorized_folder),
                        selected = folderId == null,
                        onClick = { onFolderSelected(null) }
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FerroChip(
                        label = folder.name,
                        selected = folderId == folder.id,
                        onClick = { onFolderSelected(folder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupersetSelectionBar(canGroup: Boolean, onGroup: () -> Unit, onClear: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(raised = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FerroButton(
                text = stringResource(R.string.routine_editor_group_superset),
                onClick = onGroup,
                enabled = canGroup,
                variant = FerroButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
            FerroButton(
                text = stringResource(R.string.routine_editor_clear_selection),
                onClick = onClear,
                variant = FerroButtonVariant.Ghost
            )
        }
        if (!canGroup) {
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text = stringResource(R.string.routine_editor_superset_hint),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }
    }
}

@Composable
private fun RoutineExerciseEditorCard(
    exerciseDraft: RoutineExerciseDraft,
    exercise: Exercise?,
    isSpanish: Boolean,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isSelectedForSuperset: Boolean,
    onToggleSupersetSelection: () -> Unit,
    onUngroupSuperset: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveExercise: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onResetRest: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit,
    onOpenSetTypePicker: (String, SetType) -> Unit,
    onSetWeightChanged: (String, String) -> Unit,
    onSetRepsMinChanged: (String, String) -> Unit,
    onSetRepsMaxChanged: (String, String) -> Unit,
    onSetDurationChanged: (String, String) -> Unit,
    onSetDistanceChanged: (String, String) -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val exerciseName = exercise?.localizedName(isSpanish) ?: exerciseDraft.exerciseId
    val trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
    val supersetGroup = exerciseDraft.supersetGroup
    val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US

    FerroCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
        ) {
            if (supersetGroup != null) {
                Box(
                    modifier =
                        Modifier
                            .width(spacing.space1)
                            .fillMaxHeight()
                            .background(
                                color = supersetAccentColor(supersetGroup),
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                )
                Spacer(modifier = Modifier.width(spacing.space3))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (supersetGroup != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.routine_superset_badge_format, supersetGroup).uppercase(),
                            style = FerroTheme.typography.label,
                            color = supersetAccentColor(supersetGroup)
                        )
                        FerroChip(
                            label = stringResource(R.string.routine_editor_ungroup_superset),
                            selected = false,
                            onClick = { onUngroupSuperset(supersetGroup) }
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.space2))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.routine_editor_select_superset_cd),
                        onClick = onToggleSupersetSelection,
                        tint = if (isSelectedForSuperset) colors.brasaText else colors.inkMuted,
                        containerColor = if (isSelectedForSuperset) colors.brasaSoft else colors.surfaceRaised,
                        iconSize = FerroTheme.sizes.iconSm
                    )
                    Spacer(modifier = Modifier.width(spacing.space2))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exerciseName,
                            style = FerroTheme.typography.heading,
                            color = colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (exercise != null) {
                            Text(
                                text = stringResource(exercise.primaryMuscle.labelRes()),
                                style = FerroTheme.typography.caption,
                                color = colors.inkMuted
                            )
                        }
                    }
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_arrow_upward),
                        contentDescription = stringResource(R.string.routine_editor_move_up_cd),
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        tint = colors.inkMuted,
                        iconSize = FerroTheme.sizes.iconMd
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_arrow_downward),
                        contentDescription = stringResource(R.string.routine_editor_move_down_cd),
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        tint = colors.inkMuted,
                        iconSize = FerroTheme.sizes.iconMd
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.routine_editor_remove_exercise_cd),
                        onClick = onRemoveExercise,
                        tint = colors.danger,
                        iconSize = FerroTheme.sizes.iconMd
                    )
                }

                Spacer(modifier = Modifier.height(spacing.space3))

                ExerciseRestRow(
                    restSeconds = exerciseDraft.restSeconds,
                    defaultRestSeconds = defaultRestSeconds,
                    onDecrease = { onAdjustRest(-REST_STEP_SECONDS) },
                    onIncrease = { onAdjustRest(REST_STEP_SECONDS) },
                    onReset = onResetRest
                )

                Spacer(modifier = Modifier.height(spacing.space2))

                OutlinedTextField(
                    value = exerciseDraft.notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.routine_editor_exercise_notes_placeholder),
                            style = FerroTheme.typography.body
                        )
                    },
                    singleLine = true,
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

                Spacer(modifier = Modifier.height(spacing.space3))

                SetsTableHeader(
                    trackingType = trackingType,
                    weightUnit = weightUnit
                )

                Spacer(modifier = Modifier.height(spacing.space1))

                val normalSetNumbers = computeNormalSetNumbers(exerciseDraft.sets.map { it.setType })
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    exerciseDraft.sets.forEachIndexed { setIndex, setDraft ->
                        RoutineSetEditorRow(
                            setDraft = setDraft,
                            normalSetNumber = normalSetNumbers.getOrElse(setIndex) { setIndex + 1 },
                            trackingType = trackingType,
                            locale = locale,
                            onBadgeClick = { onOpenSetTypePicker(setDraft.id, setDraft.setType) },
                            onWeightChanged = { onSetWeightChanged(setDraft.id, it) },
                            onRepsMinChanged = { onSetRepsMinChanged(setDraft.id, it) },
                            onRepsMaxChanged = { onSetRepsMaxChanged(setDraft.id, it) },
                            onDurationChanged = { onSetDurationChanged(setDraft.id, it) },
                            onDistanceChanged = { onSetDistanceChanged(setDraft.id, it) },
                            onRemoveSet = { onRemoveSet(setDraft.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space2))

                FerroButton(
                    text = stringResource(R.string.routine_editor_add_set),
                    onClick = onAddSet,
                    variant = FerroButtonVariant.Ghost,
                    leadingIcon = painterResource(R.drawable.ic_add)
                )
            }
        }
    }
}

@Composable
private fun ExerciseRestRow(
    restSeconds: Int?,
    defaultRestSeconds: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val restDisplayText =
        if (restSeconds == null) {
            stringResource(R.string.routine_editor_rest_default_format, formatRestMinSec(defaultRestSeconds))
        } else {
            formatRestMinSec(restSeconds)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(FerroTheme.sizes.iconMd),
            tint = colors.inkMuted
        )
        Spacer(modifier = Modifier.width(spacing.space2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.routine_editor_rest_label),
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
            Text(
                text = restDisplayText,
                style = FerroTheme.typography.number,
                color = colors.ink
            )
        }
        if (restSeconds != null) {
            FerroChip(
                label = stringResource(R.string.routine_editor_rest_reset),
                selected = false,
                onClick = onReset
            )
            Spacer(modifier = Modifier.width(spacing.space1))
        }
        FerroIconButton(
            painter = painterResource(R.drawable.ic_remove),
            contentDescription = stringResource(R.string.settings_rest_decrease_cd),
            onClick = onDecrease,
            iconSize = FerroTheme.sizes.iconMd
        )
        FerroIconButton(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.settings_rest_increase_cd),
            onClick = onIncrease,
            iconSize = FerroTheme.sizes.iconMd
        )
    }
}

@Composable
private fun SetsTableHeader(trackingType: TrackingType, weightUnit: WeightUnit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val unitLabel =
        when (weightUnit) {
            WeightUnit.KG -> stringResource(R.string.settings_unit_kg)
            WeightUnit.LB -> stringResource(R.string.settings_unit_lb)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
    ) {
        Text(
            text = stringResource(R.string.routine_editor_col_set).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted,
            modifier = Modifier.width(FerroTheme.sizes.touchMin)
        )

        when (trackingType) {
            TrackingType.WEIGHT_REPS -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_weight, unitLabel).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_min).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_max).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.WEIGHTED_BODYWEIGHT -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_extra_weight, unitLabel).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_min).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_max).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.ASSISTED_BODYWEIGHT -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_assist_weight, unitLabel).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_min).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_max).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.BODYWEIGHT_REPS -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_min).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_reps_max).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.DURATION -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_duration).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.DISTANCE_DURATION -> {
                Text(
                    text = stringResource(R.string.routine_editor_col_distance).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_editor_col_duration).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(FerroTheme.sizes.touchMin))
    }
}

@Composable
private fun RoutineSetEditorRow(
    setDraft: RoutineSetDraft,
    normalSetNumber: Int,
    trackingType: TrackingType,
    locale: Locale,
    onBadgeClick: () -> Unit,
    onWeightChanged: (String) -> Unit,
    onRepsMinChanged: (String) -> Unit,
    onRepsMaxChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onRemoveSet: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
    ) {
        SetTypeBadge(
            type = setDraft.setType.toFerroSetType(),
            setNumber = normalSetNumber,
            onClick = onBadgeClick
        )

        when (trackingType) {
            TrackingType.WEIGHT_REPS,
            TrackingType.WEIGHTED_BODYWEIGHT,
            TrackingType.ASSISTED_BODYWEIGHT -> {
                NumberField(
                    value = setDraft.weightInput,
                    onValueChange = onWeightChanged,
                    placeholder = "—",
                    allowDecimal = true,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = setDraft.repsMinInput,
                    onValueChange = onRepsMinChanged,
                    placeholder = "8",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = setDraft.repsMaxInput,
                    onValueChange = onRepsMaxChanged,
                    placeholder = "10",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.BODYWEIGHT_REPS -> {
                NumberField(
                    value = setDraft.repsMinInput,
                    onValueChange = onRepsMinChanged,
                    placeholder = "8",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = setDraft.repsMaxInput,
                    onValueChange = onRepsMaxChanged,
                    placeholder = "10",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.DURATION -> {
                NumberField(
                    value = setDraft.durationSecondsInput,
                    onValueChange = onDurationChanged,
                    placeholder = "60",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
            }

            TrackingType.DISTANCE_DURATION -> {
                NumberField(
                    value = setDraft.distanceMetersInput,
                    onValueChange = onDistanceChanged,
                    placeholder = "1000",
                    allowDecimal = true,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = setDraft.durationSecondsInput,
                    onValueChange = onDurationChanged,
                    placeholder = "300",
                    allowDecimal = false,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FerroIconButton(
            painter = painterResource(R.drawable.ic_remove),
            contentDescription = stringResource(R.string.routine_editor_remove_set_cd),
            onClick = onRemoveSet,
            tint = colors.inkMuted,
            iconSize = FerroTheme.sizes.iconMd
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetTypePickerSheet(currentType: SetType, onSelectSetType: (SetType) -> Unit, onDismiss: () -> Unit) {
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
            if (index > 0) {
                FerroDivider()
            }
            FerroListItem(
                title = stringResource(labelRes),
                leadingContent = {
                    SetTypeBadge(
                        type = type.toFerroSetType(),
                        setNumber = 1
                    )
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

private const val REST_STEP_SECONDS = 15

@FerroPreviews
@Composable
private fun RoutineEditorScreenPreview() {
    FerroTheme {
        RoutineEditorContent(
            uiState = RoutineEditorUiState(),
            onBackClick = {},
            onPickExercises = {},
            onNameChanged = {},
            onNotesChanged = {},
            onFolderSelected = {},
            onMoveExercise = { _, _ -> },
            onRemoveExercise = {},
            onExerciseNotesChanged = { _, _ -> },
            onAdjustExerciseRest = { _, _ -> },
            onResetExerciseRestToDefault = {},
            onToggleSupersetSelection = {},
            onClearSupersetSelection = {},
            onGroupSelectedAsSuperset = {},
            onUngroupSuperset = {},
            onAddSet = {},
            onRemoveSet = { _, _ -> },
            onOpenSetTypePicker = { _, _, _ -> },
            onDismissSetTypePicker = {},
            onSelectSetType = {},
            onSetWeightChanged = { _, _, _ -> },
            onSetRepsMinChanged = { _, _, _ -> },
            onSetRepsMaxChanged = { _, _, _ -> },
            onSetDurationChanged = { _, _, _ -> },
            onSetDistanceChanged = { _, _, _ -> },
            onDuplicateRoutine = {},
            onRequestDeleteRoutine = {},
            onDismissDeleteRoutine = {},
            onConfirmDeleteRoutine = {},
            onDismissDiscardChanges = {},
            onConfirmDiscardChanges = {},
            onSaveRoutine = {}
        )
    }
}
