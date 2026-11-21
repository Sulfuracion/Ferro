package com.davidguijo.ferro.core.data.mapper

import com.davidguijo.ferro.core.data.local.entity.BodyMeasurementEntity
import com.davidguijo.ferro.core.data.local.entity.ProgressPhotoEntity
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto

internal fun BodyMeasurementEntity.toDomain(): BodyMeasurement = BodyMeasurement(
    id = id,
    measuredAt = measuredAt,
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    neckCm = neckCm,
    chestCm = chestCm,
    waistCm = waistCm,
    hipsCm = hipsCm,
    armLCm = armLCm,
    armRCm = armRCm,
    thighLCm = thighLCm,
    thighRCm = thighRCm,
    calfCm = calfCm,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun BodyMeasurement.toEntity(): BodyMeasurementEntity = BodyMeasurementEntity(
    id = id,
    measuredAt = measuredAt,
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    neckCm = neckCm,
    chestCm = chestCm,
    waistCm = waistCm,
    hipsCm = hipsCm,
    armLCm = armLCm,
    armRCm = armRCm,
    thighLCm = thighLCm,
    thighRCm = thighRCm,
    calfCm = calfCm,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun ProgressPhotoEntity.toDomain(): ProgressPhoto = ProgressPhoto(
    id = id,
    takenAt = takenAt,
    fileName = fileName,
    pose = pose,
    measurementId = measurementId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun ProgressPhoto.toEntity(): ProgressPhotoEntity = ProgressPhotoEntity(
    id = id,
    takenAt = takenAt,
    fileName = fileName,
    pose = pose,
    measurementId = measurementId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
