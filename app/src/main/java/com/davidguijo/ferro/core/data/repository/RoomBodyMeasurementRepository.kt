package com.davidguijo.ferro.core.data.repository

import com.davidguijo.ferro.core.data.local.dao.BodyMeasurementDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomBodyMeasurementRepository
@Inject
constructor(private val bodyMeasurementDao: BodyMeasurementDao) :
    BodyMeasurementRepository {
    override fun observeMeasurements(): Flow<List<BodyMeasurement>> =
        bodyMeasurementDao.observeMeasurements().map { list -> list.map { it.toDomain() } }

    override fun observeLatestMeasurement(): Flow<BodyMeasurement?> =
        bodyMeasurementDao.observeLatestMeasurement().map { it?.toDomain() }

    override suspend fun getMeasurementById(id: String): BodyMeasurement? =
        bodyMeasurementDao.getMeasurementById(id)?.toDomain()

    override suspend fun upsertMeasurement(measurement: BodyMeasurement) {
        bodyMeasurementDao.upsertMeasurement(measurement.toEntity())
    }

    override suspend fun deleteMeasurement(id: String) {
        bodyMeasurementDao.deleteMeasurementById(id)
    }

    override fun observeProgressPhotos(): Flow<List<ProgressPhoto>> =
        bodyMeasurementDao.observeProgressPhotos().map { list -> list.map { it.toDomain() } }

    override fun observePhotosForMeasurement(measurementId: String): Flow<List<ProgressPhoto>> =
        bodyMeasurementDao.observePhotosForMeasurement(measurementId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertPhoto(photo: ProgressPhoto) {
        bodyMeasurementDao.upsertPhoto(photo.toEntity())
    }

    override suspend fun deletePhoto(id: String) {
        bodyMeasurementDao.deletePhotoById(id)
    }
}
