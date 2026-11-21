package com.davidguijo.ferro.feature.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.localizedInstructions
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import kotlinx.serialization.Serializable

/** Detalle de un ejercicio de la biblioteca. */
@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

private enum class ExerciseDetailTab(@StringRes val labelRes: Int) {
    INFO(R.string.exercise_tab_info),
    HISTORY(R.string.exercise_tab_history),
    CHARTS(R.string.exercise_tab_charts)
}

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExerciseDetailContent(
        uiState = uiState,
        onBack = onBack,
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.exercise_history_empty_title),
                            description = stringResource(R.string.exercise_history_empty_description),
                            icon = painterResource(R.drawable.ic_calendar_month)
                        )
                    }

                ExerciseDetailTab.CHARTS ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.exercise_charts_empty_title),
                            description = stringResource(R.string.exercise_charts_empty_description),
                            icon = painterResource(R.drawable.ic_bar_chart)
                        )
                    }
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
