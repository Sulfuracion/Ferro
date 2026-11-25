package com.davidguijo.ferro.core.domain.repository

import androidx.paging.PagingData
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
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** Observa el entreno en curso (si hay uno). */
    fun observeActiveWorkout(): Flow<WorkoutWithExercises?>

    suspend fun getActiveWorkout(): WorkoutWithExercises?

    fun observeWorkoutWithExercises(workoutId: String): Flow<WorkoutWithExercises?>

    suspend fun getWorkoutWithExercises(workoutId: String): WorkoutWithExercises?

    fun observeCompletedWorkouts(): Flow<List<Workout>>

    fun pagingCompletedWorkouts(): Flow<PagingData<WorkoutWithExercises>>

    fun observeCompletedWorkoutsInRange(startMillis: Long, endMillis: Long): Flow<List<Workout>>

    /** Última sesión completada en la que se realizó [exerciseId] (para la columna «Anterior»). */
    suspend fun getLastSessionForExercise(exerciseId: String, excludeWorkoutId: String? = null): ExerciseHistoryEntry?

    /** Último entreno completado de la misma rutina [routineId] (para comparar en el resumen). */
    suspend fun getLastCompletedWorkoutForRoutine(
        routineId: String,
        excludeWorkoutId: String? = null,
        beforeStartedAt: Long? = null
    ): WorkoutWithExercises?

    /** Historial completo de sesiones completadas para [exerciseId], de más reciente a más antigua. */
    fun observeExerciseHistory(exerciseId: String): Flow<List<ExerciseHistoryEntry>>

    /** Historial completo de sesiones completadas para [exerciseId], en orden cronológico ascendente. */
    suspend fun getExerciseHistory(exerciseId: String, excludeWorkoutId: String? = null): List<ExerciseHistoryEntry>

    /** Métricas agregadas en SQL por sesión para las gráficas de [exerciseId]. */
    fun observeExerciseChartMetrics(exerciseId: String, sinceMillis: Long?): Flow<List<ExerciseSessionMetricRow>>

    /** Mejor peso registrado en SQL para cada número exacto de repeticiones (1..12) en [exerciseId]. */
    fun observeExerciseRepMaxes(exerciseId: String): Flow<List<RepMaxEntry>>

    /** Agregados ligeros de volumen y duración por sesión completada desde [sinceMillis]. */
    fun observeCompletedWorkoutAggregatesSince(
        sinceMillis: Long,
        latestBodyWeightKg: Double? = null
    ): Flow<List<WorkoutSessionAggregate>>

    /** Series completadas (no calentamiento) agrupadas por sesión y ejercicio desde [sinceMillis]. */
    fun observeMuscleSetAggregatesSince(sinceMillis: Long): Flow<List<MuscleExerciseSetAggregate>>

    /** Última fecha de entreno completado por combinación muscular (agregado en SQL). */
    fun observeMuscleLastTrained(): Flow<List<MuscleLastTrainedAggregate>>

    /** Guarda de forma atómica un entreno completo con sus ejercicios y series. */
    suspend fun saveWorkoutWithExercises(workoutWithExercises: WorkoutWithExercises)

    suspend fun upsertWorkout(workout: Workout)

    suspend fun upsertWorkoutExercise(workoutExercise: WorkoutExercise)

    suspend fun deleteWorkoutExercise(workoutExerciseId: String)

    suspend fun upsertWorkoutSet(workoutSet: WorkoutSet)

    suspend fun deleteWorkoutSet(workoutSetId: String)

    suspend fun updateWorkoutStatus(workoutId: String, status: WorkoutStatus, endedAt: Long?)

    suspend fun deleteWorkout(workoutId: String)
}
