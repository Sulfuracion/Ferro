package com.davidguijo.ferro.feature.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.computeNormalSetNumbers
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineTemplate
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises

@Composable
fun RoutinesScreen(
    onOpenExerciseLibrary: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RoutinesContent(
        uiState = uiState,
        onOpenExerciseLibrary = onOpenExerciseLibrary,
        onCreateRoutine = onCreateRoutine,
        onEditRoutine = onEditRoutine,
        onStartWorkout = onStartWorkout,
        onToggleFolderExpanded = viewModel::onToggleFolderExpanded,
        onToggleUncategorizedExpanded = viewModel::onToggleUncategorizedExpanded,
        onOpenCreateFolder = viewModel::onOpenCreateFolder,
        onOpenRenameFolder = viewModel::onOpenRenameFolder,
        onDismissFolderEditor = viewModel::onDismissFolderEditor,
        onSaveFolder = viewModel::onSaveFolder,
        onRequestDeleteFolder = viewModel::onRequestDeleteFolder,
        onDismissDeleteFolder = viewModel::onDismissDeleteFolder,
        onConfirmDeleteFolder = viewModel::onConfirmDeleteFolder,
        onMoveRoutine = viewModel::onMoveRoutine,
        onRequestMoveRoutineToFolder = viewModel::onRequestMoveRoutineToFolder,
        onDismissMoveRoutineToFolder = viewModel::onDismissMoveRoutineToFolder,
        onConfirmMoveRoutineToFolder = viewModel::onConfirmMoveRoutineToFolder,
        onDuplicateRoutine = viewModel::onDuplicateRoutine,
        onRequestDeleteRoutine = viewModel::onRequestDeleteRoutine,
        onDismissDeleteRoutine = viewModel::onDismissDeleteRoutine,
        onConfirmDeleteRoutine = viewModel::onConfirmDeleteRoutine,
        onOpenRoutinePreview = viewModel::onOpenRoutinePreview,
        onDismissRoutinePreview = viewModel::onDismissRoutinePreview,
        onOpenTemplateProgram = viewModel::onOpenTemplateProgram,
        onDismissTemplateProgram = viewModel::onDismissTemplateProgram,
        onCopyTemplateProgram = viewModel::onCopyTemplateProgram,
        onCopyTemplateRoutine = viewModel::onCopyTemplateRoutine,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutinesContent(
    uiState: RoutinesUiState,
    onOpenExerciseLibrary: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    onToggleFolderExpanded: (String) -> Unit,
    onToggleUncategorizedExpanded: () -> Unit,
    onOpenCreateFolder: () -> Unit,
    onOpenRenameFolder: (RoutineFolder) -> Unit,
    onDismissFolderEditor: () -> Unit,
    onSaveFolder: (String) -> Unit,
    onRequestDeleteFolder: (String) -> Unit,
    onDismissDeleteFolder: () -> Unit,
    onConfirmDeleteFolder: () -> Unit,
    onMoveRoutine: (String, MoveDirection) -> Unit,
    onRequestMoveRoutineToFolder: (String) -> Unit,
    onDismissMoveRoutineToFolder: () -> Unit,
    onConfirmMoveRoutineToFolder: (String?) -> Unit,
    onDuplicateRoutine: (String, String) -> Unit,
    onRequestDeleteRoutine: (String) -> Unit,
    onDismissDeleteRoutine: () -> Unit,
    onConfirmDeleteRoutine: () -> Unit,
    onOpenRoutinePreview: (String) -> Unit,
    onDismissRoutinePreview: () -> Unit,
    onOpenTemplateProgram: (String) -> Unit,
    onDismissTemplateProgram: () -> Unit,
    onCopyTemplateProgram: (RoutineTemplateProgram) -> Unit,
    onCopyTemplateRoutine: (RoutineTemplate) -> Unit,
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
                        text = stringResource(R.string.routines_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                actions = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = stringResource(R.string.routines_new_folder),
                        onClick = onOpenCreateFolder
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_fitness_center),
                        contentDescription = stringResource(R.string.routines_open_exercises),
                        onClick = onOpenExerciseLibrary
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.routines_new_routine),
                        onClick = onCreateRoutine
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
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.space4),
            contentPadding = PaddingValues(top = spacing.space2, bottom = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            if (!uiState.hasAnyRoutinesOrFolders) {
                item(key = "empty_routines") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.space3)
                    ) {
                        EmptyState(
                            title = stringResource(R.string.routines_empty_title),
                            description = stringResource(R.string.routines_empty_description),
                            icon = painterResource(R.drawable.ic_calendar_month)
                        )
                        FerroButton(
                            text = stringResource(R.string.routines_new_routine),
                            onClick = onCreateRoutine,
                            leadingIcon = painterResource(R.drawable.ic_add),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                uiState.folderSections.forEach { section ->
                    item(key = "folder_${section.folder.id}") {
                        FolderSectionHeader(
                            title = section.folder.name,
                            routineCount = section.routines.size,
                            isExpanded = section.isExpanded,
                            onToggleExpanded = { onToggleFolderExpanded(section.folder.id) },
                            onRename = { onOpenRenameFolder(section.folder) },
                            onDelete = { onRequestDeleteFolder(section.folder.id) }
                        )
                    }
                    if (section.isExpanded) {
                        items(
                            items = section.routines,
                            key = { "routine_${it.routineWithExercises.routine.id}" }
                        ) { summary ->
                            val routine = summary.routineWithExercises.routine
                            val copyName = stringResource(R.string.routine_copy_name_format, routine.name)
                            RoutineSummaryCard(
                                summary = summary,
                                onClick = { onOpenRoutinePreview(routine.id) },
                                onMoveUp = { onMoveRoutine(routine.id, MoveDirection.UP) },
                                onMoveDown = { onMoveRoutine(routine.id, MoveDirection.DOWN) },
                                onEdit = { onEditRoutine(routine.id) },
                                onDuplicate = { onDuplicateRoutine(routine.id, copyName) },
                                onMoveFolder = { onRequestMoveRoutineToFolder(routine.id) },
                                onDelete = { onRequestDeleteRoutine(routine.id) }
                            )
                        }
                    }
                }

                if (uiState.uncategorizedRoutines.isNotEmpty()) {
                    if (uiState.folderSections.isNotEmpty()) {
                        item(key = "folder_uncategorized") {
                            FolderSectionHeader(
                                title = stringResource(R.string.routines_uncategorized_folder),
                                routineCount = uiState.uncategorizedRoutines.size,
                                isExpanded = uiState.isUncategorizedExpanded,
                                onToggleExpanded = onToggleUncategorizedExpanded,
                                onRename = null,
                                onDelete = null
                            )
                        }
                    }
                    if (uiState.isUncategorizedExpanded || uiState.folderSections.isEmpty()) {
                        items(
                            items = uiState.uncategorizedRoutines,
                            key = { "routine_${it.routineWithExercises.routine.id}" }
                        ) { summary ->
                            val routine = summary.routineWithExercises.routine
                            val copyName = stringResource(R.string.routine_copy_name_format, routine.name)
                            RoutineSummaryCard(
                                summary = summary,
                                onClick = { onOpenRoutinePreview(routine.id) },
                                onMoveUp = { onMoveRoutine(routine.id, MoveDirection.UP) },
                                onMoveDown = { onMoveRoutine(routine.id, MoveDirection.DOWN) },
                                onEdit = { onEditRoutine(routine.id) },
                                onDuplicate = { onDuplicateRoutine(routine.id, copyName) },
                                onMoveFolder = { onRequestMoveRoutineToFolder(routine.id) },
                                onDelete = { onRequestDeleteRoutine(routine.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.templatePrograms.isNotEmpty()) {
                item(key = "templates_header") {
                    Column {
                        Spacer(modifier = Modifier.height(spacing.space3))
                        Text(
                            text = stringResource(R.string.routine_templates_section).uppercase(),
                            style = FerroTheme.typography.label,
                            color = colors.inkMuted
                        )
                    }
                }
                items(
                    items = uiState.templatePrograms,
                    key = { "template_${it.id}" }
                ) { program ->
                    TemplateProgramCard(
                        program = program,
                        isSpanish = uiState.isSpanish,
                        onOpen = { onOpenTemplateProgram(program.id) },
                        onCopyProgram = { onCopyTemplateProgram(program) }
                    )
                }
            }
        }
    }

    val folderDialog = uiState.folderEditorDialog
    if (folderDialog != null) {
        val initialName =
            when (folderDialog) {
                FolderEditorDialogState.Create -> ""
                is FolderEditorDialogState.Rename -> folderDialog.folder.name
            }
        val titleRes =
            when (folderDialog) {
                FolderEditorDialogState.Create -> R.string.routines_folder_create_title
                is FolderEditorDialogState.Rename -> R.string.routines_folder_rename_title
            }
        FolderEditorSheet(
            title = stringResource(titleRes),
            initialName = initialName,
            onSave = onSaveFolder,
            onDismiss = onDismissFolderEditor
        )
    }

    if (uiState.folderPendingDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.routines_folder_delete_title),
            message = stringResource(R.string.routines_folder_delete_message),
            confirmLabel = stringResource(R.string.routines_folder_delete_confirm),
            onConfirm = onConfirmDeleteFolder,
            onDismiss = onDismissDeleteFolder
        )
    }

    if (uiState.routinePendingDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.routine_delete_confirm_title),
            message = stringResource(R.string.routine_delete_confirm_message),
            confirmLabel = stringResource(R.string.routine_delete_action),
            onConfirm = onConfirmDeleteRoutine,
            onDismiss = onDismissDeleteRoutine
        )
    }

    val pendingMove = uiState.routinePendingMove
    if (pendingMove != null) {
        MoveRoutineToFolderSheet(
            currentFolderId = pendingMove.routine.folderId,
            folders = uiState.folderSections.map { it.folder },
            onSelectFolder = onConfirmMoveRoutineToFolder,
            onDismiss = onDismissMoveRoutineToFolder
        )
    }

    val previewRoutine = uiState.previewRoutine
    if (previewRoutine != null) {
        val copyName = stringResource(R.string.routine_copy_name_format, previewRoutine.routine.name)
        RoutinePreviewSheet(
            routineWithExercises = previewRoutine,
            exercisesById = uiState.exercisesById,
            isSpanish = uiState.isSpanish,
            weightUnit = uiState.weightUnit,
            defaultRestSeconds = uiState.defaultRestSeconds,
            onStartWorkout = {
                val id = previewRoutine.routine.id
                onDismissRoutinePreview()
                onStartWorkout(id)
            },
            onEdit = {
                val id = previewRoutine.routine.id
                onDismissRoutinePreview()
                onEditRoutine(id)
            },
            onDuplicate = {
                val id = previewRoutine.routine.id
                onDismissRoutinePreview()
                onDuplicateRoutine(id, copyName)
            },
            onMoveFolder = {
                val id = previewRoutine.routine.id
                onDismissRoutinePreview()
                onRequestMoveRoutineToFolder(id)
            },
            onDelete = {
                onRequestDeleteRoutine(previewRoutine.routine.id)
            },
            onDismiss = onDismissRoutinePreview
        )
    }

    val previewTemplate = uiState.previewTemplateProgram
    if (previewTemplate != null) {
        TemplateProgramPreviewSheet(
            program = previewTemplate,
            exercisesById = uiState.exercisesById,
            isSpanish = uiState.isSpanish,
            weightUnit = uiState.weightUnit,
            defaultRestSeconds = uiState.defaultRestSeconds,
            onCopyProgram = { onCopyTemplateProgram(previewTemplate) },
            onCopyRoutine = onCopyTemplateRoutine,
            onDismiss = onDismissTemplateProgram
        )
    }
}

@Composable
private fun FolderSectionHeader(
    title: String,
    routineCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = colors.surface,
                    shape = RoundedCornerShape(FerroTheme.radius.md)
                )
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = spacing.space3, vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter =
                painterResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                ),
            contentDescription =
                stringResource(
                    if (isExpanded) {
                        R.string.routines_folder_collapse_cd
                    } else {
                        R.string.routines_folder_expand_cd
                    }
                ),
            modifier = Modifier.size(FerroTheme.sizes.iconLg),
            tint = colors.inkMuted
        )
        Spacer(modifier = Modifier.width(spacing.space2))
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            modifier = Modifier.size(FerroTheme.sizes.iconMd),
            tint = colors.brasaText
        )
        Spacer(modifier = Modifier.width(spacing.space2))
        Text(
            text = title,
            style = FerroTheme.typography.heading,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = routineCount.toString(),
            style = FerroTheme.typography.number,
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = spacing.space2)
        )
        if (onRename != null) {
            FerroIconButton(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.routines_folder_rename_cd),
                onClick = onRename,
                tint = colors.inkMuted,
                iconSize = FerroTheme.sizes.iconMd
            )
        }
        if (onDelete != null) {
            FerroIconButton(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.routines_folder_delete_cd),
                onClick = onDelete,
                tint = colors.danger,
                iconSize = FerroTheme.sizes.iconMd
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineSummaryCard(
    summary: RoutineSummaryUi,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveFolder: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val routine = summary.routineWithExercises.routine

    val exercisesCountText =
        pluralStringResource(
            R.plurals.routine_exercises_count,
            summary.exerciseCount,
            summary.exerciseCount
        )
    val setsCountText =
        pluralStringResource(
            R.plurals.routine_sets_count,
            summary.totalSetCount,
            summary.totalSetCount
        )

    FerroCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
            }
            FerroIconButton(
                painter = painterResource(R.drawable.ic_arrow_upward),
                contentDescription = stringResource(R.string.routine_move_up_cd),
                onClick = onMoveUp,
                enabled = summary.canMoveUp,
                tint = colors.inkMuted,
                iconSize = FerroTheme.sizes.iconMd
            )
            FerroIconButton(
                painter = painterResource(R.drawable.ic_arrow_downward),
                contentDescription = stringResource(R.string.routine_move_down_cd),
                onClick = onMoveDown,
                enabled = summary.canMoveDown,
                tint = colors.inkMuted,
                iconSize = FerroTheme.sizes.iconMd
            )
        }

        if (summary.exerciseNamesSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = summary.exerciseNamesSummary,
                style = FerroTheme.typography.caption,
                color = colors.inkSubtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(spacing.space3))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            FerroChip(
                label = stringResource(R.string.routine_edit_action),
                selected = false,
                onClick = onEdit
            )
            FerroChip(
                label = stringResource(R.string.routine_duplicate_action),
                selected = false,
                onClick = onDuplicate
            )
            FerroChip(
                label = stringResource(R.string.routine_move_folder_action),
                selected = false,
                onClick = onMoveFolder
            )
            FerroChip(
                label = stringResource(R.string.routine_delete_action),
                selected = false,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun TemplateProgramCard(
    program: RoutineTemplateProgram,
    isSpanish: Boolean,
    onOpen: () -> Unit,
    onCopyProgram: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val daysText =
        pluralStringResource(
            R.plurals.routine_days_count,
            program.routines.size,
            program.routines.size
        )
    val routineNames =
        program.routines.joinToString(" · ") { it.localizedName(isSpanish) }

    FerroCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = program.localizedName(isSpanish),
                style = FerroTheme.typography.heading,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = daysText.uppercase(),
                style = FerroTheme.typography.label,
                color = colors.brasaText
            )
        }
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = program.localizedDescription(isSpanish),
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = routineNames,
            style = FerroTheme.typography.caption,
            color = colors.inkSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(spacing.space3))
        FerroButton(
            text = stringResource(R.string.routine_template_copy_program),
            onClick = onCopyProgram,
            variant = FerroButtonVariant.Secondary,
            leadingIcon = painterResource(R.drawable.ic_content_copy),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderEditorSheet(title: String, initialName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var showError by rememberSaveable { mutableStateOf(false) }

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = title
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4)
        ) {
            Text(
                text = stringResource(R.string.routines_folder_name_label).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = spacing.space2)
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (showError && it.isNotBlank()) showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.routines_folder_name_placeholder),
                        style = FerroTheme.typography.body
                    )
                },
                isError = showError,
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
                        errorBorderColor = colors.danger,
                        cursorColor = colors.brasaText,
                        focusedPlaceholderColor = colors.inkSubtle,
                        unfocusedPlaceholderColor = colors.inkSubtle
                    )
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.routines_folder_name_error),
                    style = FerroTheme.typography.caption,
                    color = colors.danger,
                    modifier = Modifier.padding(top = spacing.space1)
                )
            }
            Spacer(modifier = Modifier.height(spacing.space4))
            FerroButton(
                text = stringResource(R.string.exercise_editor_save),
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        showError = true
                    } else {
                        onSave(trimmed)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveRoutineToFolderSheet(
    currentFolderId: String?,
    folders: List<RoutineFolder>,
    onSelectFolder: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.routine_move_folder_title)
    ) {
        FerroListItem(
            title = stringResource(R.string.routines_uncategorized_folder),
            onClick = { onSelectFolder(null) },
            trailingContent = {
                if (currentFolderId == null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconMd),
                        tint = colors.brasaText
                    )
                }
            }
        )
        folders.forEach { folder ->
            FerroDivider()
            FerroListItem(
                title = folder.name,
                onClick = { onSelectFolder(folder.id) },
                trailingContent = {
                    if (currentFolderId == folder.id) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconMd),
                            tint = colors.brasaText
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutinePreviewSheet(
    routineWithExercises: RoutineWithExercises,
    exercisesById: Map<String, Exercise>,
    isSpanish: Boolean,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    onStartWorkout: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveFolder: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val routine = routineWithExercises.routine

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = routine.name
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            if (routine.notes.isNotBlank()) {
                Text(
                    text = routine.notes,
                    style = FerroTheme.typography.body,
                    color = colors.inkMuted
                )
            }

            FerroButton(
                text = stringResource(R.string.routine_preview_start_workout),
                onClick = onStartWorkout,
                variant = FerroButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                FerroChip(
                    label = stringResource(R.string.routine_edit_action),
                    selected = false,
                    onClick = onEdit
                )
                FerroChip(
                    label = stringResource(R.string.routine_duplicate_action),
                    selected = false,
                    onClick = onDuplicate
                )
                FerroChip(
                    label = stringResource(R.string.routine_move_folder_action),
                    selected = false,
                    onClick = onMoveFolder
                )
                FerroChip(
                    label = stringResource(R.string.routine_delete_action),
                    selected = false,
                    onClick = onDelete
                )
            }

            FerroDivider()

            routineWithExercises.exercises.forEach { exerciseWithSets ->
                val routineExercise = exerciseWithSets.routineExercise
                val exercise = exercisesById[routineExercise.exerciseId]
                val exerciseName = exercise?.localizedName(isSpanish) ?: routineExercise.exerciseId
                val restSec = routineExercise.restSeconds ?: defaultRestSeconds
                val normalSetNumbers = computeNormalSetNumbers(exerciseWithSets.sets.map { it.setType })

                PreviewExerciseItem(
                    exerciseName = exerciseName,
                    supersetGroup = routineExercise.supersetGroup,
                    restSeconds = restSec,
                    notes = routineExercise.notes,
                    setsContent = {
                        exerciseWithSets.sets.forEachIndexed { index, set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                            ) {
                                SetTypeBadge(
                                    type = set.setType.toFerroSetType(),
                                    setNumber = normalSetNumbers.getOrElse(index) { index + 1 }
                                )
                                Text(
                                    text =
                                        formatSetTargetSummary(
                                            targetWeightKg = set.targetWeightKg,
                                            targetRepsMin = set.targetRepsMin,
                                            targetRepsMax = set.targetRepsMax,
                                            targetDurationS = set.targetDurationS,
                                            targetDistanceM = set.targetDistanceM,
                                            weightUnit = weightUnit,
                                            isSpanish = isSpanish
                                        ),
                                    style = FerroTheme.typography.number,
                                    color = colors.ink
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateProgramPreviewSheet(
    program: RoutineTemplateProgram,
    exercisesById: Map<String, Exercise>,
    isSpanish: Boolean,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    onCopyProgram: () -> Unit,
    onCopyRoutine: (RoutineTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        title = program.localizedName(isSpanish)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Text(
                text = program.localizedDescription(isSpanish),
                style = FerroTheme.typography.body,
                color = colors.inkMuted
            )

            FerroButton(
                text = stringResource(R.string.routine_template_copy_program),
                onClick = onCopyProgram,
                variant = FerroButtonVariant.Primary,
                leadingIcon = painterResource(R.drawable.ic_content_copy),
                modifier = Modifier.fillMaxWidth()
            )

            program.routines.forEach { templateRoutine ->
                FerroCard(raised = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = templateRoutine.localizedName(isSpanish),
                                style = FerroTheme.typography.heading,
                                color = colors.ink
                            )
                            val notes = templateRoutine.localizedNotes(isSpanish)
                            if (notes.isNotBlank()) {
                                Text(
                                    text = notes,
                                    style = FerroTheme.typography.caption,
                                    color = colors.inkMuted
                                )
                            }
                        }
                        FerroButton(
                            text = stringResource(R.string.routine_template_copy_routine),
                            onClick = { onCopyRoutine(templateRoutine) },
                            variant = FerroButtonVariant.Secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    templateRoutine.exercises.forEach { templateExercise ->
                        val exerciseName =
                            exercisesById[templateExercise.exerciseId]?.localizedName(isSpanish)
                                ?: templateExercise.exerciseId
                        val restSec = templateExercise.restSeconds ?: defaultRestSeconds
                        val normalNumbers = computeNormalSetNumbers(templateExercise.sets.map { it.setType })

                        PreviewExerciseItem(
                            exerciseName = exerciseName,
                            supersetGroup = templateExercise.supersetGroup,
                            restSeconds = restSec,
                            notes = templateExercise.localizedNotes(isSpanish),
                            setsContent = {
                                templateExercise.sets.forEachIndexed { index, set ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                                    ) {
                                        SetTypeBadge(
                                            type = set.setType.toFerroSetType(),
                                            setNumber = normalNumbers.getOrElse(index) { index + 1 }
                                        )
                                        Text(
                                            text =
                                                formatSetTargetSummary(
                                                    targetWeightKg = set.targetWeightKg,
                                                    targetRepsMin = set.targetRepsMin,
                                                    targetRepsMax = set.targetRepsMax,
                                                    targetDurationS = set.targetDurationS,
                                                    targetDistanceM = set.targetDistanceM,
                                                    weightUnit = weightUnit,
                                                    isSpanish = isSpanish
                                                ),
                                            style = FerroTheme.typography.number,
                                            color = colors.ink
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(spacing.space2))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewExerciseItem(
    exerciseName: String,
    supersetGroup: String?,
    restSeconds: Int,
    notes: String,
    setsContent: @Composable () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

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
                Text(
                    text = stringResource(R.string.routine_superset_badge_format, supersetGroup).uppercase(),
                    style = FerroTheme.typography.label,
                    color = supersetAccentColor(supersetGroup)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseName,
                    style = FerroTheme.typography.bodyStrong,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.routine_preview_rest_format, formatRestMinSec(restSeconds)),
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }
            if (notes.isNotBlank()) {
                Text(
                    text = notes,
                    style = FerroTheme.typography.caption,
                    color = colors.inkSubtle
                )
            }
            setsContent()
        }
    }
}

@FerroPreviews
@Composable
private fun RoutinesScreenPreview() {
    FerroTheme {
        RoutinesContent(
            uiState = RoutinesUiState(),
            onOpenExerciseLibrary = {},
            onCreateRoutine = {},
            onEditRoutine = {},
            onStartWorkout = {},
            onToggleFolderExpanded = {},
            onToggleUncategorizedExpanded = {},
            onOpenCreateFolder = {},
            onOpenRenameFolder = {},
            onDismissFolderEditor = {},
            onSaveFolder = {},
            onRequestDeleteFolder = {},
            onDismissDeleteFolder = {},
            onConfirmDeleteFolder = {},
            onMoveRoutine = { _, _ -> },
            onRequestMoveRoutineToFolder = {},
            onDismissMoveRoutineToFolder = {},
            onConfirmMoveRoutineToFolder = {},
            onDuplicateRoutine = { _, _ -> },
            onRequestDeleteRoutine = {},
            onDismissDeleteRoutine = {},
            onConfirmDeleteRoutine = {},
            onOpenRoutinePreview = {},
            onDismissRoutinePreview = {},
            onOpenTemplateProgram = {},
            onDismissTemplateProgram = {},
            onCopyTemplateProgram = {},
            onCopyTemplateRoutine = {}
        )
    }
}
