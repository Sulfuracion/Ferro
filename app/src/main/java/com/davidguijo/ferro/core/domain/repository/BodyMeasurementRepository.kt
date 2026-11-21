package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import kotlinx.coroutines.flow.Flow

interface BodyMeasurementRepository {
    fun observeMeasurements(): Flow<List<BodyMeasurement>>

    fun observeLatestMeasurement(): Flow<BodyMeasurement?>

    suspend fun getMeasurementById(id: String): BodyMeasurement?

    suspend fun upsertMeasurement(measurement: BodyMeasurement)

    suspend fun deleteMeasurement(id: String)

    fun observeProgressPhotos(): Flow<List<ProgressPhoto>>

    fun observePhotosForMeasurement(measurementId: String): Flow<List<ProgressPhoto>>

    suspend fun upsertPhoto(photo: ProgressPhoto)

    suspend fun deletePhoto(id: String)
}
