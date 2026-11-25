package com.davidguijo.ferro.feature.backup

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.data.repository.DataStoreSettingsRepository
import com.davidguijo.ferro.core.data.repository.RoomBackupRepository
import com.davidguijo.ferro.core.data.repository.RoomBodyMeasurementRepository
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomRoutineRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.BackupImportMode
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.BackupException
import com.davidguijo.ferro.core.domain.repository.BackupFailureReason
import com.davidguijo.ferro.feature.settings.ProfileViewModel
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BackupRepositoryAndViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-25T18:00:00Z"), ZoneOffset.UTC)

    private lateinit var sourceDb: FerroDatabase
    private lateinit var targetDb: FerroDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sourceDb = createInMemoryFerroDatabase()
        targetDb = createInMemoryFerroDatabase()
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `exportar, importar con REPLACE_ALL y comparar que todas las tablas y ajustes son identicos`() =
        runTest(testDispatcher) {
            val sourceSettings = createSettingsRepository("source_prefs", backgroundScope)
            val sourceBackupDir = tempFolder.newFolder("source_backups")
            val sourceBackupRepo = createBackupRepository(sourceDb, sourceSettings, sourceBackupDir)

            populateFullDataset(sourceDb, sourceSettings)
            val snapshotBeforeExport = sourceBackupRepo.captureSnapshot()

            // Exportamos a JSON
            val exportedJson = sourceBackupRepo.exportJson()
            assertTrue(exportedJson.contains("\"schemaVersion\": 1"))
            assertTrue(exportedJson.contains("\"appVersion\": \"0.1.0\""))
            assertEquals(fixedClock.millis(), sourceSettings.observeSettings().first().lastBackupAtMillis)

            // Validamos vista previa antes de importar
            val targetSettings = createSettingsRepository("target_prefs", backgroundScope)
            val targetBackupDir = tempFolder.newFolder("target_backups")
            val targetBackupRepo = createBackupRepository(targetDb, targetSettings, targetBackupDir)

            // Sembramos un dato viejo en destino que debe quedar guardado en la copia automática y borrarse al reemplazar
            val extraExercise =
                Exercise(
                    id = "ex-old-only",
                    nameEs = "Ejercicio viejo",
                    nameEn = "Old exercise",
                    primaryMuscle = Muscle.ABS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BODYWEIGHT,
                    trackingType = TrackingType.BODYWEIGHT_REPS,
                    instructionsEs = "",
                    instructionsEn = "",
                    isCustom = true,
                    archived = false,
                    createdAt = 500L,
                    updatedAt = 500L
                )
            RoomExerciseRepository(targetDb.exerciseDao(), fixedClock).upsertExercise(extraExercise)

            val previewResult = targetBackupRepo.validateAndPreviewJson(exportedJson)
            assertTrue(previewResult.isSuccess)
            val preview = previewResult.getOrThrow()
            assertEquals(1, preview.schemaVersion)
            assertNull(preview.migratedFromSchemaVersion)
            assertEquals("0.1.0", preview.appVersion)
            assertEquals(1, preview.workoutsCount)
            assertEquals(1, preview.routinesCount)
            assertEquals(2, preview.exercisesCount)
            assertEquals(1, preview.measurementsCount)

            // Importamos reemplazando todo
            val importResult = targetBackupRepo.importJson(exportedJson, BackupImportMode.REPLACE_ALL)
            assertTrue(importResult.isSuccess)

            // Comprobamos que se creó la copia automática previa en filesDir/backups con el dato anterior
            val safetyFile = targetBackupRepo.latestSafetyBackupFile()
            assertNotNull(safetyFile)
            assertTrue(safetyFile!!.exists())
            assertTrue(safetyFile.readText().contains("ex-old-only"))

            // Comparamos que todas las tablas y los ajustes en destino son idénticos a los de origen
            val importedSnapshot = targetBackupRepo.captureSnapshot()
            assertEquals(snapshotBeforeExport.exercises, importedSnapshot.exercises)
            assertEquals(snapshotBeforeExport.routineFolders, importedSnapshot.routineFolders)
            assertEquals(snapshotBeforeExport.routines, importedSnapshot.routines)
            assertEquals(snapshotBeforeExport.routineExercises, importedSnapshot.routineExercises)
            assertEquals(snapshotBeforeExport.routineSets, importedSnapshot.routineSets)
            assertEquals(snapshotBeforeExport.workouts, importedSnapshot.workouts)
            assertEquals(snapshotBeforeExport.workoutExercises, importedSnapshot.workoutExercises)
            assertEquals(snapshotBeforeExport.workoutSets, importedSnapshot.workoutSets)
            assertEquals(snapshotBeforeExport.personalRecords, importedSnapshot.personalRecords)
            assertEquals(snapshotBeforeExport.bodyMeasurements, importedSnapshot.bodyMeasurements)
            assertEquals(snapshotBeforeExport.progressPhotos, importedSnapshot.progressPhotos)
            assertEquals(
                snapshotBeforeExport.settings.copy(lastBackupAtMillis = fixedClock.millis()),
                importedSnapshot.settings
            )
        }

    @Test
    fun `importar con MERGE_BY_ID conserva datos actuales y fusiona o actualiza por ID`() = runTest(testDispatcher) {
        val sourceSettings = createSettingsRepository("merge_src", backgroundScope)
        val sourceBackupRepo =
            createBackupRepository(sourceDb, sourceSettings, tempFolder.newFolder("merge_src_dir"))
        populateFullDataset(sourceDb, sourceSettings)
        val exportedJson = sourceBackupRepo.exportJson()

        val targetSettings = createSettingsRepository("merge_dst", backgroundScope)
        val targetBackupRepo =
            createBackupRepository(targetDb, targetSettings, tempFolder.newFolder("merge_dst_dir"))

        val localOnlyExercise =
            Exercise(
                id = "ex-local-custom",
                nameEs = "Press JM",
                nameEn = "JM Press",
                primaryMuscle = Muscle.TRICEPS,
                secondaryMuscles = emptyList(),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "",
                instructionsEn = "",
                isCustom = true,
                archived = false,
                createdAt = 100L,
                updatedAt = 100L
            )
        val outdatedBench =
            Exercise(
                id = "ex-bench",
                nameEs = "Banca nombre viejo",
                nameEn = "Old bench",
                primaryMuscle = Muscle.CHEST,
                secondaryMuscles = emptyList(),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "",
                instructionsEn = "",
                isCustom = false,
                archived = false,
                createdAt = 100L,
                updatedAt = 100L
            )
        RoomExerciseRepository(targetDb.exerciseDao(), fixedClock)
            .upsertExercises(listOf(localOnlyExercise, outdatedBench))

        val result = targetBackupRepo.importJson(exportedJson, BackupImportMode.MERGE_BY_ID)
        assertTrue(result.isSuccess)

        val merged = targetBackupRepo.captureSnapshot()
        // Se conserva ex-local-custom y se añaden/actualizan ex-bench y ex-squat (3 en total)
        assertEquals(setOf("ex-bench", "ex-local-custom", "ex-squat"), merged.exercises.map { it.id }.toSet())
        assertEquals("Press banca", merged.exercises.first { it.id == "ex-bench" }.nameEs)
        assertEquals(1, merged.workouts.size)
        assertEquals(1, merged.routines.size)
        assertEquals(1, merged.bodyMeasurements.size)
    }

    @Test
    fun `archivo corrupto o de esquema futuro falla sin alterar los datos existentes`() = runTest(testDispatcher) {
        val settingsRepo = createSettingsRepository("corrupt_prefs", backgroundScope)
        val backupRepo =
            createBackupRepository(sourceDb, settingsRepo, tempFolder.newFolder("corrupt_backups"))
        populateFullDataset(sourceDb, settingsRepo)
        val before = backupRepo.captureSnapshot()

        val malformedInputs =
            listOf(
                "",
                "no es json",
                "{\"foo\": \"bar\"}",
                "{\"schemaVersion\": \"uno\"}",
                "{\"schemaVersion\": 1, \"exercises\": [{\"id\": \"\"}]}"
            )

        for (corrupt in malformedInputs) {
            val previewResult = backupRepo.validateAndPreviewJson(corrupt)
            assertTrue("Expected failure for input: $corrupt", previewResult.isFailure)

            val importResult = backupRepo.importJson(corrupt, BackupImportMode.REPLACE_ALL)
            assertTrue("Expected import failure for input: $corrupt", importResult.isFailure)
        }

        // Esquema futuro (v99) -> UNSUPPORTED_FUTURE_SCHEMA
        val futureSchemaJson = """{"schemaVersion": 99, "exportedAtMillis": 1000, "exercises": []}"""
        val futureResult = backupRepo.validateAndPreviewJson(futureSchemaJson)
        assertTrue(futureResult.isFailure)
        assertEquals(
            BackupFailureReason.UNSUPPORTED_FUTURE_SCHEMA,
            (futureResult.exceptionOrNull() as? BackupException)?.reason
        )

        // Los datos originales permanecen intactos
        val after = backupRepo.captureSnapshot()
        assertEquals(before.exercises, after.exercises)
        assertEquals(before.workouts, after.workouts)
        assertEquals(before.routines, after.routines)
        assertEquals(before.bodyMeasurements, after.bodyMeasurements)
    }

    @Test
    fun `archivo de version antigua v0 se migra al esquema v1 y se importa en una transaccion`() =
        runTest(testDispatcher) {
            val settingsRepo = createSettingsRepository("legacy_prefs", backgroundScope)
            val backupRepo =
                createBackupRepository(targetDb, settingsRepo, tempFolder.newFolder("legacy_backups"))

            // Archivo con formato legacy v0: usa "version": 0, "name" único en exercise, snake_case en tablas
            val legacyV0Json =
                """
                {
                  "version": 0,
                  "app_version": "0.0.9",
                  "exportedAt": 1700000000000,
                  "settings": {
                    "weightUnit": "LB",
                    "defaultRestSeconds": 120
                  },
                  "exercises": [
                    {
                      "id": "ex-legacy-1",
                      "name": "Sentadilla barra alta",
                      "muscle": "QUADS",
                      "equipment": "BARBELL",
                      "instructions": "Baja rompiendo el paralelo."
                    }
                  ],
                  "routines": [
                    {
                      "id": "r-legacy-1",
                      "name": "Pierna Clásica",
                      "position": 0
                    }
                  ],
                  "routine_exercises": [
                    {
                      "id": "re-legacy-1",
                      "routineId": "r-legacy-1",
                      "exerciseId": "ex-legacy-1",
                      "position": 0,
                      "restSeconds": 150
                    }
                  ],
                  "routine_sets": [
                    {
                      "id": "rs-legacy-1",
                      "routineExerciseId": "re-legacy-1",
                      "position": 0,
                      "setType": "NORMAL",
                      "targetWeightKg": 100.0,
                      "targetRepsMin": 5,
                      "targetRepsMax": 5
                    }
                  ],
                  "workouts": [
                    {
                      "id": "w-legacy-1",
                      "name": "Pierna Viernes",
                      "date": 1700000000000,
                      "completed": true,
                      "notes": "Sesión v0"
                    }
                  ],
                  "workout_exercises": [
                    {
                      "id": "we-legacy-1",
                      "workoutId": "w-legacy-1",
                      "exerciseId": "ex-legacy-1",
                      "position": 0
                    }
                  ],
                  "workout_sets": [
                    {
                      "id": "ws-legacy-1",
                      "workoutExerciseId": "we-legacy-1",
                      "position": 0,
                      "setType": "NORMAL",
                      "weightKg": 100.0,
                      "reps": 5,
                      "completed": true
                    }
                  ],
                  "body_measurements": [
                    {
                      "id": "bm-legacy-1",
                      "measuredAt": 1700000000000,
                      "weightKg": 81.2
                    }
                  ]
                }
                """.trimIndent()

            val previewResult = backupRepo.validateAndPreviewJson(legacyV0Json)
            assertTrue(previewResult.isSuccess)
            val preview = previewResult.getOrThrow()
            assertEquals(1, preview.schemaVersion)
            assertEquals(0, preview.migratedFromSchemaVersion)
            assertEquals("0.0.9", preview.appVersion)
            assertEquals(1, preview.workoutsCount)
            assertEquals(1, preview.routinesCount)
            assertEquals(1, preview.exercisesCount)
            assertEquals(1, preview.measurementsCount)

            val importResult = backupRepo.importJson(legacyV0Json, BackupImportMode.REPLACE_ALL)
            assertTrue(importResult.isSuccess)

            val snapshot = backupRepo.captureSnapshot()
            assertEquals(WeightUnit.LB, snapshot.settings.weightUnit)
            assertEquals(120, snapshot.settings.defaultRestSeconds)
            val migratedExercise = snapshot.exercises.single()
            assertEquals("ex-legacy-1", migratedExercise.id)
            assertEquals("Sentadilla barra alta", migratedExercise.nameEs)
            assertEquals("Sentadilla barra alta", migratedExercise.nameEn)
            assertEquals(Muscle.QUADS, migratedExercise.primaryMuscle)
            assertEquals(TrackingType.WEIGHT_REPS, migratedExercise.trackingType)
            assertEquals("Baja rompiendo el paralelo.", migratedExercise.instructionsEs)

            val migratedWorkout = snapshot.workouts.single()
            assertEquals("w-legacy-1", migratedWorkout.id)
            assertEquals(1_700_000_000_000L, migratedWorkout.startedAt)
            assertEquals(WorkoutStatus.COMPLETED, migratedWorkout.status)
            assertEquals(1, snapshot.workoutSets.size)
            assertEquals(81.2, snapshot.bodyMeasurements.single().weightKg!!, 1e-6)
        }

    @Test
    fun `BackupViewModel y ProfileViewModel gestionan flujo de exportacion, vista previa y aviso de 30 dias`() =
        runTest(testDispatcher) {
            val settingsRepo = createSettingsRepository("vm_prefs", backgroundScope)
            val backupDir = tempFolder.newFolder("vm_backups")
            val backupRepo = createBackupRepository(sourceDb, settingsRepo, backupDir)
            val workoutRepo = RoomWorkoutRepository(sourceDb.workoutDao(), fixedClock)
            populateFullDataset(sourceDb, settingsRepo)

            // Hace 45 días de la última copia -> Perfil debe mostrar aviso discreto (> 30 días)
            val fortyFiveDaysAgo = fixedClock.millis() - 45L * 86_400_000L
            settingsRepo.setLastBackupAtMillis(fortyFiveDaysAgo)

            val profileVm = ProfileViewModel(settingsRepo, workoutRepo, fixedClock)
            profileVm.uiState.test {
                var state = awaitItem()
                while (!state.showBackupReminder) {
                    state = awaitItem()
                }
                assertTrue(state.showBackupReminder)
                assertEquals(45, state.daysSinceLastBackup)
                cancelAndIgnoreRemainingEvents()
            }

            val backupVm = BackupViewModel(backupRepo, settingsRepo, fixedClock)
            backupVm.uiState.test {
                var state = awaitItem()
                while (!state.isBackupStale) {
                    state = awaitItem()
                }
                assertTrue(state.isBackupStale)

                // Exportar JSON actualiza lastBackupAtMillis a hoy y apaga el aviso de > 30 días
                var exportedJson = ""
                backupVm.exportJsonTo { content ->
                    exportedJson = content
                    true
                }
                while (state.feedback != BackupFeedback.ExportJsonSuccess || state.isBackupStale) {
                    state = awaitItem()
                }
                assertEquals(BackupFeedback.ExportJsonSuccess, state.feedback)
                assertFalse(state.isBackupStale)
                assertEquals(fixedClock.millis(), state.lastBackupAtMillis)

                // Exportar CSV
                var exportedCsv = ""
                backupVm.exportCsvTo(ZoneOffset.UTC, Locale.US) { content ->
                    exportedCsv = content
                    true
                }
                while (state.feedback != BackupFeedback.ExportCsvSuccess) {
                    state = awaitItem()
                }
                assertEquals(BackupFeedback.ExportCsvSuccess, state.feedback)
                assertTrue(exportedCsv.contains("Press banca"))

                // Cargar JSON en BackupViewModel prepara la vista previa y confirmar importa
                backupVm.onImportFileLoaded(exportedJson)
                while (state.pendingImport == null) {
                    state = awaitItem()
                }
                assertEquals(1, state.pendingImport.preview.workoutsCount)

                backupVm.onSelectImportMode(BackupImportMode.REPLACE_ALL)
                while (state.pendingImport?.selectedMode != BackupImportMode.REPLACE_ALL) {
                    state = awaitItem()
                }
                assertEquals(BackupImportMode.REPLACE_ALL, state.pendingImport.selectedMode)

                backupVm.onConfirmImport()
                while (state.feedback !is BackupFeedback.ImportSuccess) {
                    state = awaitItem()
                }
                assertNull(state.pendingImport)
                val importFeedback = state.feedback as BackupFeedback.ImportSuccess
                assertTrue(importFeedback.safetyBackupFileName.endsWith(".json"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createSettingsRepository(name: String, scope: CoroutineScope): DataStoreSettingsRepository {
        val file = File(tempFolder.newFolder(name), "$name.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file }
            )
        return DataStoreSettingsRepository(dataStore)
    }

    private fun createBackupRepository(
        database: FerroDatabase,
        settingsRepository: DataStoreSettingsRepository,
        safetyBackupDir: File
    ): RoomBackupRepository {
        val prRepo =
            RoomPersonalRecordRepository(
                personalRecordDao = database.personalRecordDao(),
                workoutDao = database.workoutDao(),
                exerciseDao = database.exerciseDao(),
                clock = fixedClock
            )
        return RoomBackupRepository(
            backupDao = database.backupDao(),
            settingsRepository = settingsRepository,
            personalRecordRepository = prRepo,
            safetyBackupDir = safetyBackupDir,
            appVersionProvider = { "0.1.0" },
            clock = fixedClock
        )
    }

    private suspend fun populateFullDataset(database: FerroDatabase, settingsRepository: DataStoreSettingsRepository) {
        settingsRepository.setLanguage(AppLanguage.ES)
        settingsRepository.setWeightUnit(WeightUnit.KG)
        settingsRepository.setThemeMode(ThemeMode.DARK)
        settingsRepository.setDefaultRestSeconds(105)
        settingsRepository.setFirstDayOfWeek(DayOfWeek.MONDAY)
        settingsRepository.setTimerSoundEnabled(true)
        settingsRepository.setTimerVibrationEnabled(false)

        val exerciseRepo = RoomExerciseRepository(database.exerciseDao(), fixedClock)
        val routineRepo = RoomRoutineRepository(database.routineDao(), fixedClock)
        val workoutRepo = RoomWorkoutRepository(database.workoutDao(), fixedClock)
        val prRepo =
            RoomPersonalRecordRepository(
                personalRecordDao = database.personalRecordDao(),
                workoutDao = database.workoutDao(),
                exerciseDao = database.exerciseDao(),
                clock = fixedClock
            )
        val bodyRepo = RoomBodyMeasurementRepository(database.bodyMeasurementDao())

        val bench =
            Exercise(
                id = "ex-bench",
                nameEs = "Press banca",
                nameEn = "Bench press",
                primaryMuscle = Muscle.CHEST,
                secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "Retrae escápulas.",
                instructionsEn = "Retract scapulae.",
                isCustom = false,
                archived = false,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        val squat =
            Exercise(
                id = "ex-squat",
                nameEs = "Sentadilla",
                nameEn = "Squat",
                primaryMuscle = Muscle.QUADS,
                secondaryMuscles = listOf(Muscle.GLUTES),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "Rompe el paralelo.",
                instructionsEn = "Break parallel.",
                isCustom = true,
                archived = false,
                createdAt = 1_500L,
                updatedAt = 1_500L
            )
        exerciseRepo.upsertExercises(listOf(bench, squat))

        val folder =
            RoutineFolder(
                id = "folder-1",
                name = "Fuerza",
                position = 0,
                createdAt = 2_000L,
                updatedAt = 2_000L
            )
        val routine =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "routine-1",
                        folderId = "folder-1",
                        name = "Full Body A",
                        notes = "Pesado",
                        position = 0,
                        createdAt = 2_500L,
                        updatedAt = 2_500L
                    ),
                exercises =
                    listOf(
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-1",
                                    routineId = "routine-1",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = "A",
                                    restSeconds = 120,
                                    notes = "Pausa 1s",
                                    createdAt = 2_500L,
                                    updatedAt = 2_500L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-1",
                                        routineExerciseId = "re-1",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 90.0,
                                        targetRepsMin = 5,
                                        targetRepsMax = 5,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 2_500L,
                                        updatedAt = 2_500L
                                    )
                                )
                        )
                    )
            )
        routineRepo.saveFolderWithRoutines(folder, listOf(routine))

        val workout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "workout-1",
                        name = "Full Body A",
                        routineId = "routine-1",
                        startedAt = 10_000L,
                        endedAt = 13_600L,
                        notes = "Muy sólido",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 10_000L,
                        updatedAt = 13_600L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise =
                                WorkoutExercise(
                                    id = "we-1",
                                    workoutId = "workout-1",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 120,
                                    notes = "Sin rebote",
                                    createdAt = 10_000L,
                                    updatedAt = 10_000L
                                ),
                            sets =
                                listOf(
                                    WorkoutSet(
                                        id = "ws-1",
                                        workoutExerciseId = "we-1",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = 92.5,
                                        reps = 5,
                                        rpe = 8.5,
                                        durationS = null,
                                        distanceM = null,
                                        completed = true,
                                        completedAt = 11_000L,
                                        createdAt = 10_000L,
                                        updatedAt = 11_000L
                                    )
                                )
                        )
                    )
            )
        workoutRepo.saveWorkoutWithExercises(workout)
        prRepo.upsertRecord(
            PersonalRecord(
                id = "pr-1",
                exerciseId = "ex-bench",
                recordType = RecordType.MAX_WEIGHT,
                value = 92.5,
                workoutSetId = "ws-1",
                achievedAt = 13_600L,
                createdAt = 13_600L,
                updatedAt = 13_600L
            )
        )

        bodyRepo.upsertMeasurement(
            BodyMeasurement(
                id = "bm-1",
                measuredAt = 20_000L,
                weightKg = 80.4,
                bodyFatPct = 14.0,
                neckCm = 39.0,
                chestCm = 104.0,
                waistCm = 81.0,
                hipsCm = 96.0,
                armLCm = 37.0,
                armRCm = 37.2,
                thighLCm = 59.0,
                thighRCm = 59.0,
                calfCm = 38.5,
                notes = "Mañana",
                createdAt = 20_000L,
                updatedAt = 20_000L
            )
        )
        bodyRepo.upsertPhoto(
            ProgressPhoto(
                id = "photo-1",
                takenAt = 20_500L,
                fileName = "front_1.jpg",
                pose = "front",
                measurementId = "bm-1",
                createdAt = 20_500L,
                updatedAt = 20_500L
            )
        )
    }
}
