package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.domain.model.RecordType
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PersonalRecordDao {
    data class WorkoutRecordCountRow(val workoutId: String, val recordCount: Int)

    data class PersonalRecordDetailRow(
        val id: String,
        val exerciseId: String,
        val recordType: RecordType,
        val value: Double,
        val workoutSetId: String,
        val achievedAt: Long,
        val createdAt: Long,
        val updatedAt: Long,
        val weightKg: Double?,
        val reps: Int?,
        val durationS: Int?,
        val distanceM: Double?
    )

    @Query(
        """
        SELECT we.workoutId AS workoutId, COUNT(pr.id) AS recordCount
        FROM personal_records AS pr
        INNER JOIN workout_sets AS ws ON ws.id = pr.workoutSetId
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        GROUP BY we.workoutId
        """
    )
    fun observeRecordCountsByWorkout(): Flow<List<WorkoutRecordCountRow>>

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun observeRecordsForExercise(exerciseId: String): Flow<List<PersonalRecordEntity>>

    @Query(
        """
        SELECT
            pr.id AS id,
            pr.exerciseId AS exerciseId,
            pr.recordType AS recordType,
            pr.value AS value,
            pr.workoutSetId AS workoutSetId,
            pr.achievedAt AS achievedAt,
            pr.createdAt AS createdAt,
            pr.updatedAt AS updatedAt,
            ws.weightKg AS weightKg,
            ws.reps AS reps,
            ws.durationS AS durationS,
            ws.distanceM AS distanceM
        FROM personal_records AS pr
        INNER JOIN workout_sets AS ws ON ws.id = pr.workoutSetId
        WHERE pr.exerciseId = :exerciseId
        ORDER BY pr.achievedAt DESC
        """
    )
    fun observeRecordDetailsForExercise(exerciseId: String): Flow<List<PersonalRecordDetailRow>>

    @Query(
        """
        SELECT * FROM personal_records
        WHERE exerciseId = :exerciseId AND recordType = :recordType
        ORDER BY value DESC, achievedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestRecord(exerciseId: String, recordType: RecordType): PersonalRecordEntity?

    @Query(
        """
        SELECT pr.* FROM personal_records AS pr
        INNER JOIN workout_sets AS ws ON ws.id = pr.workoutSetId
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        WHERE we.workoutId = :workoutId
        ORDER BY pr.achievedAt DESC
        """
    )
    fun observeRecordsForWorkout(workoutId: String): Flow<List<PersonalRecordEntity>>

    @Query(
        """
        SELECT pr.* FROM personal_records AS pr
        INNER JOIN workout_sets AS ws ON ws.id = pr.workoutSetId
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        WHERE we.workoutId = :workoutId
        ORDER BY pr.achievedAt DESC
        """
    )
    suspend fun getRecordsForWorkout(workoutId: String): List<PersonalRecordEntity>

    @Upsert
    suspend fun upsert(record: PersonalRecordEntity)

    @Upsert
    suspend fun upsertAll(records: List<PersonalRecordEntity>)

    @Query("DELETE FROM personal_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: String)

    @Query("DELETE FROM personal_records")
    suspend fun deleteAll()
}
