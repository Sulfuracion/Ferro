package com.davidguijo.ferro.core.data.repository

import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.PersonalRecordDao
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.domain.BrokenRecord
import com.davidguijo.ferro.core.domain.Records
import com.davidguijo.ferro.core.domain.model.ExerciseRecordDetail
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomPersonalRecordRepository
@Inject
constructor(
    private val personalRecordDao: PersonalRecordDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val clock: Clock
) : PersonalRecordRepository {
    override fun observeRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>> =
        personalRecordDao.observeRecordsForExercise(exerciseId).map { list -> list.map { it.toDomain() } }

    override fun observeRecordDetailsForExercise(exerciseId: String): Flow<List<ExerciseRecordDetail>> =
        personalRecordDao.observeRecordDetailsForExercise(exerciseId).map { rows ->
            rows.map { row ->
                ExerciseRecordDetail(
                    record =
                        PersonalRecord(
                            id = row.id,
                            exerciseId = row.exerciseId,
                            recordType = row.recordType,
                            value = row.value,
                            workoutSetId = row.workoutSetId,
                            achievedAt = row.achievedAt,
                            createdAt = row.createdAt,
                            updatedAt = row.updatedAt
                        ),
                    weightKg = row.weightKg,
                    reps = row.reps,
                    durationS = row.durationS,
                    distanceM = row.distanceM
                )
            }
        }

    override fun observeRecordsForWorkout(workoutId: String): Flow<List<PersonalRecord>> =
        personalRecordDao.observeRecordsForWorkout(workoutId).map { list -> list.map { it.toDomain() } }

    override fun observeRecordCountsByWorkout(): Flow<Map<String, Int>> =
        personalRecordDao.observeRecordCountsByWorkout().map { rows ->
            rows.associate { it.workoutId to it.recordCount }
        }

    override suspend fun getLatestRecord(exerciseId: String, recordType: RecordType): PersonalRecord? =
        personalRecordDao.getLatestRecord(exerciseId = exerciseId, recordType = recordType)?.toDomain()

    override suspend fun getRecordsForWorkout(workoutId: String): List<PersonalRecord> =
        personalRecordDao.getRecordsForWorkout(workoutId).map { it.toDomain() }

    override suspend fun upsertRecord(record: PersonalRecord) {
        personalRecordDao.upsert(record.toEntity())
    }

    override suspend fun upsertRecords(records: List<PersonalRecord>) {
        personalRecordDao.upsertAll(records.map { it.toEntity() })
    }

    override suspend fun deleteRecord(id: String) {
        personalRecordDao.deleteById(id)
    }

    override suspend fun evaluateAndSaveRecordsForCompletedWorkout(
        completedWorkout: WorkoutWithExercises
    ): List<BrokenRecord> {
        val exerciseIds =
            completedWorkout.exercises
                .map { it.workoutExercise.exerciseId }
                .distinct()
        if (exerciseIds.isEmpty()) return emptyList()

        val trackingTypesById = mutableMapOf<String, TrackingType>()
        val historicalSetsById = mutableMapOf<String, List<WorkoutSet>>()

        for (exerciseId in exerciseIds) {
            val exercise = exerciseDao.getById(exerciseId)
            trackingTypesById[exerciseId] = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
            val pastSessions =
                workoutDao.getExerciseHistory(
                    exerciseId = exerciseId,
                    excludeWorkoutId = completedWorkout.workout.id
                )
            historicalSetsById[exerciseId] =
                pastSessions.flatMap { session ->
                    session.toDomain().sets
                }
        }

        val achievedAt = completedWorkout.workout.endedAt ?: clock.millis()
        val brokenRecords =
            Records.detectNewRecordsForWorkout(
                completedWorkout = completedWorkout,
                trackingTypesByExerciseId = trackingTypesById,
                historicalSetsByExerciseId = historicalSetsById,
                achievedAtMillis = achievedAt
            )

        if (brokenRecords.isNotEmpty()) {
            personalRecordDao.upsertAll(brokenRecords.map { it.record.toEntity() })
        }
        return brokenRecords
    }

    override suspend fun recalculateRecords(exerciseIds: Set<String>) {
        val targetExerciseIds =
            if (exerciseIds.isEmpty()) {
                personalRecordDao.deleteAll()
                workoutDao.getAllDistinctWorkoutExerciseIds().toSet()
            } else {
                exerciseIds
            }

        for (exerciseId in targetExerciseIds) {
            if (exerciseIds.isNotEmpty()) {
                personalRecordDao.deleteByExerciseId(exerciseId)
            }
            val exercise = exerciseDao.getById(exerciseId)
            val trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
            val sessions =
                workoutDao
                    .getExerciseHistory(exerciseId = exerciseId)
                    .map { it.toDomain() }
            val rebuiltRecords =
                Records.recalculateRecordsForExerciseHistory(
                    exerciseId = exerciseId,
                    trackingType = trackingType,
                    sessions = sessions
                )
            if (rebuiltRecords.isNotEmpty()) {
                personalRecordDao.upsertAll(rebuiltRecords.map { it.toEntity() })
            }
        }
    }
}
