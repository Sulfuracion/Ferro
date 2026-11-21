package com.davidguijo.ferro.core.data.local.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity

internal data class WorkoutExerciseWithSetsEntity(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["workoutExerciseId"]
    )
    val sets: List<WorkoutSetEntity>
)

internal data class WorkoutWithExercisesEntity(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumns = ["id"],
        entityColumns = ["workoutId"]
    )
    val exercises: List<WorkoutExerciseWithSetsEntity>
)

internal data class ExerciseSessionRelation(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    val workoutName: String,
    val workoutStartedAt: Long,
    val workoutEndedAt: Long?,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["workoutExerciseId"]
    )
    val sets: List<WorkoutSetEntity>
)
