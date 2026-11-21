package com.davidguijo.ferro.core.data.mapper

import com.davidguijo.ferro.core.data.local.entity.ExerciseEntity
import com.davidguijo.ferro.core.domain.model.Exercise

internal fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    nameEs = nameEs,
    nameEn = nameEn,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    trackingType = trackingType,
    instructionsEs = instructionsEs,
    instructionsEn = instructionsEn,
    isCustom = isCustom,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    nameEs = nameEs,
    nameEn = nameEn,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    trackingType = trackingType,
    instructionsEs = instructionsEs,
    instructionsEn = instructionsEn,
    isCustom = isCustom,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt
)
