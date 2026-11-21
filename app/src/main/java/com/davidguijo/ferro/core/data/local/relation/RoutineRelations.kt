package com.davidguijo.ferro.core.data.local.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity

internal data class RoutineExerciseWithSetsEntity(
    @Embedded val routineExercise: RoutineExerciseEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["routineExerciseId"]
    )
    val sets: List<RoutineSetEntity>
)

internal data class RoutineWithExercisesEntity(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumns = ["id"],
        entityColumns = ["routineId"]
    )
    val exercises: List<RoutineExerciseWithSetsEntity>
)
