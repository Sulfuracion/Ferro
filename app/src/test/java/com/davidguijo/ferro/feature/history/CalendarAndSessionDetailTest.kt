package com.davidguijo.ferro.feature.history

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.repository.DataStoreSettingsRepository
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomRoutineRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.ActiveRestTimerStore
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import com.davidguijo.ferro.feature.workout.WorkoutNotificationController
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CalendarAndSessionDetailTest {
    private val testDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneId.of("UTC"))
    private lateinit var database: FerroDatabase
    private lateinit var workoutRepository: RoomWorkoutRepository
    private lateinit var personalRecordRepository: RoomPersonalRecordRepository
    private lateinit var routineRepository: RoomRoutineRepository
    private lateinit var exerciseRepository: RoomExerciseRepository
    private lateinit var settingsRepository: DataStoreSettingsRepository
    private lateinit var activeWorkoutManager: ActiveWorkoutManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        database =
            Room
                .inMemoryDatabaseBuilder<FerroDatabase>(context = context)
                .setDriver(AndroidSQLiteDriver())
                .allowMainThreadQueries()
                .build()
        workoutRepository = RoomWorkoutRepository(database.workoutDao(), clock)
        personalRecordRepository =
            RoomPersonalRecordRepository(
                personalRecordDao = database.personalRecordDao(),
                workoutDao = database.workoutDao(),
                exerciseDao = database.exerciseDao(),
                clock = clock
            )
        routineRepository = RoomRoutineRepository(database.routineDao(), clock)
        exerciseRepository = RoomExerciseRepository(database.exerciseDao(), clock)
        val prefsFile = File(context.cacheDir, "test_prefs_${System.nanoTime()}.preferences_pb")
        val dataStore =
            androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
                produceFile = { prefsFile }
            )
        settingsRepository = DataStoreSettingsRepository(dataStore)
        activeWorkoutManager =
            ActiveWorkoutManager(
                workoutRepository = workoutRepository,
                routineRepository = routineRepository,
                personalRecordRepository = personalRecordRepository,
                settingsRepository = settingsRepository,
                activeRestTimerStore = NoOpRestTimerStore(),
                notificationController = NoOpNotificationController(),
                clock = clock,
                scope = CoroutineScope(SupervisorJob() + testDispatcher),
                debounceMillis = 0L
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `buildMonthGridCells respects firstDayOfWeek and marks trained days`() {
        val month = YearMonth.of(2026, 11) // Nov 1, 2026 is Sunday
        val trainedDay = LocalDate.of(2026, 11, 21)
        val workoutsByDate =
            mapOf(
                trainedDay to
                    listOf(
                        Workout(
                            id = "w1",
                            name = "Push",
                            routineId = null,
                            startedAt = 1_000L,
                            endedAt = 2_000L,
                            notes = "",
                            status = WorkoutStatus.COMPLETED,
                            createdAt = 1_000L,
                            updatedAt = 2_000L
                        )
                    )
            )

        val mondayCells =
            buildMonthGridCells(
                month = month,
                firstDayOfWeek = DayOfWeek.MONDAY,
                today = trainedDay,
                selectedDate = trainedDay,
                workoutsByDate = workoutsByDate
            )
        // Nov 1 is Sunday -> 6 leading empty cells when week starts Monday
        assertEquals(6, mondayCells.takeWhile { it.date == null }.size)
        val trainedCell = mondayCells.first { it.date == trainedDay }
        assertTrue(trainedCell.hasWorkouts)
        assertTrue(trainedCell.isSelected)

        val sundayCells =
            buildMonthGridCells(
                month = month,
                firstDayOfWeek = DayOfWeek.SUNDAY,
                today = trainedDay,
                selectedDate = trainedDay,
                workoutsByDate = workoutsByDate
            )
        // Nov 1 is Sunday -> 0 leading empty cells when week starts Sunday
        assertEquals(0, sundayCells.takeWhile { it.date == null }.size)
        assertEquals(
            listOf("D", "L", "M", "X", "J", "V", "S"),
            buildWeekdayLabels(DayOfWeek.SUNDAY, Locale.forLanguageTag("es"))
        )
    }

    @Test
    fun `editing and deleting session recalculates personal records`() = runTest {
        exerciseRepository.upsertExercise(
            Exercise(
                id = "ex-bench",
                nameEs = "Press de banca",
                nameEn = "Bench Press",
                primaryMuscle = Muscle.CHEST,
                secondaryMuscles = emptyList(),
                equipment = Equipment.BARBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "",
                instructionsEn = "",
                isCustom = false,
                archived = false,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        )

        val w1 = sampleCompletedWorkout("w1", startedAt = 1_000_000L, weightKg = 100.0, reps = 5)
        val w2 = sampleCompletedWorkout("w2", startedAt = 2_000_000L, weightKg = 110.0, reps = 5)
        workoutRepository.saveWorkoutWithExercises(w1)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(w1)
        workoutRepository.saveWorkoutWithExercises(w2)
        personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(w2)

        assertEquals(
            110.0,
            personalRecordRepository.getLatestRecord("ex-bench", RecordType.MAX_WEIGHT)?.value ?: 0.0,
            0.001
        )

        val viewModel =
            SessionDetailViewModel(
                workoutId = "w2",
                workoutRepository = workoutRepository,
                personalRecordRepository = personalRecordRepository,
                routineRepository = routineRepository,
                exerciseRepository = exerciseRepository,
                settingsRepository = settingsRepository,
                activeWorkoutManager = activeWorkoutManager,
                clock = clock,
                zoneId = ZoneId.of("UTC"),
                systemLanguageProvider = { "es" }
            )

        // Wait until Room + DataStore emit the loaded workout
        backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.uiState.first { it.workoutFound }

        // Edit w2 weight from 110 kg down to 90 kg and save
        viewModel.onEnterEditMode()
        viewModel.uiState.first { it.isEditing }
        viewModel.onEditSetWeightChange("we-w2", "ws-w2", "90")
        viewModel.onSaveEdit()
        viewModel.uiState.first { !it.isEditing && !it.isSavingEdit }

        // Now max weight PR must fall back to w1's 100 kg
        assertEquals(
            100.0,
            personalRecordRepository.getLatestRecord("ex-bench", RecordType.MAX_WEIGHT)?.value ?: 0.0,
            0.001
        )

        // Save as routine
        viewModel.onOpenSaveRoutineDialog()
        viewModel.onSaveRoutineNameChange("Banca Nueva")
        viewModel.onConfirmSaveRoutine()
        viewModel.uiState.first { it.savedRoutineName != null }
        val routines = routineRepository.observeAllRoutinesWithExercises().first()
        assertTrue(routines.any { it.routine.name == "Banca Nueva" })

        // Repeat workout
        viewModel.onRepeatWorkoutClick()
        viewModel.uiState.first { it.shouldOpenActiveWorkout }
        val active = workoutRepository.getActiveWorkout()
        assertNotNull(active)
        assertEquals(WorkoutStatus.IN_PROGRESS, active?.workout?.status)

        // Delete w2 and verify w2 is gone
        viewModel.onRequestDelete()
        viewModel.onConfirmDelete()
        viewModel.uiState.first { it.isDeleted }
        assertNull(workoutRepository.getWorkoutWithExercises("w2"))
    }

    private fun sampleCompletedWorkout(id: String, startedAt: Long, weightKg: Double, reps: Int): WorkoutWithExercises =
        WorkoutWithExercises(
            workout =
                Workout(
                    id = id,
                    name = "Session $id",
                    routineId = null,
                    startedAt = startedAt,
                    endedAt = startedAt + 3_600_000L,
                    notes = "",
                    status = WorkoutStatus.COMPLETED,
                    createdAt = startedAt,
                    updatedAt = startedAt
                ),
            exercises =
                listOf(
                    WorkoutExerciseWithSets(
                        workoutExercise =
                            WorkoutExercise(
                                id = "we-$id",
                                workoutId = id,
                                exerciseId = "ex-bench",
                                position = 0,
                                supersetGroup = null,
                                restSeconds = 90,
                                notes = "",
                                createdAt = startedAt,
                                updatedAt = startedAt
                            ),
                        sets =
                            listOf(
                                WorkoutSet(
                                    id = "ws-$id",
                                    workoutExerciseId = "we-$id",
                                    position = 0,
                                    setType = SetType.NORMAL,
                                    weightKg = weightKg,
                                    reps = reps,
                                    rpe = 8.0,
                                    durationS = null,
                                    distanceM = null,
                                    completed = true,
                                    completedAt = startedAt + 60_000L,
                                    createdAt = startedAt,
                                    updatedAt = startedAt
                                )
                            )
                    )
                )
        )

    private class NoOpRestTimerStore : ActiveRestTimerStore {
        private var current: RestTimerState? = null

        override suspend fun load(): RestTimerState? = current

        override suspend fun save(state: RestTimerState?) {
            current = state
        }
    }

    private class NoOpNotificationController : WorkoutNotificationController {
        override fun syncWorkoutNotification(workout: WorkoutWithExercises, restTimer: RestTimerState?) = Unit

        override fun stopWorkoutNotification() = Unit

        override fun scheduleRestAlarm(workoutId: String, workoutName: String, endAtMillis: Long) = Unit

        override fun cancelRestAlarm() = Unit

        override fun notifyRestFinished(workoutName: String, settings: Settings, isWorkoutScreenVisible: Boolean) = Unit

        override fun cancelRestFinishedNotification() = Unit

        override fun canScheduleExactAlarms(): Boolean = true
    }
}
