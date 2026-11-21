package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "body_measurements",
    indices = [
        Index(value = ["measuredAt"])
    ]
)
internal data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
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
