package com.davidguijo.ferro.core.domain.model

data class Exercise(
    val id: String,
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
