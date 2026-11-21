package com.davidguijo.ferro.feature.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType

@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onSelectExercises: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExerciseLibraryContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChanged = viewModel::onQueryChanged,
        onMuscleSelected = viewModel::onMuscleSelected,
        onEquipmentSelected = viewModel::onEquipmentSelected,
        onToggleShowArchived = viewModel::onToggleShowArchived,
        onExerciseClick = { exercise ->
            when (uiState.selectionMode) {
                ExerciseSelectionMode.NONE -> onOpenExercise(exercise.id)
                ExerciseSelectionMode.SINGLE -> onSelectExercises(listOf(exercise.id))
                ExerciseSelectionMode.MULTIPLE -> viewModel.onToggleExerciseSelection(exercise.id)
            }
        },
        onConfirmSelection = {
            onSelectExercises(uiState.selectedIds.toList())
        },
        onOpenCreateSheet = viewModel::onOpenCreateSheet,
        onDismissCreateSheet = viewModel::onDismissCreateSheet,
        onCreateCustomExercise = viewModel::onCreateCustomExercise,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ExerciseLibraryContent(
    uiState: ExerciseLibraryUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onMuscleSelected: (Muscle?) -> Unit,
    onEquipmentSelected: (Equipment?) -> Unit,
    onToggleShowArchived: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onConfirmSelection: () -> Unit,
    onOpenCreateSheet: () -> Unit,
    onDismissCreateSheet: () -> Unit,
    onCreateCustomExercise: (CustomExerciseDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    val titleRes =
        if (uiState.selectionMode == ExerciseSelectionMode.NONE) {
            R.string.exercise_library_title
        } else {
            R.string.exercise_library_select_title
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
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.exercise_library_create_cd),
                        onClick = onOpenCreateSheet
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
            if (uiState.selectionMode == ExerciseSelectionMode.MULTIPLE && uiState.selectedIds.isNotEmpty()) {
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
                            text =
                                pluralStringResource(
                                    R.plurals.exercise_library_add_selected,
                                    uiState.selectedIds.size,
                                    uiState.selectedIds.size
                                ),
                            onClick = onConfirmSelection,
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            SearchAndFilterHeader(
                query = uiState.query,
                selectedMuscle = uiState.selectedMuscle,
                selectedEquipment = uiState.selectedEquipment,
                showArchived = uiState.showArchived,
                isSelectionMode = uiState.selectionMode != ExerciseSelectionMode.NONE,
                onQueryChanged = onQueryChanged,
                onMuscleSelected = onMuscleSelected,
                onEquipmentSelected = onEquipmentSelected,
                onToggleShowArchived = onToggleShowArchived
            )

            FerroDivider()

            when {
                uiState.totalAvailableCount == 0 && !uiState.showArchived -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.exercise_library_empty_title),
                            description = stringResource(R.string.exercise_library_empty_description),
                            icon = painterResource(R.drawable.ic_fitness_center)
                        )
                    }
                }

                uiState.groups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.exercise_library_no_results_title),
                            description = stringResource(R.string.exercise_library_no_results_description),
                            icon = painterResource(R.drawable.ic_fitness_center)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = spacing.space6)
                    ) {
                        uiState.groups.forEach { group ->
                            stickyHeader(key = "header_${group.letter}") {
                                LetterSectionHeader(letter = group.letter)
                            }
                            items(
                                items = group.exercises,
                                key = { it.id }
                            ) { exercise ->
                                ExerciseLibraryRow(
                                    exercise = exercise,
                                    isSpanish = uiState.isSpanish,
                                    selectionMode = uiState.selectionMode,
                                    isSelected = exercise.id in uiState.selectedIds,
                                    onClick = { onExerciseClick(exercise) }
                                )
                                FerroDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isCreateSheetVisible) {
        ExerciseEditorSheet(
            title = stringResource(R.string.exercise_editor_create_title),
            initialDraft =
                CustomExerciseDraft(
                    name = "",
                    primaryMuscle = uiState.selectedMuscle ?: Muscle.CHEST,
                    secondaryMuscles = emptyList(),
                    equipment = uiState.selectedEquipment ?: Equipment.BARBELL,
                    trackingType = TrackingType.WEIGHT_REPS,
                    instructions = ""
                ),
            onSave = onCreateCustomExercise,
            onDismiss = onDismissCreateSheet
        )
    }
}

@Composable
private fun SearchAndFilterHeader(
    query: String,
    selectedMuscle: Muscle?,
    selectedEquipment: Equipment?,
    showArchived: Boolean,
    isSelectionMode: Boolean,
    onQueryChanged: (String) -> Unit,
    onMuscleSelected: (Muscle?) -> Unit,
    onEquipmentSelected: (Equipment?) -> Unit,
    onToggleShowArchived: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space2)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4),
            placeholder = {
                Text(
                    text = stringResource(R.string.exercise_library_search_hint),
                    style = FerroTheme.typography.body
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.exercise_library_clear_search_cd),
                        onClick = { onQueryChanged("") },
                        iconSize = FerroTheme.sizes.iconMd
                    )
                }
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

        Spacer(modifier = Modifier.height(spacing.space2))

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            item(key = "muscle_all") {
                FerroChip(
                    label = stringResource(R.string.exercise_filter_all),
                    selected = selectedMuscle == null,
                    onClick = { onMuscleSelected(null) }
                )
            }
            items(Muscle.entries, key = { it.name }) { muscle ->
                FerroChip(
                    label = stringResource(muscle.labelRes()),
                    selected = selectedMuscle == muscle,
                    onClick = { onMuscleSelected(muscle) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            item(key = "equipment_all") {
                FerroChip(
                    label = stringResource(R.string.exercise_filter_all),
                    selected = selectedEquipment == null,
                    onClick = { onEquipmentSelected(null) }
                )
            }
            items(Equipment.entries, key = { it.name }) { equipment ->
                FerroChip(
                    label = stringResource(equipment.labelRes()),
                    selected = selectedEquipment == equipment,
                    onClick = { onEquipmentSelected(equipment) }
                )
            }
            if (!isSelectionMode) {
                item(key = "filter_archived") {
                    FerroChip(
                        label = stringResource(R.string.exercise_filter_archived),
                        selected = showArchived,
                        onClick = onToggleShowArchived
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterSectionHeader(letter: Char) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(FerroTheme.colors.bg)
                .padding(
                    horizontal = FerroTheme.spacing.space4,
                    vertical = FerroTheme.spacing.space1
                )
    ) {
        Text(
            text = letter.toString(),
            style = FerroTheme.typography.label,
            color = FerroTheme.colors.brasaText
        )
    }
}

@Composable
private fun ExerciseLibraryRow(
    exercise: Exercise,
    isSpanish: Boolean,
    selectionMode: ExerciseSelectionMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val muscleText = stringResource(exercise.primaryMuscle.labelRes())
    val equipmentText = stringResource(exercise.equipment.labelRes())

    FerroListItem(
        title = exercise.localizedName(isSpanish),
        subtitle = "$muscleText · $equipmentText",
        onClick = onClick,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
            ) {
                if (exercise.isCustom) {
                    ExerciseTag(
                        text = stringResource(R.string.exercise_custom_badge),
                        highlighted = true
                    )
                }
                if (exercise.archived) {
                    ExerciseTag(
                        text = stringResource(R.string.exercise_archived_badge),
                        highlighted = false
                    )
                }
                when (selectionMode) {
                    ExerciseSelectionMode.MULTIPLE -> SelectionCheckBox(selected = isSelected)

                    ExerciseSelectionMode.SINGLE -> Unit

                    ExerciseSelectionMode.NONE -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconMd),
                            tint = FerroTheme.colors.inkMuted
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ExerciseTag(text: String, highlighted: Boolean) {
    val colors = FerroTheme.colors
    val bgColor = if (highlighted) colors.brasaSoft else colors.surfaceRaised
    val textColor = if (highlighted) colors.brasaText else colors.inkMuted
    Box(
        modifier =
            Modifier
                .background(bgColor, RoundedCornerShape(FerroTheme.radius.sm))
                .padding(
                    horizontal = FerroTheme.spacing.space2,
                    vertical = FerroTheme.spacing.space1
                )
    ) {
        Text(
            text = text,
            style = FerroTheme.typography.caption,
            color = textColor
        )
    }
}

@Composable
private fun SelectionCheckBox(selected: Boolean) {
    val colors = FerroTheme.colors
    val shape = RoundedCornerShape(FerroTheme.radius.sm)
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .background(
                    color = if (selected) colors.brasa else colors.surfaceRaised,
                    shape = shape
                )
                .border(
                    width = 1.dp,
                    color = if (selected) colors.brasa else colors.lineStrong,
                    shape = shape
                ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(FerroTheme.sizes.iconSm),
                tint = colors.onBrasa
            )
        }
    }
}
