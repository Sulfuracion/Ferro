package com.davidguijo.ferro.core.data.mapper

import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity
import com.davidguijo.ferro.core.data.local.relation.ExerciseSessionRelation
import com.davidguijo.ferro.core.data.local.relation.WorkoutExerciseWithSetsEntity
import com.davidguijo.ferro.core.data.local.relation.WorkoutWithExercisesEntity
import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises

internal fun WorkoutEntity.toDomain(): Workout = Workout(
    id = id,
    name = name,
    routineId = routineId,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun Workout.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    name = name,
    routineId = routineId,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun WorkoutExerciseEntity.toDomain(): WorkoutExercise = WorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun WorkoutExercise.toEntity(): WorkoutExerciseEntity = WorkoutExerciseEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    setType = setType,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    durationS = durationS,
    distanceM = distanceM,
    completed = completed,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    setType = setType,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    durationS = durationS,
    distanceM = distanceM,
    completed = completed,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun WorkoutExerciseWithSetsEntity.toDomain(): WorkoutExerciseWithSets = WorkoutExerciseWithSets(
    workoutExercise = workoutExercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() }
)

internal fun WorkoutWithExercisesEntity.toDomain(): WorkoutWithExercises = WorkoutWithExercises(
    workout = workout.toDomain(),
    exercises = exercises.sortedBy { it.workoutExercise.position }.map { it.toDomain() }
)

internal fun ExerciseSessionRelation.toDomain(): ExerciseHistoryEntry = ExerciseHistoryEntry(
    workoutId = workoutExercise.workoutId,
    workoutName = workoutName,
    workoutStartedAt = workoutStartedAt,
    workoutEndedAt = workoutEndedAt,
    workoutExercise = workoutExercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() }
)
