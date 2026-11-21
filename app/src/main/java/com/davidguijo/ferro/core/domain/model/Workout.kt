package com.davidguijo.ferro.core.domain.model

data class Workout(
    val id: String,
    val name: String,
    val routineId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val notes: String,
    val status: WorkoutStatus,
    val createdAt: Long,
    val updatedAt: Long
)

data class WorkoutExercise(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val position: Int,
    val supersetGroup: String?,
    val restSeconds: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class WorkoutSet(
    val id: String,
    val workoutExerciseId: String,
    val position: Int,
    val setType: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val durationS: Int?,
    val distanceM: Double?,
    val completed: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class WorkoutExerciseWithSets(val workoutExercise: WorkoutExercise, val sets: List<WorkoutSet>)

data class WorkoutWithExercises(val workout: Workout, val exercises: List<WorkoutExerciseWithSets>)

/** Registro de un ejercicio dentro de una sesión completada (para historial y columna "Anterior"). */
data class ExerciseHistoryEntry(
    val workoutId: String,
    val workoutName: String,
    val workoutStartedAt: Long,
    val workoutEndedAt: Long?,
    val workoutExercise: WorkoutExercise,
    val sets: List<WorkoutSet>
)
