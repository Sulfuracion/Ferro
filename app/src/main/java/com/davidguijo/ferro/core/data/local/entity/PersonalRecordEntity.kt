package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.davidguijo.ferro.core.domain.model.RecordType

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["workoutSetId"]),
        Index(value = ["exerciseId", "recordType"])
    ]
)
internal data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val recordType: RecordType,
    val value: Double,
    val workoutSetId: String,
    val achievedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)
