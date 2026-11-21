package com.davidguijo.ferro.core.domain.model

data class RoutineFolder(val id: String, val name: String, val position: Int, val createdAt: Long, val updatedAt: Long)

data class Routine(
    val id: String,
    val folderId: String?,
    val name: String,
    val notes: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class RoutineExercise(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val position: Int,
    val supersetGroup: String?,
    val restSeconds: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class RoutineSet(
    val id: String,
    val routineExerciseId: String,
    val position: Int,
    val setType: SetType,
    val targetWeightKg: Double?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetDurationS: Int?,
    val targetDistanceM: Double?,
    val createdAt: Long,
    val updatedAt: Long
)

data class RoutineExerciseWithSets(val routineExercise: RoutineExercise, val sets: List<RoutineSet>)

data class RoutineWithExercises(val routine: Routine, val exercises: List<RoutineExerciseWithSets>)
