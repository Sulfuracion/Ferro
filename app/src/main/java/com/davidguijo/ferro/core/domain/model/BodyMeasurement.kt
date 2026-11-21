package com.davidguijo.ferro.core.domain.model

data class BodyMeasurement(
    val id: String,
    val measuredAt: Long,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val neckCm: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val hipsCm: Double?,
    val armLCm: Double?,
    val armRCm: Double?,
    val thighLCm: Double?,
    val thighRCm: Double?,
    val calfCm: Double?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ProgressPhoto(
    val id: String,
    val takenAt: Long,
    val fileName: String,
    val pose: String,
    val measurementId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
