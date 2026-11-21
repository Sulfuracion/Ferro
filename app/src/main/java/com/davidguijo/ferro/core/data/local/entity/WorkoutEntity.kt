package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.davidguijo.ferro.core.domain.model.WorkoutStatus

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["startedAt"]),
        Index(value = ["status"])
    ]
)
internal data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val routineId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val notes: String,
    val status: WorkoutStatus,
    val createdAt: Long,
    val updatedAt: Long
)
