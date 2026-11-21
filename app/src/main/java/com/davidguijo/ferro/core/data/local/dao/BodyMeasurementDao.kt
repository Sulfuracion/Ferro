package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.BodyMeasurementEntity
import com.davidguijo.ferro.core.data.local.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY measuredAt DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY measuredAt DESC LIMIT 1")
    fun observeLatestMeasurement(): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurements WHERE id = :id")
    suspend fun getMeasurementById(id: String): BodyMeasurementEntity?

    @Upsert
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: String)

    @Query("SELECT * FROM progress_photos ORDER BY takenAt DESC")
    fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photos WHERE measurementId = :measurementId ORDER BY takenAt DESC")
    fun observePhotosForMeasurement(measurementId: String): Flow<List<ProgressPhotoEntity>>

    @Upsert
    suspend fun upsertPhoto(photo: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)
}
