package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.domain.model.RecordType
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PersonalRecordDao {
    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun observeRecordsForExercise(exerciseId: String): Flow<List<PersonalRecordEntity>>

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
