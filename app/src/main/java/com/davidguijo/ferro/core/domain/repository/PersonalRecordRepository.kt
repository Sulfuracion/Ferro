package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.BrokenRecord
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

interface PersonalRecordRepository {
    fun observeRecordsForExercise(exerciseId: String): Flow<List<PersonalRecord>>

    fun observeRecordsForWorkout(workoutId: String): Flow<List<PersonalRecord>>

    suspend fun getLatestRecord(exerciseId: String, recordType: RecordType): PersonalRecord?

    suspend fun getRecordsForWorkout(workoutId: String): List<PersonalRecord>

    suspend fun upsertRecord(record: PersonalRecord)

    suspend fun upsertRecords(records: List<PersonalRecord>)

    suspend fun deleteRecord(id: String)

    /**
     * Compara las series de un entreno recién terminado con el histórico previo de cada ejercicio
     * y persiste en `personal_records` todos los nuevos récords batidos.
     */
    suspend fun evaluateAndSaveRecordsForCompletedWorkout(completedWorkout: WorkoutWithExercises): List<BrokenRecord>

    /**
     * Función reutilizable para recalcular desde cero los récords personales de los ejercicios
     * indicados ([exerciseIds]), o de toda la base de datos si [exerciseIds] está vacío, cuando
     * se edite o borre un entreno pasado.
     */
    suspend fun recalculateRecords(exerciseIds: Set<String> = emptySet())
}
