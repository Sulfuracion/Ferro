package com.davidguijo.ferro.feature.workout

import android.app.Application
import com.davidguijo.ferro.core.data.local.ExerciseDaoTest
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomRoutineRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.IncompleteSetsStrategy
import com.davidguijo.ferro.core.domain.RestTimer
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.ActiveRestTimerStore
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ActiveWorkoutManagerTest {
    private lateinit var database: FerroDatabase
    private lateinit var exerciseRepository: RoomExerciseRepository
    private lateinit var routineRepository: RoomRoutineRepository
    private lateinit var workoutRepository: RoomWorkoutRepository
    private lateinit var personalRecordRepository: RoomPersonalRecordRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var restTimerStore: FakeActiveRestTimerStore
    private lateinit var notificationController: FakeWorkoutNotificationController
    private lateinit var clock: MutableFakeClock

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        clock = MutableFakeClock(100_000L)
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
        settingsRepository = FakeSettingsRepository(Settings(defaultRestSeconds = 90))
        restTimerStore = FakeActiveRestTimerStore()
        notificationController = FakeWorkoutNotificationController()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `recuperacion de entreno en curso conserva series, placeholders, anterior y descanso`() =
        runTest(UnconfinedTestDispatcher()) {
            seedSampleExercisesAndHistory()
            val routine = insertSampleRoutine()

            // Instancia #1 (antes de matar el proceso)
            val manager1 = createManager(backgroundScope)
            val started = manager1.startFromRoutine(routine.routine.id)!!
            val benchEx = started.exercises[0]
            val rowEx = started.exercises[1]
            val benchSetId = benchEx.sets[0].id
            val rowSetId = rowEx.sets[0].id

            // Copiamos Anterior en el primer ejercicio (85 kg x 6) y marcamos serie
            val copied = manager1.copyPreviousToSet(benchEx.workoutExercise.id, benchSetId, TrackingType.WEIGHT_REPS)
            assertEquals(85.0, copied?.weightKg ?: 0.0, 1e-6)
            assertEquals(6, copied?.reps)

            // Al ser superserie (A -> A), completar bench NO arranca descanso; completar row SÍ arranca 120 s
            manager1.toggleSetCompleted(benchEx.workoutExercise.id, benchSetId)
            assertNull(manager1.state.value.restTimer)

            manager1.updateSetValues(rowEx.workoutExercise.id, rowSetId, weightKg = 72.5, updateWeight = true)
            manager1.updateSetValues(rowEx.workoutExercise.id, rowSetId, reps = 10, updateReps = true)
            manager1.toggleSetCompleted(rowEx.workoutExercise.id, rowSetId)
            assertNotNull(manager1.state.value.restTimer)
            assertEquals(120, RestTimer.remainingSeconds(manager1.state.value.restTimer!!, clock))

            manager1.flushNow()

            // Avanzamos el reloj 45 s mientras el proceso estuvo cerrado
            clock.advanceMillis(45_000L)

            // Instancia #2 (nuevo proceso sobre la misma base Room y DataStore)
            val freshNotificationController = FakeWorkoutNotificationController()
            val manager2 =
                ActiveWorkoutManager(
                    workoutRepository = workoutRepository,
                    routineRepository = routineRepository,
                    personalRecordRepository = personalRecordRepository,
                    settingsRepository = settingsRepository,
                    activeRestTimerStore = restTimerStore,
                    notificationController = freshNotificationController,
                    clock = clock,
                    scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
                    debounceMillis = 0L
                )

            manager2.restoreIfNeeded()
            val restored = manager2.state.value

            assertTrue(restored.isRestored)
            assertNotNull(restored.workout)
            assertEquals(started.workout.id, restored.workout?.workout?.id)
            assertEquals("Torso A", restored.workout?.workout?.name)
            assertEquals(2, restored.workout?.exercises?.size)

            // Serie 1 conserva datos y estado completado
            val restoredBenchSet = restored.workout?.exercises?.get(0)?.sets?.get(0)!!
            assertTrue(restoredBenchSet.completed)
            assertEquals(85.0, restoredBenchSet.weightKg ?: 0.0, 1e-6)
            assertEquals(6, restoredBenchSet.reps)

            // Placeholders de rutina reconstruidos para series restantes
            val secondBenchSetId = restored.workout.exercises[0].sets[1].id
            assertEquals(90.0, restored.targetPlaceholdersBySetId[secondBenchSetId]?.targetWeightKg ?: 0.0, 1e-6)

            // Columna "Anterior" cargada desde la última sesión completada
            val prevForBench = restored.previousSetsByWorkoutExerciseId[benchEx.workoutExercise.id]
            assertEquals(85.0, prevForBench?.get(0)?.weightKg ?: 0.0, 1e-6)

            // Temporizador de descanso restaurado con tiempo restante exacto (120 - 45 = 75 s) y alarma reprogramada
            assertNotNull(restored.restTimer)
            assertEquals(75, RestTimer.remainingSeconds(restored.restTimer!!, clock))
            assertEquals(1, freshNotificationController.scheduledAlarmsCount)
        }

    @Test
    fun `reemplazar ejercicio conserva sus series y terminar guarda en transaccion`() =
        runTest(UnconfinedTestDispatcher()) {
            seedSampleExercisesAndHistory()
            val routine = insertSampleRoutine()
            val manager = createManager(backgroundScope)

            val started = manager.startFromRoutine(routine.routine.id)!!
            val benchWeId = started.exercises[0].workoutExercise.id
            val firstSetId = started.exercises[0].sets[0].id
            val secondSetId = started.exercises[0].sets[1].id

            manager.updateSetValues(benchWeId, firstSetId, weightKg = 80.0, updateWeight = true)
            manager.updateSetValues(benchWeId, firstSetId, reps = 8, updateReps = true)
            manager.toggleSetCompleted(benchWeId, firstSetId)

            manager.updateSetValues(benchWeId, secondSetId, weightKg = 90.0, updateWeight = true)
            manager.updateSetValues(benchWeId, secondSetId, reps = 6, updateReps = true)

            // Añadir serie copia la anterior (serie 2 -> 90 kg × 6, pendiente)
            val addedSet = manager.addSet(benchWeId)
            assertNotNull(addedSet)
            assertEquals(90.0, addedSet?.weightKg ?: 0.0, 1e-6)
            assertEquals(6, addedSet?.reps)

            // Reemplazar press banca por press inclinado conservando sus 3 series (2 de rutina + 1 añadida)
            manager.replaceExercise(benchWeId, "ex-incline")
            val afterReplace = manager.state.value.workout!!.exercises[0]
            assertEquals("ex-incline", afterReplace.workoutExercise.exerciseId)
            assertEquals(3, afterReplace.sets.size)
            assertEquals(80.0, afterReplace.sets[0].weightKg ?: 0.0, 1e-6)

            // Terminar descartando series sin marcar
            clock.advanceMillis(60_000L)
            val finishedId = manager.finishWorkout(IncompleteSetsStrategy.DISCARD_INCOMPLETE)
            assertEquals(started.workout.id, finishedId)
            assertNull(manager.state.value.workout)
            assertNull(workoutRepository.getActiveWorkout())

            val savedCompleted = workoutRepository.getWorkoutWithExercises(started.workout.id)
            assertNotNull(savedCompleted)
            assertEquals(WorkoutStatus.COMPLETED, savedCompleted?.workout?.status)
            assertEquals(clock.millis(), savedCompleted?.workout?.endedAt)
            assertEquals(1, savedCompleted?.exercises?.size)
            assertEquals(1, savedCompleted?.exercises?.get(0)?.sets?.size)
            assertTrue(notificationController.stoppedWorkoutNotificationCount >= 1)

            // Al terminar un entreno se detectan y guardan automáticamente los nuevos récords en personal_records
            val savedRecords = personalRecordRepository.getRecordsForWorkout(started.workout.id)
            assertTrue(savedRecords.isNotEmpty())
            assertTrue(savedRecords.all { it.workoutSetId == firstSetId })
        }

    private fun createManager(scope: CoroutineScope): ActiveWorkoutManager = ActiveWorkoutManager(
        workoutRepository = workoutRepository,
        routineRepository = routineRepository,
        personalRecordRepository = personalRecordRepository,
        settingsRepository = settingsRepository,
        activeRestTimerStore = restTimerStore,
        notificationController = notificationController,
        clock = clock,
        scope = scope,
        debounceMillis = 0L
    )

    private suspend fun seedSampleExercisesAndHistory() {
        database.exerciseDao().upsertAll(
            listOf(
                ExerciseDaoTest.sampleExercise(id = "ex-bench", nameEs = "Press banca", nameEn = "Bench press"),
                ExerciseDaoTest.sampleExercise(id = "ex-row", nameEs = "Remo", nameEn = "Barbell row"),
                ExerciseDaoTest.sampleExercise(
                    id = "ex-incline",
                    nameEs = "Press inclinado",
                    nameEn = "Incline press"
                )
            )
        )

        // Sesión completada previa para probar "Anterior"
        workoutRepository.saveWorkoutWithExercises(
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-prev",
                        name = "Torso previo",
                        routineId = null,
                        startedAt = 10_000L,
                        endedAt = 13_000L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 10_000L,
                        updatedAt = 13_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise =
                                WorkoutExercise(
                                    id = "we-prev-bench",
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
                                        workoutExerciseId = "we-prev-bench",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = 85.0,
                                        reps = 6,
                                        rpe = 8.0,
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
        )
    }

    private suspend fun insertSampleRoutine(): RoutineWithExercises {
        val routine =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "r-torso",
                        folderId = null,
                        name = "Torso A",
                        notes = "Controlar bajada",
                        position = 0,
                        createdAt = 5_000L,
                        updatedAt = 5_000L
                    ),
                exercises =
                    listOf(
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-bench",
                                    routineId = "r-torso",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = "A",
                                    restSeconds = 90,
                                    notes = "Pausa",
                                    createdAt = 5_000L,
                                    updatedAt = 5_000L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-b1",
                                        routineExerciseId = "re-bench",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 85.0,
                                        targetRepsMin = 6,
                                        targetRepsMax = 8,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 5_000L,
                                        updatedAt = 5_000L
                                    ),
                                    RoutineSet(
                                        id = "rs-b2",
                                        routineExerciseId = "re-bench",
                                        position = 1,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 90.0,
                                        targetRepsMin = 5,
                                        targetRepsMax = 6,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 5_000L,
                                        updatedAt = 5_000L
                                    )
                                )
                        ),
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-row",
                                    routineId = "r-torso",
                                    exerciseId = "ex-row",
                                    position = 1,
                                    supersetGroup = "A",
                                    restSeconds = 120,
                                    notes = "",
                                    createdAt = 5_000L,
                                    updatedAt = 5_000L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-r1",
                                        routineExerciseId = "re-row",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 70.0,
                                        targetRepsMin = 10,
                                        targetRepsMax = 10,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 5_000L,
                                        updatedAt = 5_000L
                                    )
                                )
                        )
                    )
            )
        routineRepository.saveRoutineWithExercises(routine)
        return routine
    }

    private class MutableFakeClock(initialMillis: Long) : Clock() {
        private var currentMillis: Long = initialMillis

        fun advanceMillis(deltaMillis: Long) {
            currentMillis += deltaMillis
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis)

        override fun millis(): Long = currentMillis
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

    private class FakeActiveRestTimerStore : ActiveRestTimerStore {
        var stored: RestTimerState? = null

        override suspend fun load(): RestTimerState? = stored

        override suspend fun save(state: RestTimerState?) {
            stored = state
        }
    }

    private class FakeWorkoutNotificationController : WorkoutNotificationController {
        var scheduledAlarmsCount = 0
        var stoppedWorkoutNotificationCount = 0

        override fun syncWorkoutNotification(workout: WorkoutWithExercises, restTimer: RestTimerState?) = Unit

        override fun stopWorkoutNotification() {
            stoppedWorkoutNotificationCount += 1
        }

        override fun scheduleRestAlarm(workoutId: String, workoutName: String, endAtMillis: Long) {
            scheduledAlarmsCount += 1
        }

        override fun cancelRestAlarm() = Unit

        override fun notifyRestFinished(workoutName: String, settings: Settings, isWorkoutScreenVisible: Boolean) = Unit

        override fun cancelRestFinishedNotification() = Unit

        override fun canScheduleExactAlarms(): Boolean = true
    }
}
