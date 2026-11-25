package com.davidguijo.ferro.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity
import com.davidguijo.ferro.core.data.local.relation.ExerciseSessionRelation
import com.davidguijo.ferro.core.data.local.relation.WorkoutWithExercisesEntity
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WorkoutDao {
    data class ExerciseSessionMetricProjection(
        val workoutId: String,
        val startedAt: Long,
        val estimated1RmKg: Double?,
        val maxWeightKg: Double?,
        val bestSetVolumeKg: Double?,
        val sessionVolumeKg: Double?
    )

    data class RepMaxProjection(val reps: Int, val bestWeightKg: Double, val achievedAt: Long)

    data class WorkoutSessionAggregateProjection(
        val workoutId: String,
        val startedAt: Long,
        val endedAt: Long?,
        val volumeKg: Double
    )

    data class MuscleExerciseSetAggregateProjection(
        val workoutStartedAt: Long,
        val primaryMuscle: Muscle,
        val secondaryMuscles: List<Muscle>,
        val completedSetCount: Int
    )

    data class MuscleLastTrainedProjection(
        val primaryMuscle: Muscle,
        val secondaryMuscles: List<Muscle>,
        val lastTrainedAt: Long
    )

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

    @Transaction
    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY startedAt DESC")
    fun pagingCompletedWorkouts(
        status: WorkoutStatus = WorkoutStatus.COMPLETED
    ): PagingSource<Int, WorkoutWithExercisesEntity>

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

    @Query(
        """
        SELECT
            w.id AS workoutId,
            w.startedAt AS startedAt,
            MAX(
                CASE
                    WHEN ws.weightKg > 0 AND ws.reps = 1 THEN ws.weightKg
                    WHEN ws.weightKg > 0 AND ws.reps BETWEEN 2 AND 12 THEN ws.weightKg * (1.0 + ws.reps / 30.0)
                    ELSE NULL
                END
            ) AS estimated1RmKg,
            MAX(
                CASE
                    WHEN ws.weightKg > 0 AND ws.reps > 0 THEN ws.weightKg
                    ELSE NULL
                END
            ) AS maxWeightKg,
            MAX(
                CASE
                    WHEN ws.weightKg > 0 AND ws.reps > 0 THEN ws.weightKg * ws.reps
                    ELSE NULL
                END
            ) AS bestSetVolumeKg,
            SUM(
                CASE
                    WHEN ws.weightKg > 0 AND ws.reps > 0 THEN ws.weightKg * ws.reps
                    ELSE 0.0
                END
            ) AS sessionVolumeKg
        FROM workout_sets AS ws
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts AS w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId
          AND w.status = :completedStatus
          AND ws.completed = 1
          AND ws.setType != :warmupType
          AND (:sinceMillis IS NULL OR w.startedAt >= :sinceMillis)
        GROUP BY w.id
        ORDER BY w.startedAt ASC
        """
    )
    fun observeExerciseChartMetrics(
        exerciseId: String,
        sinceMillis: Long?,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED,
        warmupType: SetType = SetType.WARMUP
    ): Flow<List<ExerciseSessionMetricProjection>>

    @Query(
        """
        SELECT
            ws.reps AS reps,
            ws.weightKg AS bestWeightKg,
            MAX(COALESCE(ws.completedAt, w.endedAt, w.startedAt)) AS achievedAt
        FROM workout_sets AS ws
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts AS w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId
          AND w.status = :completedStatus
          AND ws.completed = 1
          AND ws.setType != :warmupType
          AND ws.reps BETWEEN 1 AND 12
          AND ws.weightKg IS NOT NULL
          AND ws.weightKg > 0
          AND ws.weightKg = (
              SELECT MAX(ws2.weightKg)
              FROM workout_sets AS ws2
              INNER JOIN workout_exercises AS we2 ON we2.id = ws2.workoutExerciseId
              INNER JOIN workouts AS w2 ON w2.id = we2.workoutId
              WHERE we2.exerciseId = :exerciseId
                AND w2.status = :completedStatus
                AND ws2.completed = 1
                AND ws2.setType != :warmupType
                AND ws2.reps = ws.reps
                AND ws2.weightKg IS NOT NULL
                AND ws2.weightKg > 0
          )
        GROUP BY ws.reps, ws.weightKg
        ORDER BY ws.reps ASC
        """
    )
    fun observeExerciseRepMaxes(
        exerciseId: String,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED,
        warmupType: SetType = SetType.WARMUP
    ): Flow<List<RepMaxProjection>>

    @Query(
        """
        SELECT
            w.id AS workoutId,
            w.startedAt AS startedAt,
            w.endedAt AS endedAt,
            COALESCE(
                SUM(
                    CASE
                        WHEN ws.completed = 1
                          AND ws.setType != :warmupType
                          AND e.trackingType = :weightRepsType
                          AND ws.weightKg > 0
                          AND ws.reps > 0
                        THEN ws.weightKg * ws.reps
                        WHEN ws.completed = 1
                          AND ws.setType != :warmupType
                          AND e.trackingType = :weightedBodyweightType
                          AND ws.reps > 0
                          AND :latestBodyWeightKg IS NOT NULL
                          AND :latestBodyWeightKg > 0
                        THEN (:latestBodyWeightKg + COALESCE(ws.weightKg, 0.0)) * ws.reps
                        WHEN ws.completed = 1
                          AND ws.setType != :warmupType
                          AND e.trackingType = :weightedBodyweightType
                          AND ws.weightKg > 0
                          AND ws.reps > 0
                        THEN ws.weightKg * ws.reps
                        WHEN ws.completed = 1
                          AND ws.setType != :warmupType
                          AND e.trackingType = :assistedBodyweightType
                          AND ws.reps > 0
                          AND :latestBodyWeightKg IS NOT NULL
                          AND :latestBodyWeightKg > 0
                        THEN CASE
                            WHEN :latestBodyWeightKg > COALESCE(ws.weightKg, 0.0)
                            THEN (:latestBodyWeightKg - COALESCE(ws.weightKg, 0.0)) * ws.reps
                            ELSE 0.0
                        END
                        ELSE 0.0
                    END
                ),
                0.0
            ) AS volumeKg
        FROM workouts AS w
        LEFT JOIN workout_exercises AS we ON we.workoutId = w.id
        LEFT JOIN exercises AS e ON e.id = we.exerciseId
        LEFT JOIN workout_sets AS ws ON ws.workoutExerciseId = we.id
        WHERE w.status = :completedStatus
          AND w.startedAt >= :sinceMillis
        GROUP BY w.id
        ORDER BY w.startedAt ASC
        """
    )
    fun observeCompletedWorkoutAggregatesSince(
        sinceMillis: Long,
        latestBodyWeightKg: Double? = null,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED,
        warmupType: SetType = SetType.WARMUP,
        weightRepsType: TrackingType = TrackingType.WEIGHT_REPS,
        weightedBodyweightType: TrackingType = TrackingType.WEIGHTED_BODYWEIGHT,
        assistedBodyweightType: TrackingType = TrackingType.ASSISTED_BODYWEIGHT
    ): Flow<List<WorkoutSessionAggregateProjection>>

    @Query(
        """
        SELECT
            w.startedAt AS workoutStartedAt,
            e.primaryMuscle AS primaryMuscle,
            e.secondaryMuscles AS secondaryMuscles,
            COUNT(ws.id) AS completedSetCount
        FROM workout_sets AS ws
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts AS w ON w.id = we.workoutId
        INNER JOIN exercises AS e ON e.id = we.exerciseId
        WHERE w.status = :completedStatus
          AND w.startedAt >= :sinceMillis
          AND ws.completed = 1
          AND ws.setType != :warmupType
        GROUP BY w.id, e.id
        """
    )
    fun observeMuscleSetAggregatesSince(
        sinceMillis: Long,
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED,
        warmupType: SetType = SetType.WARMUP
    ): Flow<List<MuscleExerciseSetAggregateProjection>>

    @Query(
        """
        SELECT
            e.primaryMuscle AS primaryMuscle,
            e.secondaryMuscles AS secondaryMuscles,
            MAX(COALESCE(w.endedAt, w.startedAt)) AS lastTrainedAt
        FROM workout_sets AS ws
        INNER JOIN workout_exercises AS we ON we.id = ws.workoutExerciseId
        INNER JOIN workouts AS w ON w.id = we.workoutId
        INNER JOIN exercises AS e ON e.id = we.exerciseId
        WHERE w.status = :completedStatus
          AND ws.completed = 1
          AND ws.setType != :warmupType
        GROUP BY e.primaryMuscle, e.secondaryMuscles
        """
    )
    fun observeMuscleLastTrained(
        completedStatus: WorkoutStatus = WorkoutStatus.COMPLETED,
        warmupType: SetType = SetType.WARMUP
    ): Flow<List<MuscleLastTrainedProjection>>

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
