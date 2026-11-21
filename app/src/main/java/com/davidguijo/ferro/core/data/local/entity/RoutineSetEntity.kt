package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.davidguijo.ferro.core.domain.model.SetType

@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineExerciseId"])
    ]
)
internal data class RoutineSetEntity(
    @PrimaryKey val id: String,
    val routineExerciseId: String,
    val position: Int,
    val setType: SetType,
    val targetWeightKg: Double?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetDurationS: Int?,
    val targetDistanceM: Double?,
    val createdAt: Long,
    val updatedAt: Long
)
