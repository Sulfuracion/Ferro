package com.davidguijo.ferro.core.data.local

import androidx.room3.ColumnTypeConverter
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutStatus

internal object FerroTypeConverters {
    @ColumnTypeConverter
    fun fromSetType(value: SetType): String = value.name

    @ColumnTypeConverter
    fun toSetType(value: String): SetType = SetType.valueOf(value)

    @ColumnTypeConverter
    fun fromTrackingType(value: TrackingType): String = value.name

    @ColumnTypeConverter
    fun toTrackingType(value: String): TrackingType = TrackingType.valueOf(value)

    @ColumnTypeConverter
    fun fromMuscle(value: Muscle): String = value.name

    @ColumnTypeConverter
    fun toMuscle(value: String): Muscle = Muscle.valueOf(value)

    @ColumnTypeConverter
    fun fromMuscleList(values: List<Muscle>): String = values.joinToString(separator = ",") { it.name }

    @ColumnTypeConverter
    fun toMuscleList(value: String): List<Muscle> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(",").map { Muscle.valueOf(it.trim()) }
    }

    @ColumnTypeConverter
    fun fromEquipment(value: Equipment): String = value.name

    @ColumnTypeConverter
    fun toEquipment(value: String): Equipment = Equipment.valueOf(value)

    @ColumnTypeConverter
    fun fromRecordType(value: RecordType): String = value.name

    @ColumnTypeConverter
    fun toRecordType(value: String): RecordType = RecordType.valueOf(value)

    @ColumnTypeConverter
    fun fromWorkoutStatus(value: WorkoutStatus): String = value.name

    @ColumnTypeConverter
    fun toWorkoutStatus(value: String): WorkoutStatus = WorkoutStatus.valueOf(value)
}
