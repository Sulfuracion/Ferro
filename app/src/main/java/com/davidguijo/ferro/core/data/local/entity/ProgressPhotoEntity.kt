package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "progress_photos",
    foreignKeys = [
        ForeignKey(
            entity = BodyMeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["measurementId"]),
        Index(value = ["takenAt"])
    ]
)
internal data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val takenAt: Long,
    val fileName: String,
    val pose: String,
    val measurementId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
