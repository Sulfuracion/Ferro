package com.davidguijo.ferro.core.domain.model

/** Modo de importación de una copia de seguridad JSON. */
enum class BackupImportMode {
    REPLACE_ALL,
    MERGE_BY_ID
}

/**
 * Vista previa validada de un archivo de copia de seguridad JSON antes de importarlo.
 */
data class BackupPreview(
    val schemaVersion: Int,
    val migratedFromSchemaVersion: Int?,
    val appVersion: String,
    val exportedAtMillis: Long,
    val workoutsCount: Int,
    val routinesCount: Int,
    val exercisesCount: Int,
    val measurementsCount: Int
)

/** Resultado de la importación de una copia de seguridad JSON. */
data class BackupImportResult(val preview: BackupPreview, val mode: BackupImportMode, val safetyBackupFileName: String)

/** Instantánea completa de datos del usuario para exportación, importación o cálculo de CSV. */
data class BackupSnapshot(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAtMillis: Long,
    val settings: Settings,
    val exercises: List<Exercise>,
    val routineFolders: List<RoutineFolder>,
    val routines: List<Routine>,
    val routineExercises: List<RoutineExercise>,
    val routineSets: List<RoutineSet>,
    val workouts: List<Workout>,
    val workoutExercises: List<WorkoutExercise>,
    val workoutSets: List<WorkoutSet>,
    val personalRecords: List<PersonalRecord>,
    val bodyMeasurements: List<BodyMeasurement>,
    val progressPhotos: List<ProgressPhoto>
)
