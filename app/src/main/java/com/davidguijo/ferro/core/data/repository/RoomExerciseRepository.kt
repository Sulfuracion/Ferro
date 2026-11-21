package com.davidguijo.ferro.core.data.repository

import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomExerciseRepository
@Inject
constructor(
    private val exerciseDao: ExerciseDao,
    private val clock: Clock
) : ExerciseRepository {
    override fun observeActiveExercises(): Flow<List<Exercise>> =
        exerciseDao.observeActiveExercises().map { list -> list.map { it.toDomain() } }

    override fun observeAllExercises(): Flow<List<Exercise>> =
        exerciseDao.observeAllExercises().map { list -> list.map { it.toDomain() } }

    override fun observeExerciseById(id: String): Flow<Exercise?> = exerciseDao.observeById(id).map { it?.toDomain() }

    override fun searchExercises(
        query: String,
        muscle: Muscle?,
        equipment: Equipment?,
        includeArchived: Boolean
    ): Flow<List<Exercise>> = exerciseDao
        .searchExercises(
            query = query.trim(),
            muscle = muscle,
            equipment = equipment,
            includeArchived = includeArchived
        )
        .map { list -> list.map { it.toDomain() } }

    override suspend fun getExerciseById(id: String): Exercise? = exerciseDao.getById(id)?.toDomain()

    override suspend fun upsertExercise(exercise: Exercise) {
        exerciseDao.upsert(exercise.toEntity())
    }

    override suspend fun upsertExercises(exercises: List<Exercise>) {
        exerciseDao.upsertAll(exercises.map { it.toEntity() })
    }

    override suspend fun insertMissingExercises(exercises: List<Exercise>): Int {
        if (exercises.isEmpty()) return 0
        val existingIds = exerciseDao.getAllIds().toHashSet()
        val missing = exercises.filter { it.id !in existingIds }
        if (missing.isEmpty()) return 0
        exerciseDao.upsertAll(missing.map { it.toEntity() })
        return missing.size
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        exerciseDao.setArchived(id = id, archived = archived, updatedAt = clock.millis())
    }

    override suspend fun deleteExercise(id: String) {
        exerciseDao.deleteById(id)
    }
}
