package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.ExerciseEntity
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY nameEs COLLATE NOCASE ASC")
    fun observeActiveExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY nameEs COLLATE NOCASE ASC")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:includeArchived = 1 OR archived = 0)
          AND (:muscle IS NULL OR primaryMuscle = :muscle)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (
            :query = ''
            OR nameEs LIKE '%' || :query || '%' COLLATE NOCASE
            OR nameEn LIKE '%' || :query || '%' COLLATE NOCASE
          )
        ORDER BY nameEs COLLATE NOCASE ASC
        """
    )
    fun searchExercises(
        query: String,
        muscle: Muscle?,
        equipment: Equipment?,
        includeArchived: Boolean
    ): Flow<List<ExerciseEntity>>

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("UPDATE exercises SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("SELECT id FROM exercises")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}
