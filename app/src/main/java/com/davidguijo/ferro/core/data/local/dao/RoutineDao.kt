package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineFolderEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity
import com.davidguijo.ferro.core.data.local.relation.RoutineWithExercisesEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RoutineDao {
    @Query("SELECT * FROM routine_folders ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun observeFolders(): Flow<List<RoutineFolderEntity>>

    @Upsert
    suspend fun upsertFolder(folder: RoutineFolderEntity)

    @Query("DELETE FROM routine_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("SELECT * FROM routines ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun observeAllRoutinesWithExercises(): Flow<List<RoutineWithExercisesEntity>>

    @Query(
        """
        SELECT * FROM routines
        WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId
        ORDER BY position ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeRoutinesInFolder(folderId: String?): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun observeRoutineWithExercises(routineId: String): Flow<RoutineWithExercisesEntity?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithExercises(routineId: String): RoutineWithExercisesEntity?

    @Query(
        "SELECT COALESCE(MAX(position), -1) FROM routines WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId"
    )
    suspend fun getMaxRoutinePositionInFolder(folderId: String?): Int

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Upsert
    suspend fun upsertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertRoutineSets(sets: List<RoutineSetEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercisesByRoutineId(routineId: String)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: String)

    /** Guarda de forma atómica una rutina completa reemplazando sus ejercicios y series. */
    @Transaction
    suspend fun saveRoutineWithExercises(
        routine: RoutineEntity,
        exercises: List<RoutineExerciseEntity>,
        sets: List<RoutineSetEntity>
    ) {
        upsertRoutine(routine)
        deleteRoutineExercisesByRoutineId(routine.id)
        if (exercises.isNotEmpty()) {
            upsertRoutineExercises(exercises)
        }
        if (sets.isNotEmpty()) {
            upsertRoutineSets(sets)
        }
    }

    /** Guarda una carpeta opcional y un conjunto de rutinas con sus ejercicios y series en una única transacción. */
    @Transaction
    suspend fun saveFolderWithRoutines(
        folder: RoutineFolderEntity?,
        routines: List<RoutineEntity>,
        exercises: List<RoutineExerciseEntity>,
        sets: List<RoutineSetEntity>
    ) {
        if (folder != null) {
            upsertFolder(folder)
        }
        for (routine in routines) {
            upsertRoutine(routine)
            deleteRoutineExercisesByRoutineId(routine.id)
        }
        if (exercises.isNotEmpty()) {
            upsertRoutineExercises(exercises)
        }
        if (sets.isNotEmpty()) {
            upsertRoutineSets(sets)
        }
    }

    /** Duplica una rutina junto con todos sus ejercicios y series en una única transacción. */
    @Transaction
    suspend fun duplicateRoutine(
        sourceRoutineId: String,
        newRoutineId: String,
        newName: String,
        now: Long
    ): RoutineWithExercisesEntity? {
        val source = getRoutineWithExercises(sourceRoutineId) ?: return null
        val nextPosition = getMaxRoutinePositionInFolder(source.routine.folderId) + 1
        val copiedRoutine =
            source.routine.copy(
                id = newRoutineId,
                name = newName,
                position = nextPosition,
                createdAt = now,
                updatedAt = now
            )

        val copiedExercises = mutableListOf<RoutineExerciseEntity>()
        val copiedSets = mutableListOf<RoutineSetEntity>()

        for (exerciseWithSets in source.exercises.sortedBy { it.routineExercise.position }) {
            val newExerciseId = UUID.randomUUID().toString()
            copiedExercises +=
                exerciseWithSets.routineExercise.copy(
                    id = newExerciseId,
                    routineId = newRoutineId,
                    createdAt = now,
                    updatedAt = now
                )
            for (set in exerciseWithSets.sets.sortedBy { it.position }) {
                copiedSets +=
                    set.copy(
                        id = UUID.randomUUID().toString(),
                        routineExerciseId = newExerciseId,
                        createdAt = now,
                        updatedAt = now
                    )
            }
        }

        upsertRoutine(copiedRoutine)
        if (copiedExercises.isNotEmpty()) {
            upsertRoutineExercises(copiedExercises)
        }
        if (copiedSets.isNotEmpty()) {
            upsertRoutineSets(copiedSets)
        }
        return getRoutineWithExercises(newRoutineId)
    }
}
