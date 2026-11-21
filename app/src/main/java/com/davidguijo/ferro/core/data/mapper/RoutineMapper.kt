package com.davidguijo.ferro.core.data.mapper

import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineFolderEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity
import com.davidguijo.ferro.core.data.local.relation.RoutineExerciseWithSetsEntity
import com.davidguijo.ferro.core.data.local.relation.RoutineWithExercisesEntity
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises

internal fun RoutineFolderEntity.toDomain(): RoutineFolder = RoutineFolder(
    id = id,
    name = name,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineFolder.toEntity(): RoutineFolderEntity = RoutineFolderEntity(
    id = id,
    name = name,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    folderId = folderId,
    name = name,
    notes = notes,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    folderId = folderId,
    name = name,
    notes = notes,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineExerciseEntity.toDomain(): RoutineExercise = RoutineExercise(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineExercise.toEntity(): RoutineExerciseEntity = RoutineExerciseEntity(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineSetEntity.toDomain(): RoutineSet = RoutineSet(
    id = id,
    routineExerciseId = routineExerciseId,
    position = position,
    setType = setType,
    targetWeightKg = targetWeightKg,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetDurationS = targetDurationS,
    targetDistanceM = targetDistanceM,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineSet.toEntity(): RoutineSetEntity = RoutineSetEntity(
    id = id,
    routineExerciseId = routineExerciseId,
    position = position,
    setType = setType,
    targetWeightKg = targetWeightKg,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetDurationS = targetDurationS,
    targetDistanceM = targetDistanceM,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun RoutineExerciseWithSetsEntity.toDomain(): RoutineExerciseWithSets = RoutineExerciseWithSets(
    routineExercise = routineExercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() }
)

internal fun RoutineWithExercisesEntity.toDomain(): RoutineWithExercises = RoutineWithExercises(
    routine = routine.toDomain(),
    exercises = exercises.sortedBy { it.routineExercise.position }.map { it.toDomain() }
)
