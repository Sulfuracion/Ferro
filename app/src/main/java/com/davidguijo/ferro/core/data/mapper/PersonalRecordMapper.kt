package com.davidguijo.ferro.core.data.mapper

import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.domain.model.PersonalRecord

internal fun PersonalRecordEntity.toDomain(): PersonalRecord = PersonalRecord(
    id = id,
    exerciseId = exerciseId,
    recordType = recordType,
    value = value,
    workoutSetId = workoutSetId,
    achievedAt = achievedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun PersonalRecord.toEntity(): PersonalRecordEntity = PersonalRecordEntity(
    id = id,
    exerciseId = exerciseId,
    recordType = recordType,
    value = value,
    workoutSetId = workoutSetId,
    achievedAt = achievedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
