package com.davidguijo.ferro.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.resolveRestDurationSeconds
import com.davidguijo.ferro.feature.exercises.labelRes
import com.davidguijo.ferro.feature.routines.formatRestMinSec
import com.davidguijo.ferro.feature.routines.supersetAccentColor
import java.util.Locale

/** Tarjeta de un ejercicio dentro del entreno activo con tabla de series, notas y menú. */
@Composable
internal fun ActiveExerciseCard(
    exercise: ActiveExerciseUi,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    isSpanish: Boolean,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenExerciseMenu: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onToggleRpeColumn: () -> Unit,
    onOpenSetTypePicker: (String, com.davidguijo.ferro.core.domain.model.SetType) -> Unit,
    onCopyPrevious: (String) -> Unit,
    onSetWeightChanged: (String, String) -> Unit,
    onSetRepsChanged: (String, String) -> Unit,
    onSetDurationChanged: (String, String) -> Unit,
    onSetDistanceChanged: (String, String) -> Unit,
    onOpenRpePicker: (String, Double?) -> Unit,
    onToggleSetCompleted: (String) -> Unit,
    onRemoveSet: (String) -> Unit,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
    val focusManager = LocalFocusManager.current

    val fieldCount = fieldCountForTrackingType(exercise.trackingType)
    val setIds = exercise.sets.map { it.id }
    val focusGrid =
        remember(setIds, fieldCount) {
            List(setIds.size) { List(fieldCount) { FocusRequester() } }
        }

    FerroCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
        ) {
            if (exercise.supersetGroup != null) {
                Box(
                    modifier =
                        Modifier
                            .width(spacing.space1)
                            .fillMaxHeight()
                            .background(
                                color = supersetAccentColor(exercise.supersetGroup),
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                )
                Spacer(modifier = Modifier.width(spacing.space3))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (exercise.supersetGroup != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.routine_superset_badge_format,
                                exercise.supersetGroup
                            ).uppercase(),
                        style = FerroTheme.typography.label,
                        color = supersetAccentColor(exercise.supersetGroup)
                    )
                    Spacer(modifier = Modifier.height(spacing.space1))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                                .clip(RoundedCornerShape(FerroTheme.radius.sm))
                                .clickable { onOpenExerciseDetail(exercise.exerciseId) }
                                .padding(vertical = spacing.space1),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = exercise.name,
                            style = FerroTheme.typography.heading,
                            color = colors.brasaText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val restText =
                            formatRestMinSec(
                                resolveRestDurationSeconds(exercise.restSeconds, defaultRestSeconds)
                            )
                        val restSubtitle = stringResource(R.string.routine_preview_rest_format, restText)
                        val subtitle =
                            if (exercise.muscle != null) {
                                "${stringResource(exercise.muscle.labelRes())} · $restSubtitle"
                            } else {
                                restSubtitle
                            }
                        Text(
                            text = subtitle,
                            style = FerroTheme.typography.caption,
                            color = colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(spacing.space1))

                    FerroChip(
                        label = stringResource(R.string.active_exercise_rpe_toggle),
                        selected = exercise.showRpeColumn,
                        onClick = onToggleRpeColumn
                    )

                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.active_exercise_menu_cd, exercise.name),
                        onClick = onOpenExerciseMenu,
                        tint = colors.inkMuted,
                        iconSize = FerroTheme.sizes.iconMd
                    )
                }

                Spacer(modifier = Modifier.height(spacing.space2))

                OutlinedTextField(
                    value = exercise.notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.active_exercise_notes_placeholder),
                            style = FerroTheme.typography.caption
                        )
                    },
                    textStyle = FerroTheme.typography.caption,
                    maxLines = 3,
                    shape = RoundedCornerShape(FerroTheme.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.ink,
                            unfocusedTextColor = colors.ink,
                            focusedContainerColor = colors.surfaceRaised,
                            unfocusedContainerColor = colors.surfaceRaised,
                            focusedBorderColor = colors.focus,
                            unfocusedBorderColor = colors.line,
                            cursorColor = colors.focus,
                            focusedPlaceholderColor = colors.inkSubtle,
                            unfocusedPlaceholderColor = colors.inkSubtle
                        )
                )

                Spacer(modifier = Modifier.height(spacing.space3))

                ActiveSetsTableHeader(
                    trackingType = exercise.trackingType,
                    weightUnit = weightUnit,
                    showRpeColumn = exercise.showRpeColumn
                )

                Spacer(modifier = Modifier.height(spacing.space1))

                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    exercise.sets.forEachIndexed { setIndex, set ->
                        val nextRowFirstRequester = focusGrid.getOrNull(setIndex + 1)?.firstOrNull()
                        key(set.id) {
                            ActiveSetRow(
                                set = set,
                                trackingType = exercise.trackingType,
                                showRpeColumn = exercise.showRpeColumn,
                                locale = locale,
                                focusRequesters = focusGrid[setIndex],
                                nextFocusRequester = nextRowFirstRequester,
                                onDoneIme = { focusManager.clearFocus() },
                                onBadgeClick = { onOpenSetTypePicker(set.id, set.setType) },
                                onCopyPrevious = { onCopyPrevious(set.id) },
                                onWeightChanged = { value -> onSetWeightChanged(set.id, value) },
                                onRepsChanged = { value -> onSetRepsChanged(set.id, value) },
                                onDurationChanged = { value -> onSetDurationChanged(set.id, value) },
                                onDistanceChanged = { value -> onSetDistanceChanged(set.id, value) },
                                onRpeClick = { onOpenRpePicker(set.id, set.rpeValue) },
                                onToggleCompleted = { onToggleSetCompleted(set.id) },
                                onRemoveSet = { onRemoveSet(set.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space2))

                FerroButton(
                    text = stringResource(R.string.routine_editor_add_set),
                    onClick = onAddSet,
                    variant = FerroButtonVariant.Ghost,
                    leadingIcon = painterResource(R.drawable.ic_add),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
