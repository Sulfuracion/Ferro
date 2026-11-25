package com.davidguijo.ferro.feature.body

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroBottomSheet
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.NumberField
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.BodyMetric
import com.davidguijo.ferro.core.domain.LengthUnit
import com.davidguijo.ferro.core.domain.WeightUnit
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyMeasurementFormSheet(
    draft: BodyMeasurementFormDraft,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    onDismiss: () -> Unit,
    onUpdateDate: (Long) -> Unit,
    onUpdateField: (BodyMetric, String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSave: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val titleRes =
        if (draft.editingId == null) {
            R.string.body_add_record
        } else {
            R.string.body_edit_record
        }

    FerroBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = stringResource(titleRes)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            val formattedDate =
                Instant.ofEpochMilli(draft.measuredAt)
                    .atZone(zoneId)
                    .toLocalDate()
                    .format(dateFormatter)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = colors.lineStrong,
                            shape = RoundedCornerShape(FerroTheme.radius.md)
                        )
                        .clickable { showDatePicker = true }
                        .padding(horizontal = spacing.space3, vertical = spacing.space3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.body_form_date),
                    style = FerroTheme.typography.bodyStrong,
                    color = colors.ink
                )
                Text(
                    text = formattedDate,
                    style = FerroTheme.typography.body,
                    color = colors.brasaText
                )
            }

            val weightUnitLabel =
                stringResource(
                    if (weightUnit == WeightUnit.KG) R.string.settings_unit_kg else R.string.settings_unit_lb
                )
            val lengthUnitLabel =
                stringResource(
                    if (lengthUnit == LengthUnit.CM) R.string.body_unit_cm else R.string.body_unit_in
                )
            val pctLabel = stringResource(R.string.body_unit_pct)

            BodyMetric.entries.forEachIndexed { index, metric ->
                val unitLabel =
                    when (metric) {
                        BodyMetric.WEIGHT -> weightUnitLabel
                        BodyMetric.BODY_FAT -> pctLabel
                        else -> lengthUnitLabel
                    }
                val isError = metric in draft.fieldErrors
                val errorText =
                    if (isError) {
                        stringResource(
                            if (metric == BodyMetric.BODY_FAT) {
                                R.string.body_form_invalid_body_fat
                            } else {
                                R.string.body_form_invalid_number
                            }
                        )
                    } else {
                        null
                    }
                MetricInputRow(
                    label = stringResource(metric.labelRes()),
                    unitLabel = unitLabel,
                    value = draft.fieldTexts[metric].orEmpty(),
                    isError = isError,
                    errorText = errorText,
                    imeAction = if (index == BodyMetric.entries.lastIndex) ImeAction.Done else ImeAction.Next,
                    onValueChange = { onUpdateField(metric, it) },
                    locale = locale
                )
            }

            OutlinedTextField(
                value = draft.notes,
                onValueChange = onUpdateNotes,
                label = { Text(text = stringResource(R.string.body_form_notes)) },
                placeholder = { Text(text = stringResource(R.string.body_form_notes_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.focus,
                        unfocusedBorderColor = colors.lineStrong,
                        focusedLabelColor = colors.focus,
                        unfocusedLabelColor = colors.inkMuted,
                        cursorColor = colors.focus,
                        focusedTextColor = colors.ink,
                        unfocusedTextColor = colors.ink
                    )
            )

            Spacer(modifier = Modifier.height(spacing.space1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                FerroButton(
                    text = stringResource(R.string.body_form_cancel),
                    onClick = onDismiss,
                    variant = FerroButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FerroButton(
                    text = stringResource(R.string.body_form_save),
                    onClick = onSave,
                    enabled = !draft.isSaving,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDatePicker) {
        val currentLocalDate = Instant.ofEpochMilli(draft.measuredAt).atZone(zoneId).toLocalDate()
        val initialUtcMillis = currentLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            val selectedLocalDate =
                                Instant.ofEpochMilli(selectedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            val preservedTime =
                                Instant.ofEpochMilli(draft.measuredAt).atZone(zoneId).toLocalTime()
                            val combinedMillis =
                                selectedLocalDate
                                    .atTime(if (preservedTime == LocalTime.MIDNIGHT) LocalTime.NOON else preservedTime)
                                    .atZone(zoneId)
                                    .toInstant()
                                    .toEpochMilli()
                            onUpdateDate(combinedMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.body_form_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.body_form_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MetricInputRow(
    label: String,
    unitLabel: String,
    value: String,
    isError: Boolean,
    errorText: String?,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    locale: java.util.Locale
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = FerroTheme.typography.body,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(spacing.space2))
            NumberField(
                value = value,
                onValueChange = onValueChange,
                isError = isError,
                imeAction = imeAction,
                textAlign = TextAlign.End,
                locale = locale,
                modifier = Modifier.width(104.dp)
            )
            Spacer(modifier = Modifier.width(spacing.space2))
            Text(
                text = unitLabel,
                style = FerroTheme.typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.width(28.dp)
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = FerroTheme.typography.caption,
                color = colors.danger,
                modifier = Modifier.padding(top = spacing.space1)
            )
        }
    }
}

@StringRes
internal fun BodyMetric.labelRes(): Int = when (this) {
    BodyMetric.WEIGHT -> R.string.body_metric_weight
    BodyMetric.BODY_FAT -> R.string.body_metric_body_fat
    BodyMetric.NECK -> R.string.body_metric_neck
    BodyMetric.CHEST -> R.string.body_metric_chest
    BodyMetric.WAIST -> R.string.body_metric_waist
    BodyMetric.HIPS -> R.string.body_metric_hips
    BodyMetric.ARM_L -> R.string.body_metric_arm_l
    BodyMetric.ARM_R -> R.string.body_metric_arm_r
    BodyMetric.THIGH_L -> R.string.body_metric_thigh_l
    BodyMetric.THIGH_R -> R.string.body_metric_thigh_r
    BodyMetric.CALF -> R.string.body_metric_calf
}
