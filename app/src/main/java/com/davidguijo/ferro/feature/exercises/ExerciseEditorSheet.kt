package com.davidguijo.ferro.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroBottomSheet
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType

/** Datos introducidos en el formulario de creación/edición de ejercicio personalizado. */
data class CustomExerciseDraft(
    val name: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val instructions: String
)

/**
 * Hoja modal compartida para crear o editar un ejercicio personalizado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseEditorSheet(
    title: String,
    initialDraft: CustomExerciseDraft,
    onSave: (CustomExerciseDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf(initialDraft.name) }
    var instructions by rememberSaveable { mutableStateOf(initialDraft.instructions) }
    var primaryMuscle by rememberSaveable { mutableStateOf(initialDraft.primaryMuscle) }
    var secondaryMuscles by rememberSaveable { mutableStateOf(initialDraft.secondaryMuscles) }
    var equipment by rememberSaveable { mutableStateOf(initialDraft.equipment) }
    var trackingType by rememberSaveable { mutableStateOf(initialDraft.trackingType) }
    var showNameError by rememberSaveable { mutableStateOf(false) }

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

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = title
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space4)
        ) {
            SectionLabel(text = stringResource(R.string.exercise_editor_name_label))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (showNameError && it.isNotBlank()) showNameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.exercise_editor_name_placeholder),
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
                    text = stringResource(R.string.exercise_editor_name_error),
                    style = FerroTheme.typography.caption,
                    color = colors.danger,
                    modifier = Modifier.padding(top = spacing.space1)
                )
            }

            Spacer(modifier = Modifier.height(spacing.space4))
            SectionLabel(text = stringResource(R.string.exercise_editor_primary_muscle))
            ChipRow(
                items = Muscle.entries,
                isSelected = { it == primaryMuscle },
                label = { stringResource(it.labelRes()) },
                onClick = { selected ->
                    primaryMuscle = selected
                    secondaryMuscles = secondaryMuscles.filterNot { it == selected }
                }
            )

            Spacer(modifier = Modifier.height(spacing.space4))
            SectionLabel(text = stringResource(R.string.exercise_editor_secondary_muscles))
            ChipRow(
                items = Muscle.entries.filterNot { it == primaryMuscle },
                isSelected = { it in secondaryMuscles },
                label = { stringResource(it.labelRes()) },
                onClick = { selected ->
                    secondaryMuscles =
                        if (selected in secondaryMuscles) {
                            secondaryMuscles - selected
                        } else {
                            secondaryMuscles + selected
                        }
                }
            )

            Spacer(modifier = Modifier.height(spacing.space4))
            SectionLabel(text = stringResource(R.string.exercise_editor_equipment))
            ChipRow(
                items = Equipment.entries,
                isSelected = { it == equipment },
                label = { stringResource(it.labelRes()) },
                onClick = { equipment = it }
            )

            Spacer(modifier = Modifier.height(spacing.space4))
            SectionLabel(text = stringResource(R.string.exercise_editor_tracking_type))
            ChipRow(
                items = TrackingType.entries,
                isSelected = { it == trackingType },
                label = { stringResource(it.labelRes()) },
                onClick = { trackingType = it }
            )

            Spacer(modifier = Modifier.height(spacing.space4))
            SectionLabel(text = stringResource(R.string.exercise_editor_instructions_label))
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.exercise_editor_instructions_placeholder),
                        style = FerroTheme.typography.body
                    )
                },
                minLines = 3,
                maxLines = 6,
                textStyle = FerroTheme.typography.body,
                shape = RoundedCornerShape(FerroTheme.radius.md),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(spacing.space5))
            FerroButton(
                text = stringResource(R.string.exercise_editor_save),
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        showNameError = true
                    } else {
                        onSave(
                            CustomExerciseDraft(
                                name = trimmedName,
                                primaryMuscle = primaryMuscle,
                                secondaryMuscles = secondaryMuscles,
                                equipment = equipment,
                                trackingType = trackingType,
                                instructions = instructions.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = FerroTheme.typography.label,
        color = FerroTheme.colors.inkMuted,
        modifier = Modifier.padding(bottom = FerroTheme.spacing.space2)
    )
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onClick: (T) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2),
        contentPadding = PaddingValues(end = FerroTheme.spacing.space2)
    ) {
        items(items) { item ->
            FerroChip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onClick(item) }
            )
        }
    }
}
