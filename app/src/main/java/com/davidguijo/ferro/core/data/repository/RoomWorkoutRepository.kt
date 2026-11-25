package com.davidguijo.ferro.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.ExerciseSessionMetricRow
import com.davidguijo.ferro.core.domain.model.MuscleExerciseSetAggregate
import com.davidguijo.ferro.core.domain.model.MuscleLastTrainedAggregate
import com.davidguijo.ferro.core.domain.model.RepMaxEntry
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutSessionAggregate
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomWorkoutRepository
@Inject
constructor(private val workoutDao: WorkoutDao, private val clock: Clock) :
    WorkoutRepository {
    override fun observeActiveWorkout(): Flow<WorkoutWithExercises?> =
        workoutDao.observeActiveWorkout().map { it?.toDomain() }

    override suspend fun getActiveWorkout(): WorkoutWithExercises? = workoutDao.getActiveWorkout()?.toDomain()

    override fun observeWorkoutWithExercises(workoutId: String): Flow<WorkoutWithExercises?> =
        workoutDao.observeWorkoutWithExercises(workoutId).map { it?.toDomain() }

    override suspend fun getWorkoutWithExercises(workoutId: String): WorkoutWithExercises? =
        workoutDao.getWorkoutWithExercises(workoutId)?.toDomain()

    override fun observeCompletedWorkouts(): Flow<List<Workout>> =
        workoutDao.observeCompletedWorkouts().map { list -> list.map { it.toDomain() } }

    override fun pagingCompletedWorkouts(): Flow<PagingData<WorkoutWithExercises>> = Pager(
        config = PagingConfig(pageSize = HISTORY_PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { workoutDao.pagingCompletedWorkouts() }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    private companion object {
        const val HISTORY_PAGE_SIZE = 20
    }

    override fun observeCompletedWorkoutsInRange(startMillis: Long, endMillis: Long): Flow<List<Workout>> = workoutDao
        .observeCompletedWorkoutsInRange(startMillis = startMillis, endMillis = endMillis)
        .map { list -> list.map { it.toDomain() } }

    override suspend fun getLastSessionForExercise(
        exerciseId: String,
        excludeWorkoutId: String?
    ): ExerciseHistoryEntry? = workoutDao
        .getLastSessionForExercise(exerciseId = exerciseId, excludeWorkoutId = excludeWorkoutId)
        ?.toDomain()

    override suspend fun getLastCompletedWorkoutForRoutine(
        routineId: String,
        excludeWorkoutId: String?,
        beforeStartedAt: Long?
    ): WorkoutWithExercises? = workoutDao
        .getLastCompletedWorkoutForRoutine(
            routineId = routineId,
            excludeWorkoutId = excludeWorkoutId,
            beforeStartedAt = beforeStartedAt
        )?.toDomain()

    override fun observeExerciseHistory(exerciseId: String): Flow<List<ExerciseHistoryEntry>> =
        workoutDao.observeExerciseHistory(exerciseId).map { list -> list.map { it.toDomain() } }

    override suspend fun getExerciseHistory(exerciseId: String, excludeWorkoutId: String?): List<ExerciseHistoryEntry> =
        workoutDao.getExerciseHistory(exerciseId = exerciseId, excludeWorkoutId = excludeWorkoutId).map {
            it.toDomain()
        }

    override fun observeExerciseChartMetrics(
        exerciseId: String,
        sinceMillis: Long?
    ): Flow<List<ExerciseSessionMetricRow>> = workoutDao
        .observeExerciseChartMetrics(exerciseId = exerciseId, sinceMillis = sinceMillis)
        .map { rows ->
            rows.map { row ->
                ExerciseSessionMetricRow(
                    workoutId = row.workoutId,
                    startedAt = row.startedAt,
                    estimated1RmKg = row.estimated1RmKg,
                    maxWeightKg = row.maxWeightKg,
                    bestSetVolumeKg = row.bestSetVolumeKg,
                    sessionVolumeKg = row.sessionVolumeKg
                )
            }
        }

    override fun observeExerciseRepMaxes(exerciseId: String): Flow<List<RepMaxEntry>> =
        workoutDao.observeExerciseRepMaxes(exerciseId = exerciseId).map { rows ->
            rows.map { row ->
                RepMaxEntry(
                    reps = row.reps,
                    weightKg = row.bestWeightKg,
                    achievedAt = row.achievedAt
                )
            }
        }

    override fun observeCompletedWorkoutAggregatesSince(
        sinceMillis: Long,
        latestBodyWeightKg: Double?
    ): Flow<List<WorkoutSessionAggregate>> = workoutDao
        .observeCompletedWorkoutAggregatesSince(
            sinceMillis = sinceMillis,
            latestBodyWeightKg = latestBodyWeightKg
        ).map { rows ->
            rows.map { row ->
                WorkoutSessionAggregate(
                    workoutId = row.workoutId,
                    startedAt = row.startedAt,
                    endedAt = row.endedAt,
                    volumeKg = row.volumeKg
                )
            }
        }

    override fun observeMuscleSetAggregatesSince(sinceMillis: Long): Flow<List<MuscleExerciseSetAggregate>> =
        workoutDao.observeMuscleSetAggregatesSince(sinceMillis = sinceMillis).map { rows ->
            rows.map { row ->
                MuscleExerciseSetAggregate(
                    workoutStartedAt = row.workoutStartedAt,
                    primaryMuscle = row.primaryMuscle,
                    secondaryMuscles = row.secondaryMuscles,
                    completedSetCount = row.completedSetCount
                )
            }
        }

    override fun observeMuscleLastTrained(): Flow<List<MuscleLastTrainedAggregate>> =
        workoutDao.observeMuscleLastTrained().map { rows ->
            rows.map { row ->
                MuscleLastTrainedAggregate(
                    primaryMuscle = row.primaryMuscle,
                    secondaryMuscles = row.secondaryMuscles,
                    lastTrainedAt = row.lastTrainedAt
                )
            }
        }

    override suspend fun saveWorkoutWithExercises(workoutWithExercises: WorkoutWithExercises) {
        val workoutEntity = workoutWithExercises.workout.toEntity()
        val exerciseEntities = workoutWithExercises.exercises.map { it.workoutExercise.toEntity() }
        val setEntities = workoutWithExercises.exercises.flatMap { exercise -> exercise.sets.map { it.toEntity() } }
        workoutDao.saveWorkoutWithExercises(
            workout = workoutEntity,
            exercises = exerciseEntities,
            sets = setEntities
        )
    }

    override suspend fun upsertWorkout(workout: Workout) {
        workoutDao.upsertWorkout(workout.toEntity())
    }

    override suspend fun upsertWorkoutExercise(workoutExercise: WorkoutExercise) {
        workoutDao.upsertWorkoutExercise(workoutExercise.toEntity())
    }

    override suspend fun deleteWorkoutExercise(workoutExerciseId: String) {
        workoutDao.deleteWorkoutExerciseById(workoutExerciseId)
    }

    override suspend fun upsertWorkoutSet(workoutSet: WorkoutSet) {
        workoutDao.upsertWorkoutSet(workoutSet.toEntity())
    }

    override suspend fun deleteWorkoutSet(workoutSetId: String) {
        workoutDao.deleteWorkoutSetById(workoutSetId)
    }

    override suspend fun updateWorkoutStatus(workoutId: String, status: WorkoutStatus, endedAt: Long?) {
        workoutDao.updateWorkoutStatus(
            workoutId = workoutId,
            status = status,
            endedAt = endedAt,
            updatedAt = clock.millis()
        )
    }

    override suspend fun deleteWorkout(workoutId: String) {
        workoutDao.deleteWorkoutById(workoutId)
    }
}
