package com.davidguijo.ferro.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.davidguijo.ferro.core.data.local.entity.BodyMeasurementEntity
import com.davidguijo.ferro.core.data.local.entity.ExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.data.local.entity.ProgressPhotoEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineFolderEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity

@Dao
internal interface BackupDao {
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM routine_folders ORDER BY position ASC, id ASC")
    suspend fun getAllRoutineFolders(): List<RoutineFolderEntity>

    @Query("SELECT * FROM routines ORDER BY position ASC, id ASC")
    suspend fun getAllRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId ASC, position ASC, id ASC")
    suspend fun getAllRoutineExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_sets ORDER BY routineExerciseId ASC, position ASC, id ASC")
    suspend fun getAllRoutineSets(): List<RoutineSetEntity>

    @Query("SELECT * FROM workouts ORDER BY startedAt ASC, id ASC")
    suspend fun getAllWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_exercises ORDER BY workoutId ASC, position ASC, id ASC")
    suspend fun getAllWorkoutExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_sets ORDER BY workoutExerciseId ASC, position ASC, id ASC")
    suspend fun getAllWorkoutSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM personal_records ORDER BY achievedAt ASC, id ASC")
    suspend fun getAllPersonalRecords(): List<PersonalRecordEntity>

    @Query("SELECT * FROM body_measurements ORDER BY measuredAt ASC, id ASC")
    suspend fun getAllBodyMeasurements(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM progress_photos ORDER BY takenAt ASC, id ASC")
    suspend fun getAllProgressPhotos(): List<ProgressPhotoEntity>

    @Upsert
    suspend fun upsertExercises(items: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertRoutineFolders(items: List<RoutineFolderEntity>)

    @Upsert
    suspend fun upsertRoutines(items: List<RoutineEntity>)

    @Upsert
    suspend fun upsertRoutineExercises(items: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertRoutineSets(items: List<RoutineSetEntity>)

    @Upsert
    suspend fun upsertWorkouts(items: List<WorkoutEntity>)

    @Upsert
    suspend fun upsertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun upsertWorkoutSets(items: List<WorkoutSetEntity>)

    @Upsert
    suspend fun upsertPersonalRecords(items: List<PersonalRecordEntity>)

    @Upsert
    suspend fun upsertBodyMeasurements(items: List<BodyMeasurementEntity>)

    @Upsert
    suspend fun upsertProgressPhotos(items: List<ProgressPhotoEntity>)

    @Query("DELETE FROM personal_records")
    suspend fun deleteAllPersonalRecords()

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllWorkoutSets()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllWorkoutExercises()

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM routine_sets")
    suspend fun deleteAllRoutineSets()

    @Query("DELETE FROM routine_exercises")
    suspend fun deleteAllRoutineExercises()

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM routine_folders")
    suspend fun deleteAllRoutineFolders()

    @Query("DELETE FROM progress_photos")
    suspend fun deleteAllProgressPhotos()

    @Query("DELETE FROM body_measurements")
    suspend fun deleteAllBodyMeasurements()

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    /** Reemplaza por completo todas las tablas de la base de datos en una sola transacción. */
    @Transaction
    suspend fun replaceAllTables(
        exercises: List<ExerciseEntity>,
        routineFolders: List<RoutineFolderEntity>,
        routines: List<RoutineEntity>,
        routineExercises: List<RoutineExerciseEntity>,
        routineSets: List<RoutineSetEntity>,
        workouts: List<WorkoutEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
        workoutSets: List<WorkoutSetEntity>,
        personalRecords: List<PersonalRecordEntity>,
        bodyMeasurements: List<BodyMeasurementEntity>,
        progressPhotos: List<ProgressPhotoEntity>
    ) {
        deleteAllPersonalRecords()
        deleteAllWorkoutSets()
        deleteAllWorkoutExercises()
        deleteAllWorkouts()
        deleteAllRoutineSets()
        deleteAllRoutineExercises()
        deleteAllRoutines()
        deleteAllRoutineFolders()
        deleteAllProgressPhotos()
        deleteAllBodyMeasurements()
        deleteAllExercises()

        mergeAllTables(
            exercises = exercises,
            routineFolders = routineFolders,
            routines = routines,
            routineExercises = routineExercises,
            routineSets = routineSets,
            workouts = workouts,
            workoutExercises = workoutExercises,
            workoutSets = workoutSets,
            personalRecords = personalRecords,
            bodyMeasurements = bodyMeasurements,
            progressPhotos = progressPhotos
        )
    }

    /** Fusiona por clave primaria (`id`) todas las tablas en orden de padre a hijo en una sola transacción. */
    @Transaction
    suspend fun mergeAllTables(
        exercises: List<ExerciseEntity>,
        routineFolders: List<RoutineFolderEntity>,
        routines: List<RoutineEntity>,
        routineExercises: List<RoutineExerciseEntity>,
        routineSets: List<RoutineSetEntity>,
        workouts: List<WorkoutEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
        workoutSets: List<WorkoutSetEntity>,
        personalRecords: List<PersonalRecordEntity>,
        bodyMeasurements: List<BodyMeasurementEntity>,
        progressPhotos: List<ProgressPhotoEntity>
    ) {
        if (exercises.isNotEmpty()) upsertExercises(exercises)
        if (routineFolders.isNotEmpty()) upsertRoutineFolders(routineFolders)
        if (routines.isNotEmpty()) upsertRoutines(routines)
        if (routineExercises.isNotEmpty()) upsertRoutineExercises(routineExercises)
        if (routineSets.isNotEmpty()) upsertRoutineSets(routineSets)
        if (workouts.isNotEmpty()) upsertWorkouts(workouts)
        if (workoutExercises.isNotEmpty()) upsertWorkoutExercises(workoutExercises)
        if (workoutSets.isNotEmpty()) upsertWorkoutSets(workoutSets)
        if (personalRecords.isNotEmpty()) upsertPersonalRecords(personalRecords)
        if (bodyMeasurements.isNotEmpty()) upsertBodyMeasurements(bodyMeasurements)
        if (progressPhotos.isNotEmpty()) upsertProgressPhotos(progressPhotos)
    }
}
