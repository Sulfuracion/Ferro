package com.davidguijo.ferro.feature.exercises

import app.cash.turbine.test
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(5_000L), ZoneOffset.UTC)

    private val bench =
        sampleExercise(
            id = "bench",
            nameEs = "Press banca con barra",
            nameEn = "Barbell bench press",
            primaryMuscle = Muscle.CHEST,
            equipment = Equipment.BARBELL
        )
    private val curl =
        sampleExercise(
            id = "curl",
            nameEs = "Curl de bíceps con mancuernas",
            nameEn = "Dumbbell biceps curl",
            primaryMuscle = Muscle.BICEPS,
            equipment = Equipment.DUMBBELL
        )
    private val archivedFly =
        sampleExercise(
            id = "fly",
            nameEs = "Aperturas con mancuernas",
            nameEn = "Dumbbell chest fly",
            primaryMuscle = Muscle.CHEST,
            equipment = Equipment.DUMBBELL,
            archived = true
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
    fun `filtra por búsqueda normalizada, músculo, equipo y archivados`() = runTest {
        val exerciseRepo = FakeExerciseRepository(listOf(bench, curl, archivedFly))
        val settingsRepo = FakeSettingsRepository(Settings(language = AppLanguage.ES))
        val viewModel =
            ExerciseLibraryViewModel(
                selectionMode = ExerciseSelectionMode.NONE,
                exerciseRepository = exerciseRepo,
                settingsRepository = settingsRepo,
                clock = fixedClock,
                systemLanguageProvider = { "es" }
            )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(listOf("curl", "bench"), initial.groups.flatMap { g -> g.exercises.map { it.id } })

            viewModel.onQueryChanged("biceps")
            val byQuery = awaitItem()
            assertEquals(listOf("curl"), byQuery.groups.flatMap { g -> g.exercises.map { it.id } })

            viewModel.onQueryChanged("")
            awaitItem()

            viewModel.onMuscleSelected(Muscle.CHEST)
            val byMuscle = awaitItem()
            assertEquals(listOf("bench"), byMuscle.groups.flatMap { g -> g.exercises.map { it.id } })

            viewModel.onToggleShowArchived()
            val withArchived = awaitItem()
            assertEquals(listOf("fly", "bench"), withArchived.groups.flatMap { g -> g.exercises.map { it.id } })

            viewModel.onEquipmentSelected(Equipment.BARBELL)
            val byEquipment = awaitItem()
            assertEquals(listOf("bench"), byEquipment.groups.flatMap { g -> g.exercises.map { it.id } })

            // Volver a pulsar el filtro activo lo deselecciona
            viewModel.onEquipmentSelected(Equipment.BARBELL)
            val clearedEquipment = awaitItem()
            assertEquals(listOf("fly", "bench"), clearedEquipment.groups.flatMap { g -> g.exercises.map { it.id } })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `en inglés busca por nombre en inglés y no en español`() = runTest {
        val exerciseRepo = FakeExerciseRepository(listOf(bench, curl))
        val settingsRepo = FakeSettingsRepository(Settings(language = AppLanguage.EN))
        val viewModel =
            ExerciseLibraryViewModel(
                selectionMode = ExerciseSelectionMode.NONE,
                exerciseRepository = exerciseRepo,
                settingsRepository = settingsRepo,
                clock = fixedClock,
                systemLanguageProvider = { "en" }
            )

        viewModel.uiState.test {
            awaitItem()

            viewModel.onQueryChanged("bench")
            assertEquals(
                listOf("bench"),
                awaitItem().groups.flatMap { g -> g.exercises.map { it.id } }
            )

            viewModel.onQueryChanged("press banca")
            assertEquals(
                emptyList<String>(),
                awaitItem().groups.flatMap { g -> g.exercises.map { it.id } }
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `modo selector múltiple alterna selección y crear personalizado añade ejercicio`() = runTest {
        val exerciseRepo = FakeExerciseRepository(listOf(bench, curl))
        val settingsRepo = FakeSettingsRepository(Settings(language = AppLanguage.ES))
        val viewModel =
            ExerciseLibraryViewModel(
                selectionMode = ExerciseSelectionMode.MULTIPLE,
                exerciseRepository = exerciseRepo,
                settingsRepository = settingsRepo,
                clock = fixedClock,
                systemLanguageProvider = { "es" }
            )

        viewModel.uiState.test {
            awaitItem()

            viewModel.onToggleExerciseSelection("bench")
            assertEquals(setOf("bench"), awaitItem().selectedIds)

            viewModel.onToggleExerciseSelection("curl")
            assertEquals(setOf("bench", "curl"), awaitItem().selectedIds)

            viewModel.onToggleExerciseSelection("bench")
            assertEquals(setOf("curl"), awaitItem().selectedIds)

            viewModel.onCreateCustomExercise(
                CustomExerciseDraft(
                    name = "Press landmine",
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.SHOULDERS),
                    equipment = Equipment.BARBELL,
                    trackingType = TrackingType.WEIGHT_REPS,
                    instructions = "1. Empuja arriba."
                )
            )
            val afterCreate = awaitItem()
            val custom = afterCreate.groups.flatMap { it.exercises }.firstOrNull { it.isCustom }
            assertTrue(custom != null)
            assertEquals("Press landmine", custom?.nameEs)
            assertEquals("Press landmine", custom?.nameEn)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleExercise(
        id: String,
        nameEs: String,
        nameEn: String,
        primaryMuscle: Muscle,
        equipment: Equipment,
        archived: Boolean = false
    ): Exercise = Exercise(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        trackingType = TrackingType.WEIGHT_REPS,
        instructionsEs = "",
        instructionsEn = "",
        isCustom = false,
        archived = archived,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )

    private class FakeExerciseRepository(initial: List<Exercise>) : ExerciseRepository {
        private val items = MutableStateFlow(initial)

        override fun observeActiveExercises(): Flow<List<Exercise>> =
            items.map { list -> list.filterNot { it.archived } }

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

        override suspend fun upsertExercise(exercise: Exercise) {
            items.update { list ->
                val idx = list.indexOfFirst { it.id == exercise.id }
                if (idx >= 0) list.toMutableList().apply { set(idx, exercise) } else list + exercise
            }
        }

        override suspend fun upsertExercises(exercises: List<Exercise>) {
            exercises.forEach { upsertExercise(it) }
        }

        override suspend fun insertMissingExercises(exercises: List<Exercise>): Int {
            val existing = items.value.map { it.id }.toSet()
            val missing = exercises.filter { it.id !in existing }
            items.update { it + missing }
            return missing.size
        }

        override suspend fun setArchived(id: String, archived: Boolean) {
            items.update { list ->
                list.map { if (it.id == id) it.copy(archived = archived) else it }
            }
        }

        override suspend fun deleteExercise(id: String) {
            items.update { list -> list.filterNot { it.id == id } }
        }
    }

    private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<Settings> = state

        override suspend fun setLanguage(language: AppLanguage) {
            state.update { it.copy(language = language) }
        }

        override suspend fun setWeightUnit(unit: WeightUnit) = Unit

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override suspend fun setDefaultRestSeconds(seconds: Int) = Unit

        override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) = Unit

        override suspend fun setTimerSoundEnabled(enabled: Boolean) = Unit

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) = Unit
    }
}
