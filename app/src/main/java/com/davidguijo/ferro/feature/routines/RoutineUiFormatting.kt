package com.davidguijo.ferro.feature.routines

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroSetType
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.SetType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun SetType.toFerroSetType(): FerroSetType = when (this) {
    SetType.WARMUP -> FerroSetType.Warmup
    SetType.NORMAL -> FerroSetType.Normal
    SetType.FAILURE -> FerroSetType.Failure
    SetType.DROP -> FerroSetType.Drop
}

@Composable
internal fun supersetAccentColor(group: String): Color {
    val colors = FerroTheme.colors
    val firstChar = group.firstOrNull()?.uppercaseChar() ?: 'A'
    return when ((firstChar.code - 'A'.code).mod(4)) {
        0 -> colors.setWarmup
        1 -> colors.setDrop
        2 -> colors.brasaText
        else -> colors.success
    }
}

internal fun formatRestMinSec(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

internal fun formatDistanceMeters(meters: Double, isSpanish: Boolean): String {
    val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
    val formatter =
        DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale)).apply {
            isGroupingUsed = false
        }
    return formatter.format(meters)
}

@Composable
internal fun formatSetTargetSummary(
    targetWeightKg: Double?,
    targetRepsMin: Int?,
    targetRepsMax: Int?,
    targetDurationS: Int?,
    targetDistanceM: Double?,
    weightUnit: WeightUnit,
    isSpanish: Boolean
): String {
    val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
    val repsText =
        when {
            targetRepsMin != null && targetRepsMax != null && targetRepsMin != targetRepsMax ->
                pluralStringResource(
                    R.plurals.routine_set_summary_reps_range,
                    targetRepsMax,
                    targetRepsMin,
                    targetRepsMax
                )

            targetRepsMin != null ->
                pluralStringResource(
                    R.plurals.routine_set_summary_reps_fixed,
                    targetRepsMin,
                    targetRepsMin
                )

            targetRepsMax != null ->
                pluralStringResource(
                    R.plurals.routine_set_summary_reps_fixed,
                    targetRepsMax,
                    targetRepsMax
                )

            else -> null
        }

    return when {
        targetDistanceM != null && targetDurationS != null ->
            stringResource(
                R.string.routine_set_summary_distance_duration,
                formatDistanceMeters(targetDistanceM, isSpanish),
                targetDurationS
            )

        targetDurationS != null ->
            stringResource(R.string.routine_set_summary_duration_s, targetDurationS)

        targetWeightKg != null && repsText != null -> {
            val weightText =
                Units.formatWeight(
                    weightKg = targetWeightKg,
                    unit = weightUnit,
                    locale = locale,
                    includeUnit = true
                )
            stringResource(R.string.routine_set_summary_weight_reps, weightText, repsText)
        }

        targetWeightKg != null ->
            Units.formatWeight(
                weightKg = targetWeightKg,
                unit = weightUnit,
                locale = locale,
                includeUnit = true
            )

        repsText != null -> repsText

        else -> "—"
    }
}
