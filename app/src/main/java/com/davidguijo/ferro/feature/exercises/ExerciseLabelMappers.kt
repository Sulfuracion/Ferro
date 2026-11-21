package com.davidguijo.ferro.feature.exercises

import androidx.annotation.StringRes
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType

@StringRes
internal fun Muscle.labelRes(): Int = when (this) {
    Muscle.CHEST -> R.string.muscle_chest
    Muscle.LATS -> R.string.muscle_lats
    Muscle.UPPER_BACK -> R.string.muscle_upper_back
    Muscle.LOWER_BACK -> R.string.muscle_lower_back
    Muscle.TRAPS -> R.string.muscle_traps
    Muscle.SHOULDERS -> R.string.muscle_shoulders
    Muscle.BICEPS -> R.string.muscle_biceps
    Muscle.TRICEPS -> R.string.muscle_triceps
    Muscle.FOREARMS -> R.string.muscle_forearms
    Muscle.ABS -> R.string.muscle_abs
    Muscle.OBLIQUES -> R.string.muscle_obliques
    Muscle.QUADS -> R.string.muscle_quads
    Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
    Muscle.GLUTES -> R.string.muscle_glutes
    Muscle.CALVES -> R.string.muscle_calves
    Muscle.ADDUCTORS -> R.string.muscle_adductors
    Muscle.ABDUCTORS -> R.string.muscle_abductors
    Muscle.FULL_BODY -> R.string.muscle_full_body
    Muscle.CARDIO -> R.string.muscle_cardio
}

@StringRes
internal fun Equipment.labelRes(): Int = when (this) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.BAND -> R.string.equipment_band
    Equipment.PLATE -> R.string.equipment_plate
    Equipment.EZ_BAR -> R.string.equipment_ez_bar
    Equipment.TRAP_BAR -> R.string.equipment_trap_bar
    Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
    Equipment.BENCH -> R.string.equipment_bench
    Equipment.PULL_UP_BAR -> R.string.equipment_pull_up_bar
    Equipment.CARDIO_MACHINE -> R.string.equipment_cardio_machine
    Equipment.OTHER -> R.string.equipment_other
}

@StringRes
internal fun TrackingType.labelRes(): Int = when (this) {
    TrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
    TrackingType.BODYWEIGHT_REPS -> R.string.tracking_bodyweight_reps
    TrackingType.WEIGHTED_BODYWEIGHT -> R.string.tracking_weighted_bodyweight
    TrackingType.ASSISTED_BODYWEIGHT -> R.string.tracking_assisted_bodyweight
    TrackingType.DURATION -> R.string.tracking_duration
    TrackingType.DISTANCE_DURATION -> R.string.tracking_distance_duration
}
