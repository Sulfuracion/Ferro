package com.davidguijo.ferro.core.data.repository

import android.content.Context
import com.davidguijo.ferro.core.data.backup.DecodedBackupPayload
import com.davidguijo.ferro.core.data.backup.FerroBackupDocumentDto
import com.davidguijo.ferro.core.data.backup.FerroBackupJsonCodec
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.dao.BackupDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.domain.BackupCalculations
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BackupImportMode
import com.davidguijo.ferro.core.domain.model.BackupImportResult
import com.davidguijo.ferro.core.domain.model.BackupPreview
import com.davidguijo.ferro.core.domain.model.BackupSnapshot
import com.davidguijo.ferro.core.domain.repository.BackupRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
internal class RoomBackupRepository
internal constructor(
    private val backupDao: BackupDao,
    private val settingsRepository: SettingsRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    private val safetyBackupDir: File,
    private val appVersionProvider: () -> String,
    private val clock: Clock
) : BackupRepository {
    @Inject
    constructor(
        backupDao: BackupDao,
        settingsRepository: SettingsRepository,
        personalRecordRepository: PersonalRecordRepository,
        @ApplicationContext context: Context,
        clock: Clock
    ) : this(
        backupDao = backupDao,
        settingsRepository = settingsRepository,
        personalRecordRepository = personalRecordRepository,
        safetyBackupDir = File(context.filesDir, SAFETY_BACKUPS_DIR_NAME),
        appVersionProvider = {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: FerroBackupDocumentDto.DEFAULT_APP_VERSION
        },
        clock = clock
    )

    override suspend fun captureSnapshot(): BackupSnapshot = withContext(Dispatchers.IO) {
        val now = clock.millis()
        val settings = settingsRepository.observeSettings().first()
        BackupSnapshot(
            schemaVersion = FerroDatabase.SCHEMA_VERSION,
            appVersion = appVersionProvider(),
            exportedAtMillis = now,
            settings = settings,
            exercises = backupDao.getAllExercises().map { it.toDomain() },
            routineFolders = backupDao.getAllRoutineFolders().map { it.toDomain() },
            routines = backupDao.getAllRoutines().map { it.toDomain() },
            routineExercises = backupDao.getAllRoutineExercises().map { it.toDomain() },
            routineSets = backupDao.getAllRoutineSets().map { it.toDomain() },
            workouts = backupDao.getAllWorkouts().map { it.toDomain() },
            workoutExercises = backupDao.getAllWorkoutExercises().map { it.toDomain() },
            workoutSets = backupDao.getAllWorkoutSets().map { it.toDomain() },
            personalRecords = backupDao.getAllPersonalRecords().map { it.toDomain() },
            bodyMeasurements = backupDao.getAllBodyMeasurements().map { it.toDomain() },
            progressPhotos = backupDao.getAllProgressPhotos().map { it.toDomain() }
        )
    }

    override suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val snapshot = captureSnapshot()
        val encoded = FerroBackupJsonCodec.encode(snapshot)
        settingsRepository.setLastBackupAtMillis(snapshot.exportedAtMillis)
        encoded
    }

    override suspend fun validateAndPreviewJson(rawJson: String): Result<BackupPreview> = withContext(Dispatchers.IO) {
        FerroBackupJsonCodec.decodeAndValidate(rawJson).map { payload ->
            payload.toPreview()
        }
    }

    override suspend fun importJson(rawJson: String, mode: BackupImportMode): Result<BackupImportResult> =
        withContext(Dispatchers.IO) {
            FerroBackupJsonCodec.decodeAndValidate(rawJson).mapCatching { payload ->
                // 1. Copia automática de seguridad de los datos actuales en filesDir/backups
                val currentSnapshot = captureSnapshot()
                val safetyFile = writeAutomaticSafetyBackup(currentSnapshot)

                // 2. Normalización de claves foráneas opcionales para evitar fallos si una carpeta/rutina
                //    referenciada no está presente
                val snapshot = sanitizeForeignKeys(payload.snapshot, mode)

                // 3. Importar todas las tablas en una sola transacción de Room
                val exerciseEntities = snapshot.exercises.map { it.toEntity() }
                val folderEntities = snapshot.routineFolders.map { it.toEntity() }
                val routineEntities = snapshot.routines.map { it.toEntity() }
                val routineExerciseEntities = snapshot.routineExercises.map { it.toEntity() }
                val routineSetEntities = snapshot.routineSets.map { it.toEntity() }
                val workoutEntities = snapshot.workouts.map { it.toEntity() }
                val workoutExerciseEntities = snapshot.workoutExercises.map { it.toEntity() }
                val workoutSetEntities = snapshot.workoutSets.map { it.toEntity() }
                val personalRecordEntities = snapshot.personalRecords.map { it.toEntity() }
                val measurementEntities = snapshot.bodyMeasurements.map { it.toEntity() }
                val photoEntities = snapshot.progressPhotos.map { it.toEntity() }

                when (mode) {
                    BackupImportMode.REPLACE_ALL -> {
                        backupDao.replaceAllTables(
                            exercises = exerciseEntities,
                            routineFolders = folderEntities,
                            routines = routineEntities,
                            routineExercises = routineExerciseEntities,
                            routineSets = routineSetEntities,
                            workouts = workoutEntities,
                            workoutExercises = workoutExerciseEntities,
                            workoutSets = workoutSetEntities,
                            personalRecords = personalRecordEntities,
                            bodyMeasurements = measurementEntities,
                            progressPhotos = photoEntities
                        )
                        val preservedBackupTime =
                            currentSnapshot.settings.lastBackupAtMillis
                                ?: payload.snapshot.exportedAtMillis.takeIf { it > 0L }
                        settingsRepository.replaceSettings(
                            snapshot.settings.copy(lastBackupAtMillis = preservedBackupTime)
                        )
                    }

                    BackupImportMode.MERGE_BY_ID -> {
                        backupDao.mergeAllTables(
                            exercises = exerciseEntities,
                            routineFolders = folderEntities,
                            routines = routineEntities,
                            routineExercises = routineExerciseEntities,
                            routineSets = routineSetEntities,
                            workouts = workoutEntities,
                            workoutExercises = workoutExerciseEntities,
                            workoutSets = workoutSetEntities,
                            personalRecords = personalRecordEntities,
                            bodyMeasurements = measurementEntities,
                            progressPhotos = photoEntities
                        )
                        // Si al fusionar entrenos cambian las mejores marcas o el archivo no traía PRs, recalculamos
                        if (snapshot.personalRecords.isEmpty() && snapshot.workouts.isNotEmpty()) {
                            personalRecordRepository.recalculateRecords()
                        }
                    }
                }

                BackupImportResult(
                    preview = payload.toPreview(),
                    mode = mode,
                    safetyBackupFileName = safetyFile.name
                )
            }
        }

    override suspend fun exportSeriesCsv(
        weightUnit: WeightUnit,
        zoneId: ZoneId,
        locale: Locale,
        useEnglishExerciseNames: Boolean
    ): String = withContext(Dispatchers.IO) {
        val exercisesById = backupDao.getAllExercises().associate { it.id to it.toDomain() }
        val workouts = backupDao.getAllWorkouts().map { it.toDomain() }
        val workoutExercises = backupDao.getAllWorkoutExercises().map { it.toDomain() }
        val workoutSets = backupDao.getAllWorkoutSets().map { it.toDomain() }
        BackupCalculations.buildSeriesCsv(
            workouts = workouts,
            workoutExercises = workoutExercises,
            workoutSets = workoutSets,
            exercisesById = exercisesById,
            weightUnit = weightUnit,
            zoneId = zoneId,
            locale = locale,
            useEnglishExerciseNames = useEnglishExerciseNames
        )
    }

    override fun latestSafetyBackupFile(): File? {
        if (!safetyBackupDir.exists()) return null
        return safetyBackupDir
            .listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun writeAutomaticSafetyBackup(currentSnapshot: BackupSnapshot): File {
        safetyBackupDir.mkdirs()
        val timestamp = clock.millis()
        val file = File(safetyBackupDir, "ferro_pre_import_$timestamp.json")
        file.writeText(FerroBackupJsonCodec.encode(currentSnapshot), Charsets.UTF_8)
        pruneOldSafetyBackups()
        return file
    }

    private fun pruneOldSafetyBackups() {
        val files =
            safetyBackupDir
                .listFiles { file -> file.isFile && file.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
        if (files.size > MAX_SAFETY_BACKUPS) {
            files.drop(MAX_SAFETY_BACKUPS).forEach { it.delete() }
        }
    }

    private suspend fun sanitizeForeignKeys(incoming: BackupSnapshot, mode: BackupImportMode): BackupSnapshot {
        val existingExerciseIds =
            if (mode == BackupImportMode.MERGE_BY_ID) {
                backupDao.getAllExercises().map { it.id }.toSet()
            } else {
                emptySet()
            }
        val existingFolderIds =
            if (mode == BackupImportMode.MERGE_BY_ID) {
                backupDao.getAllRoutineFolders().map { it.id }.toSet()
            } else {
                emptySet()
            }
        val existingRoutineIds =
            if (mode == BackupImportMode.MERGE_BY_ID) {
                backupDao.getAllRoutines().map { it.id }.toSet()
            } else {
                emptySet()
            }
        val existingMeasurementIds =
            if (mode == BackupImportMode.MERGE_BY_ID) {
                backupDao.getAllBodyMeasurements().map { it.id }.toSet()
            } else {
                emptySet()
            }

        val validFolderIds = incoming.routineFolders.map { it.id }.toSet() + existingFolderIds
        val sanitizedRoutines =
            incoming.routines.map { routine ->
                if (routine.folderId != null && routine.folderId !in validFolderIds) {
                    routine.copy(folderId = null)
                } else {
                    routine
                }
            }
        val validRoutineIds = sanitizedRoutines.map { it.id }.toSet() + existingRoutineIds
        val validExerciseIds = incoming.exercises.map { it.id }.toSet() + existingExerciseIds

        val sanitizedRoutineExercises =
            incoming.routineExercises.filter { re ->
                re.routineId in validRoutineIds && re.exerciseId in validExerciseIds
            }
        val validRoutineExerciseIds = sanitizedRoutineExercises.map { it.id }.toSet()
        val sanitizedRoutineSets =
            incoming.routineSets.filter { rs ->
                rs.routineExerciseId in validRoutineExerciseIds
            }

        val sanitizedWorkouts =
            incoming.workouts.map { workout ->
                if (workout.routineId != null && workout.routineId !in validRoutineIds) {
                    workout.copy(routineId = null)
                } else {
                    workout
                }
            }
        val validWorkoutIds = sanitizedWorkouts.map { it.id }.toSet()
        val sanitizedWorkoutExercises =
            incoming.workoutExercises.filter { we ->
                we.workoutId in validWorkoutIds && we.exerciseId in validExerciseIds
            }
        val validWorkoutExerciseIds = sanitizedWorkoutExercises.map { it.id }.toSet()
        val sanitizedWorkoutSets =
            incoming.workoutSets.filter { ws ->
                ws.workoutExerciseId in validWorkoutExerciseIds
            }
        val validWorkoutSetIds = sanitizedWorkoutSets.map { it.id }.toSet()
        val sanitizedPersonalRecords =
            incoming.personalRecords.filter { pr ->
                pr.exerciseId in validExerciseIds && pr.workoutSetId in validWorkoutSetIds
            }

        val validMeasurementIds = incoming.bodyMeasurements.map { it.id }.toSet() + existingMeasurementIds
        val sanitizedPhotos =
            incoming.progressPhotos.map { photo ->
                if (photo.measurementId != null && photo.measurementId !in validMeasurementIds) {
                    photo.copy(measurementId = null)
                } else {
                    photo
                }
            }

        return incoming.copy(
            routines = sanitizedRoutines,
            routineExercises = sanitizedRoutineExercises,
            routineSets = sanitizedRoutineSets,
            workouts = sanitizedWorkouts,
            workoutExercises = sanitizedWorkoutExercises,
            workoutSets = sanitizedWorkoutSets,
            personalRecords = sanitizedPersonalRecords,
            progressPhotos = sanitizedPhotos
        )
    }

    private fun DecodedBackupPayload.toPreview(): BackupPreview = BackupPreview(
        schemaVersion = snapshot.schemaVersion,
        migratedFromSchemaVersion =
            originalSchemaVersion.takeIf { it < snapshot.schemaVersion },
        appVersion = snapshot.appVersion,
        exportedAtMillis = snapshot.exportedAtMillis,
        workoutsCount = snapshot.workouts.size,
        routinesCount = snapshot.routines.size,
        exercisesCount = snapshot.exercises.size,
        measurementsCount = snapshot.bodyMeasurements.size
    )

    companion object {
        const val SAFETY_BACKUPS_DIR_NAME = "backups"
        private const val MAX_SAFETY_BACKUPS = 5
    }
}
