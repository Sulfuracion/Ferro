package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity
import com.davidguijo.ferro.core.data.local.relation.ExerciseSessionRelation
import com.davidguijo.ferro.core.data.local.relation.WorkoutWithExercisesEntity
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(status: WorkoutStatus = WorkoutStatus.IN_PROGRESS): Flow<WorkoutWithExercisesEntity?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkout(status: WorkoutStatus = WorkoutStatus.IN_PROGRESS): WorkoutWithExercisesEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun observeWorkoutWithExercises(workoutId: String): Flow<WorkoutWithExercisesEntity?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: String): WorkoutWithExercisesEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM workouts
        WHERE routineId = :routineId
          AND status = :completedStatus
          AND (:excludeWorkoutId IS NULL OR id != :excludeWorkoutId)
          AND (:beforeStartedAt IS NULL OR startedAt <= :beforeStartedAt)
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastCompletedWorkoutForRoutine(
        routineId: String,
        excludeWorkoutId: String? = null,
        beforeStartedAt: Long? = null,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED
    ): WorkoutWithExercisesEntity?

    @Query("SELECT DISTINCT exerciseId FROM workout_exercises")
    suspend fun getAllDistinctWorkoutExerciseIds(): List<String>

    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY startedAt DESC")
    fun observeCompletedWorkouts(status: WorkoutStatus = WorkoutStatus.COMPLETED): Flow<List<WorkoutEntity>>

    @Query(
        """
        SELECT * FROM workouts
        WHERE status = :status
          AND startedAt >= :startMillis
          AND startedAt < :endMillis
        ORDER BY startedAt DESC
        """
    )
    fun observeCompletedWorkoutsInRange(
        startMillis: Long,
        endMillis: Long,
        status: WorkoutStatus = WorkoutStatus.COMPLETED
    ): Flow<List<WorkoutEntity>>

    @Transaction
    @Query(
        """
        SELECT
            we.*,
            w.name AS workoutName,
            w.startedAt AS workoutStartedAt,
            w.endedAt AS workoutEndedAt
        FROM workout_exercises AS we
        INNER JOIN workouts AS w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId
          AND w.status = :completedStatus
          AND (:excludeWorkoutId IS NULL OR w.id != :excludeWorkoutId)
        ORDER BY w.startedAt DESC, we.position ASC
        LIMIT 1
        """
    )
    suspend fun getLastSessionForExercise(
        exerciseId: String,
        excludeWorkoutId: String? = null,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED
    ): ExerciseSessionRelation?

    @Transaction
    @Query(
        """
        SELECT
            we.*,
            w.name AS workoutName,
            w.startedAt AS workoutStartedAt,
            w.endedAt AS workoutEndedAt
        FROM workout_exercises AS we
        INNER JOIN workouts AS w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId
          AND w.status = :completedStatus
        ORDER BY w.startedAt DESC, we.position ASC
        """
    )
    fun observeExerciseHistory(
        exerciseId: String,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED
    ): Flow<List<ExerciseSessionRelation>>

    @Transaction
    @Query(
        """
        SELECT
            we.*,
            w.name AS workoutName,
            w.startedAt AS workoutStartedAt,
            w.endedAt AS workoutEndedAt
        FROM workout_exercises AS we
        INNER JOIN workouts AS w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId
          AND w.status = :completedStatus
          AND (:excludeWorkoutId IS NULL OR w.id != :excludeWorkoutId)
        ORDER BY w.startedAt ASC, we.position ASC
        """
    )
    suspend fun getExerciseHistory(
        exerciseId: String,
        excludeWorkoutId: String? = null,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED
    ): List<ExerciseSessionRelation>

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Upsert
    suspend fun upsertWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Upsert
    suspend fun upsertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)

    @Upsert
    suspend fun upsertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteAllWorkoutExercises(workoutId: String)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId AND id NOT IN (:keepIds)")
    suspend fun deleteWorkoutExercisesNotIn(workoutId: String, keepIds: List<String>)

    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteWorkoutExerciseById(workoutExerciseId: String)

    @Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteAllWorkoutSets(workoutExerciseId: String)

    @Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId AND id NOT IN (:keepIds)")
    suspend fun deleteWorkoutSetsNotIn(workoutExerciseId: String, keepIds: List<String>)

    @Query("DELETE FROM workout_sets WHERE id = :workoutSetId")
    suspend fun deleteWorkoutSetById(workoutSetId: String)

    @Query("UPDATE workouts SET status = :status, endedAt = :endedAt, updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun updateWorkoutStatus(workoutId: String, status: WorkoutStatus, endedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)

    /**
     * Guarda de forma atómica un entreno completo con sus ejercicios y series.
     * Actualiza en sitio las filas existentes para no disparar `ON DELETE CASCADE` sobre `personal_records`.
     */
    @Transaction
    suspend fun saveWorkoutWithExercises(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>
    ) {
        upsertWorkout(workout)
        if (exercises.isEmpty()) {
            deleteAllWorkoutExercises(workout.id)
            return
        }
        deleteWorkoutExercisesNotIn(workout.id, exercises.map { it.id })
        upsertWorkoutExercises(exercises)

        val setsByExercise = sets.groupBy { it.workoutExerciseId }
        for (exercise in exercises) {
            val exerciseSets = setsByExercise[exercise.id].orEmpty()
            if (exerciseSets.isEmpty()) {
                deleteAllWorkoutSets(exercise.id)
            } else {
                deleteWorkoutSetsNotIn(exercise.id, exerciseSets.map { it.id })
            }
        }
        if (sets.isNotEmpty()) {
            upsertWorkoutSets(sets)
        }
    }
}
