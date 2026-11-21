package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeActiveExercises(): Flow<List<Exercise>>

    fun observeAllExercises(): Flow<List<Exercise>>

    fun observeExerciseById(id: String): Flow<Exercise?>

    fun searchExercises(
        query: String = "",
        muscle: Muscle? = null,
        equipment: Equipment? = null,
        includeArchived: Boolean = false
    ): Flow<List<Exercise>>

    suspend fun getExerciseById(id: String): Exercise?

    suspend fun upsertExercise(exercise: Exercise)

    suspend fun upsertExercises(exercises: List<Exercise>)

    suspend fun insertMissingExercises(exercises: List<Exercise>): Int

    suspend fun setArchived(id: String, archived: Boolean)

    suspend fun deleteExercise(id: String)
}
