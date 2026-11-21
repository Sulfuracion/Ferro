package com.davidguijo.ferro.feature.workout

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.ExerciseDaoTest
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomRoutineRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WorkoutSummaryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: FerroDatabase
    private lateinit var exerciseRepository: RoomExerciseRepository
    private lateinit var routineRepository: RoomRoutineRepository
    private lateinit var workoutRepository: RoomWorkoutRepository
    private lateinit var personalRecordRepository: RoomPersonalRecordRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(200_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = createInMemoryFerroDatabase()
        exerciseRepository = RoomExerciseRepository(database.exerciseDao(), clock)
        routineRepository = RoomRoutineRepository(database.routineDao(), clock)
        workoutRepository = RoomWorkoutRepository(database.workoutDao(), clock)
        personalRecordRepository =
            RoomPersonalRecordRepository(
                personalRecordDao = database.personalRecordDao(),
                workoutDao = database.workoutDao(),
                exerciseDao = database.exerciseDao(),
                clock = clock
            )
        settingsRepository = FakeSettingsRepository(Settings())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `resumen muestra records marcados en series, comparacion con rutina previa y actualiza rutina`() = runTest {
        database.exerciseDao().upsert(
            ExerciseDaoTest.sampleExercise(id = "ex-bench", nameEs = "Press banca", nameEn = "Bench press")
        )

        val routine =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "r-push",
                        folderId = null,
                        name = "Push A",
                        notes = "",
                        position = 0,
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    ),
                exercises =
                    listOf(
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-bench",
                                    routineId = "r-push",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 90,
                                    notes = "",
                                    createdAt = 1_000L,
                                    updatedAt = 1_000L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-1",
                                        routineExerciseId = "re-bench",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 80.0,
                                        targetRepsMin = 8,
                                        targetRepsMax = 8,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 1_000L,
                                        updatedAt = 1_000L
                                    )
                                )
                        )
                    )
            )
        routineRepository.saveRoutineWithExercises(routine)

        // Sesión completada anterior de la misma rutina (80 kg x 8, duración 30 min)
        val previousWorkout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-prev",
                        name = "Push A",
                        routineId = "r-push",
                        startedAt = 10_000L,
                        endedAt = 10_000L + 1_800_000L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 10_000L,
                        updatedAt = 10_000L + 1_800_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise =
                                WorkoutExercise(
                                    id = "we-prev",
                                    workoutId = "w-prev",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 90,
                                    notes = "",
                                    createdAt = 10_000L,
                                    updatedAt = 10_000L
                                ),
                            sets =
                                listOf(
                                    WorkoutSet(
                                        id = "ws-prev-1",
                                        workoutExerciseId = "we-prev",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = 80.0,
                                        reps = 8,
                                        rpe = null,
                                        durationS = null,
                                        distanceM = null,
                                        completed = true,
                                        completedAt = 20_000L,
                                        createdAt = 10_000L,
                                        updatedAt = 20_000L
                                    )
                                )
                        )
                    )
            )
        workoutRepository.saveWorkoutWithExercises(previousWorkout)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(previousWorkout)

        // Sesión actual de la misma rutina con récord (85 kg x 8)
        val currentWorkout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-curr",
                        name = "Push A",
                        routineId = "r-push",
                        startedAt = 50_000L,
                        endedAt = 50_000L + 1_920_000L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 50_000L,
                        updatedAt = 50_000L + 1_920_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise =
                                WorkoutExercise(
                                    id = "we-curr",
                                    workoutId = "w-curr",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 90,
                                    notes = "",
                                    createdAt = 50_000L,
                                    updatedAt = 50_000L
                                ),
                            sets =
                                listOf(
                                    WorkoutSet(
                                        id = "ws-curr-1",
                                        workoutExerciseId = "we-curr",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = 85.0,
                                        reps = 8,
                                        rpe = 8.5,
                                        durationS = null,
                                        distanceM = null,
                                        completed = true,
                                        completedAt = 60_000L,
                                        createdAt = 50_000L,
                                        updatedAt = 60_000L
                                    )
                                )
                        )
                    )
            )
        workoutRepository.saveWorkoutWithExercises(currentWorkout)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(currentWorkout)

        val viewModel =
            WorkoutSummaryViewModel(
                workoutId = "w-curr",
                workoutRepository = workoutRepository,
                routineRepository = routineRepository,
                personalRecordRepository = personalRecordRepository,
                exerciseRepository = exerciseRepository,
                settingsRepository = settingsRepository,
                clock = clock,
                systemLanguageProvider = { "es" }
            )

        viewModel.uiState.test {
            var state = awaitItem()
            while (!state.isLoaded || state.workoutName.isEmpty() || state.routineComparison == null) {
                state = awaitItem()
            }

            assertEquals("Push A", state.workoutName)
            assertEquals("32:00", state.durationText)
            assertEquals("680 kg", state.volumeText)
            assertEquals(1, state.completedSetsCount)

            // Récords batidos y marcados en la serie ws-curr-1
            assertTrue(state.brokenRecords.isNotEmpty())
            val exerciseUi = state.exercises.first()
            val setUi = exerciseUi.sets.first()
            assertTrue(setUi.isRecord)
            assertTrue(RecordType.MAX_WEIGHT in setUi.recordTypes)

            // Comparación con la última vez que se hizo esa rutina (+120 s, +40 kg, 0 series)
            val comp = state.routineComparison
            assertNotNull(comp)
            assertEquals(120_000L, comp.durationDeltaMillis)
            assertEquals(40.0, comp.volumeDeltaKg, 1e-6)
            assertEquals(0, comp.completedSetsDelta)

            // Ofrece actualizar la rutina porque el peso subió de 80 a 85 kg
            assertTrue(state.canUpdateRoutineValues)
            assertFalse(state.routineUpdated)

            viewModel.onUpdateRoutineWithWorkoutValues()

            var updatedState = awaitItem()
            while (!updatedState.routineUpdated) {
                updatedState = awaitItem()
            }
            assertTrue(updatedState.routineUpdated)
            assertFalse(updatedState.canUpdateRoutineValues)

            val savedRoutine = routineRepository.getRoutineWithExercises("r-push")
            assertEquals(85.0, savedRoutine?.exercises?.first()?.sets?.first()?.targetWeightKg ?: 0.0, 1e-6)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recalculateRecords en repositorio reconstruye personal_records al borrar una sesion`() = runTest {
        database.exerciseDao().upsert(
            ExerciseDaoTest.sampleExercise(id = "ex-squat", nameEs = "Sentadilla", nameEn = "Squat")
        )

        val w1 =
            sampleCompletedWorkout(
                workoutId = "w-1",
                startedAt = 10_000L,
                setId = "ws-1",
                weightKg = 100.0,
                reps = 5
            )
        workoutRepository.saveWorkoutWithExercises(w1)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(w1)

        val w2 =
            sampleCompletedWorkout(
                workoutId = "w-2",
                startedAt = 20_000L,
                setId = "ws-2",
                weightKg = 120.0,
                reps = 5
            )
        workoutRepository.saveWorkoutWithExercises(w2)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(w2)

        val w3 =
            sampleCompletedWorkout(
                workoutId = "w-3",
                startedAt = 30_000L,
                setId = "ws-3",
                weightKg = 110.0,
                reps = 5
            )
        workoutRepository.saveWorkoutWithExercises(w3)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(w3)

        // Inicialmente w-3 no tiene récords porque w-2 tenía 120 kg
        assertTrue(personalRecordRepository.getRecordsForWorkout("w-3").isEmpty())

        // Si borramos w-2 y recalculamos récords, w-3 pasa a tener el récord de 110 kg
        workoutRepository.deleteWorkout("w-2")
        personalRecordRepository.recalculateRecords(setOf("ex-squat"))

        val w3Records = personalRecordRepository.getRecordsForWorkout("w-3")
        assertTrue(w3Records.any { it.recordType == RecordType.MAX_WEIGHT && it.value == 110.0 })
        assertEquals(
            110.0,
            personalRecordRepository.getLatestRecord("ex-squat", RecordType.MAX_WEIGHT)?.value ?: 0.0,
            1e-6
        )
    }

    private fun sampleCompletedWorkout(workoutId: String, startedAt: Long, setId: String, weightKg: Double, reps: Int) =
        WorkoutWithExercises(
            workout =
                Workout(
                    id = workoutId,
                    name = "Pierna",
                    routineId = null,
                    startedAt = startedAt,
                    endedAt = startedAt + 3_600_000L,
                    notes = "",
                    status = WorkoutStatus.COMPLETED,
                    createdAt = startedAt,
                    updatedAt = startedAt + 3_600_000L
                ),
            exercises =
                listOf(
                    WorkoutExerciseWithSets(
                        workoutExercise =
                            WorkoutExercise(
                                id = "we-$workoutId",
                                workoutId = workoutId,
                                exerciseId = "ex-squat",
                                position = 0,
                                supersetGroup = null,
                                restSeconds = 120,
                                notes = "",
                                createdAt = startedAt,
                                updatedAt = startedAt
                            ),
                        sets =
                            listOf(
                                WorkoutSet(
                                    id = setId,
                                    workoutExerciseId = "we-$workoutId",
                                    position = 0,
                                    setType = SetType.NORMAL,
                                    weightKg = weightKg,
                                    reps = reps,
                                    rpe = null,
                                    durationS = null,
                                    distanceM = null,
                                    completed = true,
                                    completedAt = startedAt + 1_000L,
                                    createdAt = startedAt,
                                    updatedAt = startedAt + 1_000L
                                )
                            )
                    )
                )
        )

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
