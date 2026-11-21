package com.davidguijo.ferro.feature.routines

import app.cash.turbine.test
import com.davidguijo.ferro.core.domain.MoveDirection
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineTemplate
import com.davidguijo.ferro.core.domain.model.RoutineTemplateExercise
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineTemplateSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesAndEditorViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(20_000L), ZoneOffset.UTC)

    private val bench =
        Exercise(
            id = "ex-bench",
            nameEs = "Press banca con barra",
            nameEn = "Barbell bench press",
            primaryMuscle = Muscle.CHEST,
            secondaryMuscles = listOf(Muscle.TRICEPS),
            equipment = Equipment.BARBELL,
            trackingType = TrackingType.WEIGHT_REPS,
            instructionsEs = "",
            instructionsEn = "",
            isCustom = false,
            archived = false,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

    private val row =
        Exercise(
            id = "ex-row",
            nameEs = "Remo con barra",
            nameEn = "Barbell row",
            primaryMuscle = Muscle.UPPER_BACK,
            secondaryMuscles = listOf(Muscle.LATS),
            equipment = Equipment.BARBELL,
            trackingType = TrackingType.WEIGHT_REPS,
            instructionsEs = "",
            instructionsEn = "",
            isCustom = false,
            archived = false,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

    private val plank =
        Exercise(
            id = "ex-plank",
            nameEs = "Plancha frontal",
            nameEn = "Front plank",
            primaryMuscle = Muscle.ABS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            trackingType = TrackingType.DURATION,
            instructionsEs = "",
            instructionsEn = "",
            isCustom = false,
            archived = false,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RoutinesViewModel gestiona carpetas, reordenacion, mover de carpeta y copiar plantillas`() = runTest {
        val singleSet =
            listOf(
                RoutineTemplateSet(
                    setType = SetType.NORMAL,
                    targetRepsMin = 5,
                    targetRepsMax = 5
                )
            )
        val sampleProgram =
            RoutineTemplateProgram(
                id = "tpl-5x5",
                nameEs = "Fuerza 5x5",
                nameEn = "Strength 5x5",
                descriptionEs = "Fuerza básica",
                descriptionEn = "Basic strength",
                routines =
                    listOf(
                        RoutineTemplate(
                            id = "tpl-a",
                            nameEs = "Fuerza A",
                            nameEn = "Strength A",
                            notesEs = "",
                            notesEn = "",
                            exercises =
                                listOf(
                                    RoutineTemplateExercise(
                                        exerciseId = bench.id,
                                        supersetGroup = null,
                                        restSeconds = 180,
                                        notesEs = "",
                                        notesEn = "",
                                        sets = singleSet
                                    )
                                )
                        ),
                        RoutineTemplate(
                            id = "tpl-b",
                            nameEs = "Fuerza B",
                            nameEn = "Strength B",
                            notesEs = "",
                            notesEn = "",
                            exercises =
                                listOf(
                                    RoutineTemplateExercise(
                                        exerciseId = row.id,
                                        supersetGroup = null,
                                        restSeconds = 180,
                                        notesEs = "",
                                        notesEn = "",
                                        sets = singleSet
                                    )
                                )
                        )
                    )
            )
        val routineRepo = FakeRoutineRepository(templates = listOf(sampleProgram))
        val exerciseRepo = FakeExerciseRepository(listOf(bench, row, plank))
        val settingsRepo = FakeSettingsRepository(Settings(language = AppLanguage.ES))

        val viewModel =
            RoutinesViewModel(
                routineRepository = routineRepo,
                exerciseRepository = exerciseRepo,
                settingsRepository = settingsRepo,
                clock = fixedClock,
                systemLanguageProvider = { "es" }
            )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(1, initial.templatePrograms.size)

            // Copiar programa completo crea carpeta y 2 rutinas
            viewModel.onCopyTemplateProgram(sampleProgram)
            val afterCopy = awaitItem()
            assertEquals(1, afterCopy.folderSections.size)
            val section = afterCopy.folderSections.first()
            assertEquals("Fuerza 5x5", section.folder.name)
            assertEquals(listOf("Fuerza A", "Fuerza B"), section.routines.map { it.routineWithExercises.routine.name })

            // Plegar y desplegar carpeta
            viewModel.onToggleFolderExpanded(section.folder.id)
            assertFalse(awaitItem().folderSections.first().isExpanded)
            viewModel.onToggleFolderExpanded(section.folder.id)
            assertTrue(awaitItem().folderSections.first().isExpanded)

            // Renombrar carpeta
            viewModel.onOpenRenameFolder(section.folder)
            awaitItem()
            viewModel.onSaveFolder("Fuerza Lineal")
            val afterRename = awaitItem()
            assertEquals("Fuerza Lineal", afterRename.folderSections.first().folder.name)

            // Reordenar rutinas dentro de la carpeta
            val firstRoutineId = afterRename.folderSections.first().routines[0].routineWithExercises.routine.id
            viewModel.onMoveRoutine(firstRoutineId, MoveDirection.DOWN)
            val afterMove = awaitItem()
            assertEquals(
                listOf("Fuerza B", "Fuerza A"),
                afterMove.folderSections.first().routines.map { it.routineWithExercises.routine.name }
            )

            // Mover Fuerza A a "Sin carpeta" (null)
            viewModel.onRequestMoveRoutineToFolder(firstRoutineId)
            awaitItem()
            viewModel.onConfirmMoveRoutineToFolder(null)
            val afterMoveOut = awaitItem()
            assertEquals(1, afterMoveOut.folderSections.first().routines.size)
            assertEquals(1, afterMoveOut.uncategorizedRoutines.size)
            assertEquals("Fuerza A", afterMoveOut.uncategorizedRoutines.first().routineWithExercises.routine.name)

            // Borrar carpeta mueve sus rutinas restantes a "Sin carpeta"
            viewModel.onRequestDeleteFolder(section.folder.id)
            awaitItem()
            viewModel.onConfirmDeleteFolder()
            val afterDeleteFolder = awaitItem()
            assertTrue(afterDeleteFolder.folderSections.isEmpty())
            assertEquals(2, afterDeleteFolder.uncategorizedRoutines.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RoutineEditorViewModel edita ejercicios, superseries, convierte lb a kg y avisa cambios sin guardar`() =
        runTest {
            val routineRepo = FakeRoutineRepository()
            val exerciseRepo = FakeExerciseRepository(listOf(bench, row, plank))
            val settingsRepo =
                FakeSettingsRepository(
                    Settings(
                        language = AppLanguage.ES,
                        weightUnit = WeightUnit.LB,
                        defaultRestSeconds = 90
                    )
                )

            val viewModel =
                RoutineEditorViewModel(
                    initialRoutineId = null,
                    savedStateHandle = null,
                    routineRepository = routineRepo,
                    exerciseRepository = exerciseRepo,
                    settingsRepository = settingsRepo,
                    clock = fixedClock,
                    systemLanguageProvider = { "es" }
                )

            viewModel.uiState.test {
                val initial = awaitItem()
                assertFalse(initial.hasUnsavedChanges)

                // Añadir ejercicios marca cambios sin guardar
                viewModel.onNameChanged("Torso Pesado")
                assertTrue(awaitItem().hasUnsavedChanges)

                viewModel.onAddExercises(listOf(bench.id, row.id, plank.id))
                val withExercises = awaitItem()
                assertEquals(3, withExercises.exercises.size)

                // Solicitar volver con cambios sin guardar muestra diálogo de confirmación
                viewModel.onRequestBack()
                assertTrue(awaitItem().showDiscardChangesDialog)
                viewModel.onDismissDiscardChangesDialog()
                assertFalse(awaitItem().showDiscardChangesDialog)

                // Seleccionar los dos primeros ejercicios (consecutivos) y agrupar en superserie
                val ex0Id = withExercises.exercises[0].id
                val ex1Id = withExercises.exercises[1].id
                viewModel.onToggleSupersetSelection(ex0Id)
                awaitItem()
                viewModel.onToggleSupersetSelection(ex1Id)
                val withSelection = awaitItem()
                assertTrue(withSelection.canGroupSelectedAsSuperset)

                viewModel.onGroupSelectedAsSuperset()
                val grouped = awaitItem()
                assertEquals(listOf("A", "A", null), grouped.exercises.map { it.supersetGroup })

                // Editar serie con 225 lb y guardar -> se persiste en kg en el repositorio
                val benchSetId = grouped.exercises[0].sets.first().id
                viewModel.onSetWeightChanged(ex0Id, benchSetId, "225")
                awaitItem()
                viewModel.onSetRepsMinChanged(ex0Id, benchSetId, "5")
                awaitItem()
                viewModel.onSetRepsMaxChanged(ex0Id, benchSetId, "5")
                awaitItem()

                viewModel.onSaveRoutine()
                val savedState = awaitItem()
                assertTrue(savedState.isFinished)

                val savedRoutine = routineRepo.getRoutineWithExercises(savedState.routineId!!)
                assertNotNull(savedRoutine)
                assertEquals("Torso Pesado", savedRoutine?.routine?.name)
                assertEquals(3, savedRoutine?.exercises?.size)
                assertEquals("A", savedRoutine?.exercises?.get(0)?.routineExercise?.supersetGroup)
                assertEquals("A", savedRoutine?.exercises?.get(1)?.routineExercise?.supersetGroup)
                assertNull(savedRoutine?.exercises?.get(2)?.routineExercise?.supersetGroup)

                val savedBenchSet = savedRoutine?.exercises?.get(0)?.sets?.first()
                assertEquals(Units.lbToKg(225.0), savedBenchSet?.targetWeightKg ?: 0.0, 1e-4)
                assertEquals(5, savedBenchSet?.targetRepsMin)
                assertEquals(5, savedBenchSet?.targetRepsMax)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private class FakeRoutineRepository(private val templates: List<RoutineTemplateProgram> = emptyList()) :
        RoutineRepository {
        private val folders = MutableStateFlow<List<RoutineFolder>>(emptyList())
        private val routinesWithExercises = MutableStateFlow<List<RoutineWithExercises>>(emptyList())

        override fun observeFolders(): Flow<List<RoutineFolder>> = folders

        override fun observeRoutines(): Flow<List<Routine>> =
            routinesWithExercises.map { list -> list.map { it.routine } }

        override fun observeAllRoutinesWithExercises(): Flow<List<RoutineWithExercises>> = routinesWithExercises

        override fun observeRoutinesInFolder(folderId: String?): Flow<List<Routine>> =
            routinesWithExercises.map { list ->
                list.map { it.routine }.filter { it.folderId == folderId }
            }

        override fun observeRoutineWithExercises(routineId: String): Flow<RoutineWithExercises?> =
            routinesWithExercises.map { list -> list.firstOrNull { it.routine.id == routineId } }

        override suspend fun getRoutineWithExercises(routineId: String): RoutineWithExercises? =
            routinesWithExercises.value.firstOrNull { it.routine.id == routineId }

        override suspend fun upsertFolder(folder: RoutineFolder) {
            folders.update { current ->
                val idx = current.indexOfFirst { it.id == folder.id }
                if (idx >= 0) current.toMutableList().apply { set(idx, folder) } else current + folder
            }
        }

        override suspend fun deleteFolder(folderId: String) {
            folders.update { list -> list.filterNot { it.id == folderId } }
            routinesWithExercises.update { list ->
                list.map { rwe ->
                    if (rwe.routine.folderId == folderId) {
                        rwe.copy(routine = rwe.routine.copy(folderId = null))
                    } else {
                        rwe
                    }
                }
            }
        }

        override suspend fun upsertRoutines(routines: List<Routine>) {
            val byId = routines.associateBy { it.id }
            routinesWithExercises.update { list ->
                list.map { rwe ->
                    val updated = byId[rwe.routine.id]
                    if (updated != null) rwe.copy(routine = updated) else rwe
                }
            }
        }

        override suspend fun saveRoutineWithExercises(routineWithExercises: RoutineWithExercises) {
            this.routinesWithExercises.update { current ->
                val idx = current.indexOfFirst { it.routine.id == routineWithExercises.routine.id }
                if (idx >= 0) {
                    current.toMutableList().apply { set(idx, routineWithExercises) }
                } else {
                    current + routineWithExercises
                }
            }
        }

        override suspend fun saveFolderWithRoutines(
            folder: RoutineFolder?,
            routinesWithExercises: List<RoutineWithExercises>
        ) {
            if (folder != null) {
                upsertFolder(folder)
            }
            routinesWithExercises.forEach { saveRoutineWithExercises(it) }
        }

        override suspend fun duplicateRoutine(
            sourceRoutineId: String,
            newName: String,
            newRoutineId: String?
        ): RoutineWithExercises? {
            val source = getRoutineWithExercises(sourceRoutineId) ?: return null
            val copyId = newRoutineId ?: UUID.randomUUID().toString()
            val copy =
                source.copy(
                    routine = source.routine.copy(id = copyId, name = newName)
                )
            saveRoutineWithExercises(copy)
            return copy
        }

        override suspend fun deleteRoutine(routineId: String) {
            routinesWithExercises.update { list -> list.filterNot { it.routine.id == routineId } }
        }

        override suspend fun getTemplatePrograms(): List<RoutineTemplateProgram> = templates
    }

    private class FakeExerciseRepository(initial: List<Exercise>) : ExerciseRepository {
        private val items = MutableStateFlow(initial)

        override fun observeActiveExercises(): Flow<List<Exercise>> = items

        override fun observeAllExercises(): Flow<List<Exercise>> = items

        override fun observeExerciseById(id: String): Flow<Exercise?> =
            items.map { list -> list.firstOrNull { it.id == id } }

        override fun searchExercises(
            query: String,
            muscle: Muscle?,
            equipment: Equipment?,
            includeArchived: Boolean
        ): Flow<List<Exercise>> = items

        override suspend fun getExerciseById(id: String): Exercise? = items.value.firstOrNull { it.id == id }

        override suspend fun upsertExercise(exercise: Exercise) = Unit

        override suspend fun upsertExercises(exercises: List<Exercise>) = Unit

        override suspend fun insertMissingExercises(exercises: List<Exercise>): Int = 0

        override suspend fun setArchived(id: String, archived: Boolean) = Unit

        override suspend fun deleteExercise(id: String) = Unit
    }

    private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<Settings> = state

        override suspend fun setLanguage(language: AppLanguage) = Unit

        override suspend fun setWeightUnit(unit: WeightUnit) = Unit

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override suspend fun setDefaultRestSeconds(seconds: Int) = Unit

        override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) = Unit

        override suspend fun setTimerSoundEnabled(enabled: Boolean) = Unit

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) = Unit
    }
}
