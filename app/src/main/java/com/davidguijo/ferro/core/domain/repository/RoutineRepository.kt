package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeFolders(): Flow<List<RoutineFolder>>

    fun observeRoutines(): Flow<List<Routine>>

    fun observeAllRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    fun observeRoutinesInFolder(folderId: String?): Flow<List<Routine>>

    fun observeRoutineWithExercises(routineId: String): Flow<RoutineWithExercises?>

    suspend fun getRoutineWithExercises(routineId: String): RoutineWithExercises?

    suspend fun upsertFolder(folder: RoutineFolder)

    suspend fun deleteFolder(folderId: String)

    suspend fun upsertRoutines(routines: List<Routine>)

    suspend fun saveRoutineWithExercises(routineWithExercises: RoutineWithExercises)

    suspend fun saveFolderWithRoutines(folder: RoutineFolder?, routinesWithExercises: List<RoutineWithExercises>)

    /**
     * Duplica una rutina junto con todos sus ejercicios y series en una única transacción.
     * Devuelve la nueva rutina con sus ejercicios o `null` si no existe [sourceRoutineId].
     */
    suspend fun duplicateRoutine(
        sourceRoutineId: String,
        newName: String,
        newRoutineId: String? = null
    ): RoutineWithExercises?

    suspend fun deleteRoutine(routineId: String)

    suspend fun getTemplatePrograms(): List<RoutineTemplateProgram>
}
