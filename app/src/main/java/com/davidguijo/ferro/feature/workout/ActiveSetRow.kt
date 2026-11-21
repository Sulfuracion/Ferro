package com.davidguijo.ferro.feature.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.NumberField
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.feature.routines.toFerroSetType
import java.util.Locale
import kotlinx.coroutines.launch

/** Cabecera de la tabla de series alineada con docs/design/components/SetRow/README.md. */
@Composable
internal fun ActiveSetsTableHeader(
    trackingType: TrackingType,
    weightUnit: WeightUnit,
    showRpeColumn: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val fontScale = LocalDensity.current.fontScale
    val unitLabel =
        when (weightUnit) {
            WeightUnit.KG -> stringResource(R.string.settings_unit_kg)
            WeightUnit.LB -> stringResource(R.string.settings_unit_lb)
        }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = useCompactSetLayout(maxWidth.value, fontScale, showRpeColumn)
        if (!compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.active_set_col_set).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
                Text(
                    text = stringResource(R.string.active_set_col_previous).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
                Text(
                    text = stringResource(R.string.active_set_col_done).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
            }
            return@BoxWithConstraints
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            Text(
                text = stringResource(R.string.active_set_col_set).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(SET_COL_WIDTH)
            )
            Text(
                text = stringResource(R.string.active_set_col_previous).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            when (trackingType) {
                TrackingType.WEIGHT_REPS,
                TrackingType.WEIGHTED_BODYWEIGHT,
                TrackingType.ASSISTED_BODYWEIGHT -> {
                    val weightHeaderRes =
                        when (trackingType) {
                            TrackingType.WEIGHTED_BODYWEIGHT -> R.string.active_set_col_extra_weight
                            TrackingType.ASSISTED_BODYWEIGHT -> R.string.active_set_col_assist_weight
                            else -> R.string.active_set_col_weight
                        }
                    Text(
                        text = stringResource(weightHeaderRes, unitLabel).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(WEIGHT_COL_WIDTH)
                    )
                    Text(
                        text = stringResource(R.string.active_set_col_reps).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(REPS_COL_WIDTH)
                    )
                }

                TrackingType.BODYWEIGHT_REPS -> {
                    Text(
                        text = stringResource(R.string.active_set_col_reps).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(SINGLE_FIELD_COL_WIDTH)
                    )
                }

                TrackingType.DURATION -> {
                    Text(
                        text = stringResource(R.string.active_set_col_duration).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(SINGLE_FIELD_COL_WIDTH)
                    )
                }

                TrackingType.DISTANCE_DURATION -> {
                    Text(
                        text = stringResource(R.string.active_set_col_distance).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(WEIGHT_COL_WIDTH)
                    )
                    Text(
                        text = stringResource(R.string.active_set_col_duration).uppercase(),
                        style = FerroTheme.typography.label,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(REPS_COL_WIDTH)
                    )
                }
            }

            if (showRpeColumn) {
                Text(
                    text = stringResource(R.string.active_set_col_rpe).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(RPE_COL_WIDTH)
                )
            }

            Spacer(modifier = Modifier.width(FerroTheme.sizes.touchMin))
        }
    }
}

/**
 * Fila de serie en el entreno activo con soporte para deslizar para borrar,
 * toque en «Anterior» para copiar, foco en cadena con `ImeAction.Next` y desplazamiento automático.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ActiveSetRow(
    set: ActiveSetUi,
    trackingType: TrackingType,
    showRpeColumn: Boolean,
    locale: Locale,
    focusRequesters: List<FocusRequester>,
    nextFocusRequester: FocusRequester?,
    onDoneIme: () -> Unit,
    onBadgeClick: () -> Unit,
    onCopyPrevious: () -> Unit,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onRpeClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onRemoveSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val fontScale = LocalDensity.current.fontScale
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val rowBackground by
        animateColorAsState(
            targetValue = if (set.completed) colors.brasaSoft else Color.Transparent,
            animationSpec = tween(durationMillis = CHECK_ANIM_MS),
            label = "set_row_bg"
        )

    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        modifier =
            modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onRemoveSet()
            }
        },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = colors.danger,
                            shape = RoundedCornerShape(FerroTheme.radius.md)
                        )
                        .padding(horizontal = spacing.space4),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.active_set_delete_cd),
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.onBrasa
                )
            }
        }
    ) {
        val rowShape = RoundedCornerShape(FerroTheme.radius.md)
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(rowBackground, rowShape)
                    .padding(vertical = spacing.space1)
        ) {
            val compact = useCompactSetLayout(maxWidth.value, fontScale, showRpeColumn)

            val onFieldFocused: (Boolean) -> Unit = { focused ->
                if (focused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }

            if (compact) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = FerroTheme.sizes.rowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    Box(
                        modifier = Modifier.width(SET_COL_WIDTH),
                        contentAlignment = Alignment.Center
                    ) {
                        SetTypeBadge(
                            type = set.setType.toFerroSetType(),
                            setNumber = set.normalSetNumber,
                            onClick = onBadgeClick
                        )
                    }

                    PreviousSetCell(
                        previousLabel = set.previousLabel,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopyPrevious()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SetInputFields(
                        set = set,
                        trackingType = trackingType,
                        locale = locale,
                        focusRequesters = focusRequesters,
                        nextFocusRequester = nextFocusRequester,
                        onDoneIme = onDoneIme,
                        onFieldFocused = onFieldFocused,
                        onWeightChanged = onWeightChanged,
                        onRepsChanged = onRepsChanged,
                        onDurationChanged = onDurationChanged,
                        onDistanceChanged = onDistanceChanged,
                        useCompactWidths = true
                    )

                    if (showRpeColumn) {
                        RpeCellButton(
                            rpeLabel = set.rpeLabel,
                            completed = set.completed,
                            onClick = onRpeClick,
                            modifier = Modifier.width(RPE_COL_WIDTH)
                        )
                    }

                    SetDoneCheckbox(
                        completed = set.completed,
                        onClick = {
                            if (!set.completed) {
                                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onToggleCompleted()
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1)
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
                            setNumber = set.normalSetNumber,
                            onClick = onBadgeClick
                        )
                        PreviousSetCell(
                            previousLabel = set.previousLabel,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCopyPrevious()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (showRpeColumn) {
                            RpeCellButton(
                                rpeLabel = set.rpeLabel,
                                completed = set.completed,
                                onClick = onRpeClick,
                                modifier = Modifier.width(FerroTheme.sizes.touchMin)
                            )
                        }
                        SetDoneCheckbox(
                            completed = set.completed,
                            onClick = {
                                if (!set.completed) {
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onToggleCompleted()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        SetInputFields(
                            set = set,
                            trackingType = trackingType,
                            locale = locale,
                            focusRequesters = focusRequesters,
                            nextFocusRequester = nextFocusRequester,
                            onDoneIme = onDoneIme,
                            onFieldFocused = onFieldFocused,
                            onWeightChanged = onWeightChanged,
                            onRepsChanged = onRepsChanged,
                            onDurationChanged = onDurationChanged,
                            onDistanceChanged = onDistanceChanged,
                            useCompactWidths = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviousSetCell(previousLabel: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val hasPrevious = previousLabel != null
    val copyCd = stringResource(R.string.active_set_copy_previous_cd)

    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                .clip(RoundedCornerShape(FerroTheme.radius.sm))
                .then(
                    if (hasPrevious) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onClick)
                            .semantics { contentDescription = "$copyCd: $previousLabel" }
                    } else {
                        Modifier
                    }
                ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = previousLabel ?: stringResource(R.string.active_set_previous_none),
            style = FerroTheme.typography.caption,
            color = if (hasPrevious) colors.inkMuted else colors.inkSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SetInputFields(
    set: ActiveSetUi,
    trackingType: TrackingType,
    locale: Locale,
    focusRequesters: List<FocusRequester>,
    nextFocusRequester: FocusRequester?,
    onDoneIme: () -> Unit,
    onFieldFocused: (Boolean) -> Unit,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    useCompactWidths: Boolean
) {
    val showBorder = !set.completed

    fun Modifier.fieldModifier(index: Int, compactWidth: androidx.compose.ui.unit.Dp): Modifier {
        val base = if (useCompactWidths) this.width(compactWidth) else this.weight(1f)
        val requester = focusRequesters.getOrNull(index)
        val withRequester = if (requester != null) base.focusRequester(requester) else base
        return withRequester.onFocusChanged { onFieldFocused(it.isFocused) }
    }

    fun actionForField(index: Int, totalFields: Int): Pair<ImeAction, () -> Unit> {
        val nextWithinRow = focusRequesters.getOrNull(index + 1)
        return when {
            index + 1 < totalFields && nextWithinRow != null ->
                ImeAction.Next to {
                    try {
                        nextWithinRow.requestFocus()
                    } catch (_: IllegalStateException) {
                    }
                }

            nextFocusRequester != null ->
                ImeAction.Next to {
                    try {
                        nextFocusRequester.requestFocus()
                    } catch (_: IllegalStateException) {
                    }
                }

            else -> ImeAction.Done to onDoneIme
        }
    }

    when (trackingType) {
        TrackingType.WEIGHT_REPS,
        TrackingType.WEIGHTED_BODYWEIGHT,
        TrackingType.ASSISTED_BODYWEIGHT -> {
            val (firstAction, onFirstIme) = actionForField(0, 2)
            val (secondAction, onSecondIme) = actionForField(1, 2)
            NumberField(
                value = set.weightInput,
                onValueChange = onWeightChanged,
                placeholder = set.weightPlaceholder,
                allowDecimal = true,
                showBorder = showBorder,
                imeAction = firstAction,
                onImeAction = onFirstIme,
                locale = locale,
                modifier = Modifier.fieldModifier(0, WEIGHT_COL_WIDTH)
            )
            NumberField(
                value = set.repsInput,
                onValueChange = onRepsChanged,
                placeholder = set.repsPlaceholder,
                allowDecimal = false,
                showBorder = showBorder,
                imeAction = secondAction,
                onImeAction = onSecondIme,
                locale = locale,
                modifier = Modifier.fieldModifier(1, REPS_COL_WIDTH)
            )
        }

        TrackingType.BODYWEIGHT_REPS -> {
            val (action, onIme) = actionForField(0, 1)
            NumberField(
                value = set.repsInput,
                onValueChange = onRepsChanged,
                placeholder = set.repsPlaceholder,
                allowDecimal = false,
                showBorder = showBorder,
                imeAction = action,
                onImeAction = onIme,
                locale = locale,
                modifier = Modifier.fieldModifier(0, SINGLE_FIELD_COL_WIDTH)
            )
        }

        TrackingType.DURATION -> {
            val (action, onIme) = actionForField(0, 1)
            NumberField(
                value = set.durationInput,
                onValueChange = onDurationChanged,
                placeholder = set.durationPlaceholder,
                allowDecimal = false,
                showBorder = showBorder,
                imeAction = action,
                onImeAction = onIme,
                locale = locale,
                modifier = Modifier.fieldModifier(0, SINGLE_FIELD_COL_WIDTH)
            )
        }

        TrackingType.DISTANCE_DURATION -> {
            val (firstAction, onFirstIme) = actionForField(0, 2)
            val (secondAction, onSecondIme) = actionForField(1, 2)
            NumberField(
                value = set.distanceInput,
                onValueChange = onDistanceChanged,
                placeholder = set.distancePlaceholder,
                allowDecimal = true,
                showBorder = showBorder,
                imeAction = firstAction,
                onImeAction = onFirstIme,
                locale = locale,
                modifier = Modifier.fieldModifier(0, WEIGHT_COL_WIDTH)
            )
            NumberField(
                value = set.durationInput,
                onValueChange = onDurationChanged,
                placeholder = set.durationPlaceholder,
                allowDecimal = false,
                showBorder = showBorder,
                imeAction = secondAction,
                onImeAction = onSecondIme,
                locale = locale,
                modifier = Modifier.fieldModifier(1, REPS_COL_WIDTH)
            )
        }
    }
}

@Composable
private fun RpeCellButton(rpeLabel: String?, completed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val shape = RoundedCornerShape(FerroTheme.radius.md)
    val selectCd = stringResource(R.string.active_set_rpe_select_cd)

    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = FerroTheme.sizes.touchMin)
                .background(colors.surfaceRaised, shape)
                .then(
                    if (!completed) {
                        Modifier.border(1.dp, colors.lineStrong, shape)
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription =
                        if (rpeLabel != null) "$selectCd: $rpeLabel" else selectCd
                },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rpeLabel ?: stringResource(R.string.active_set_rpe_placeholder),
            style = FerroTheme.typography.number,
            color = if (rpeLabel != null) colors.ink else colors.inkSubtle,
            maxLines = 1
        )
    }
}

@Composable
private fun SetDoneCheckbox(completed: Boolean, onClick: () -> Unit) {
    val colors = FerroTheme.colors
    val shape = RoundedCornerShape(FerroTheme.radius.sm)
    val boxColor by
        animateColorAsState(
            targetValue = if (completed) colors.brasa else colors.surfaceRaised,
            animationSpec = tween(durationMillis = CHECK_ANIM_MS),
            label = "set_done_box"
        )
    val label =
        stringResource(
            if (completed) R.string.active_set_unmark_done_cd else R.string.active_set_mark_done_cd
        )
    val doneStateDesc = stringResource(R.string.active_set_col_done)

    Box(
        modifier =
            Modifier
                .size(FerroTheme.sizes.touchMin)
                .clip(shape)
                .clickable(role = Role.Checkbox, onClick = onClick)
                .semantics {
                    contentDescription = label
                    if (completed) {
                        stateDescription = doneStateDesc
                    }
                },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(CHECKBOX_VISUAL_SIZE)
                    .background(boxColor, shape)
                    .then(
                        if (!completed) {
                            Modifier.border(1.dp, colors.lineStrong, shape)
                        } else {
                            Modifier
                        }
                    ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(FerroTheme.sizes.iconMd),
                    tint = colors.onBrasa
                )
            }
        }
    }
}

internal fun fieldCountForTrackingType(trackingType: TrackingType): Int = when (trackingType) {
    TrackingType.WEIGHT_REPS,
    TrackingType.WEIGHTED_BODYWEIGHT,
    TrackingType.ASSISTED_BODYWEIGHT,
    TrackingType.DISTANCE_DURATION -> 2

    TrackingType.BODYWEIGHT_REPS,
    TrackingType.DURATION -> 1
}

private fun useCompactSetLayout(maxWidthDp: Float, fontScale: Float, showRpeColumn: Boolean): Boolean {
    if (fontScale >= LARGE_FONT_THRESHOLD) return false
    val minRequiredWidth = if (showRpeColumn) 356f else 300f
    return maxWidthDp >= minRequiredWidth
}

private val SET_COL_WIDTH = 40.dp
private val WEIGHT_COL_WIDTH = 72.dp
private val REPS_COL_WIDTH = 56.dp
private val SINGLE_FIELD_COL_WIDTH = 104.dp
private val RPE_COL_WIDTH = 44.dp
private val CHECKBOX_VISUAL_SIZE = 28.dp
private const val CHECK_ANIM_MS = 120
private const val LARGE_FONT_THRESHOLD = 1.5f
