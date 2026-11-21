package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["primaryMuscle"]),
        Index(value = ["equipment"]),
        Index(value = ["archived"])
    ]
)
internal data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val instructionsEs: String,
    val instructionsEn: String,
    val isCustom: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
