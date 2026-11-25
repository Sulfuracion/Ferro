package com.davidguijo.ferro.feature.progress

import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.ExerciseChartMetric
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.RecoveryStatus
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
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.feature.exercises.ExerciseDetailViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProgressStatsDaoAndViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val utc = ZoneOffset.UTC
    private val nowMillis = LocalDate.of(2026, 11, 21).atTime(18, 0).toInstant(utc).toEpochMilli()
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(nowMillis), utc)

    private lateinit var database: FerroDatabase
    private lateinit var exerciseRepository: RoomExerciseRepository
    private lateinit var workoutRepository: RoomWorkoutRepository
    private lateinit var personalRecordRepository: RoomPersonalRecordRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = createInMemoryFerroDatabase()
        exerciseRepository = RoomExerciseRepository(database.exerciseDao(), fixedClock)
        workoutRepository = RoomWorkoutRepository(database.workoutDao(), fixedClock)
        personalRecordRepository =
            RoomPersonalRecordRepository(
                personalRecordDao = database.personalRecordDao(),
                workoutDao = database.workoutDao(),
                exerciseDao = database.exerciseDao(),
                clock = fixedClock
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `ExerciseDetailViewModel expone graficas por rango y metrica, historial, records y tabla 1 a 12 reps`() =
        runTest {
            val bench =
                Exercise(
                    id = "ex-bench",
                    nameEs = "Press banca",
                    nameEn = "Bench press",
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
                    equipment = Equipment.BARBELL,
                    trackingType = TrackingType.WEIGHT_REPS,
                    instructionsEs = "",
                    instructionsEn = "",
                    isCustom = false,
                    archived = false,
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                )
            exerciseRepository.upsertExercise(bench)

            // Sesión antigua (hace 5 meses -> fuera de 3 meses, dentro de 1 año / Todo)
            val oldStart = LocalDate.of(2026, 6, 10).atTime(10, 0).toInstant(utc).toEpochMilli()
            val oldWorkout =
                completedWorkout(
                    id = "w-old",
                    name = "Torso antiguo",
                    startedAt = oldStart,
                    endedAt = oldStart + 45 * 60_000L,
                    exerciseId = "ex-bench",
                    sets =
                        listOf(
                            completedSet("s-old-warm", "we-w-old", 0, SetType.WARMUP, 60.0, 10, oldStart + 60_000L),
                            completedSet("s-old-1", "we-w-old", 1, SetType.NORMAL, 80.0, 5, oldStart + 120_000L)
                        )
                )

            // Sesión reciente (hace 2 días)
            val recentStart = LocalDate.of(2026, 11, 19).atTime(10, 0).toInstant(utc).toEpochMilli()
            val recentWorkout =
                completedWorkout(
                    id = "w-recent",
                    name = "Torso reciente",
                    startedAt = recentStart,
                    endedAt = recentStart + 60 * 60_000L,
                    exerciseId = "ex-bench",
                    sets =
                        listOf(
                            completedSet("s-rec-1", "we-w-recent", 0, SetType.NORMAL, 100.0, 5, recentStart + 60_000L),
                            completedSet("s-rec-2", "we-w-recent", 1, SetType.NORMAL, 110.0, 1, recentStart + 180_000L)
                        )
                )

            workoutRepository.saveWorkoutWithExercises(oldWorkout)
            personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(oldWorkout)
            workoutRepository.saveWorkoutWithExercises(recentWorkout)
            personalRecordRepository.evaluateAndSaveRecordsForCompletedWorkout(recentWorkout)

            val settingsRepo = FakeSettingsRepository(Settings(weightUnit = WeightUnit.KG))
            val viewModel =
                ExerciseDetailViewModel(
                    exerciseId = "ex-bench",
                    exerciseRepository = exerciseRepository,
                    workoutRepository = workoutRepository,
                    personalRecordRepository = personalRecordRepository,
                    settingsRepository = settingsRepo,
                    clock = fixedClock,
                    zoneId = utc,
                    systemLanguageProvider = { "es" }
                )

            viewModel.uiState.test {
                var state3m = awaitItem()
                while (!state3m.isLoaded) {
                    state3m = awaitItem()
                }
                // En 3 meses solo aparece la sesión reciente
                assertEquals(1, state3m.chartPoints.size)
                assertEquals("w-recent", state3m.chartPoints.first().workoutId)
                // 100 * (1 + 5/30) = 116.6667 > 110 (1 rep)
                assertEquals(100.0 * (1.0 + 5.0 / 30.0), state3m.chartPoints.first().valueKg, 1e-3)
                assertEquals(2, state3m.historyEntries.size)
                assertEquals(110.0, state3m.repMaxTable.getValue(1).weightKg, 1e-4)
                assertEquals(100.0, state3m.repMaxTable.getValue(5).weightKg, 1e-4)
                assertTrue(state3m.bestRecords.any { it.record.recordType == RecordType.MAX_WEIGHT })

                // Cambiar a rango ALL incluye ambas sesiones en orden cronológico
                viewModel.onSelectRange(ProgressTimeRange.ALL)
                var stateAll = awaitItem()
                while (stateAll.selectedRange != ProgressTimeRange.ALL || stateAll.chartPoints.size < 2) {
                    stateAll = awaitItem()
                }
                assertEquals(listOf("w-old", "w-recent"), stateAll.chartPoints.map { it.workoutId })

                // Cambiar métrica a SESSION_VOLUME (ignora calentamiento)
                viewModel.onSelectMetric(ExerciseChartMetric.SESSION_VOLUME)
                var stateVol = awaitItem()
                while (stateVol.selectedMetric != ExerciseChartMetric.SESSION_VOLUME) {
                    stateVol = awaitItem()
                }
                assertEquals(listOf(400.0, 610.0), stateVol.chartPoints.map { it.valueKg })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ProgressViewModel agrega volumen semanal, series por grupo muscular y recuperacion desde SQL`() = runTest {
        val bench =
            Exercise(
                id = "ex-bench",
                nameEs = "Press banca",
                nameEn = "Bench press",
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
        exerciseRepository.upsertExercise(bench)

        // Esta semana (hace 24h): 2 series efectivas + 1 calentamiento (debe excluirse)
        val thisWeekStart = nowMillis - 24 * 3_600_000L
        val w1 =
            completedWorkout(
                id = "w-this-week",
                name = "Empuje",
                startedAt = thisWeekStart,
                endedAt = thisWeekStart + 50 * 60_000L,
                exerciseId = "ex-bench",
                sets =
                    listOf(
                        completedSet("s-w", "we-w-this-week", 0, SetType.WARMUP, 40.0, 10, thisWeekStart + 60_000L),
                        completedSet("s-1", "we-w-this-week", 1, SetType.NORMAL, 100.0, 5, thisWeekStart + 120_000L),
                        completedSet("s-2", "we-w-this-week", 2, SetType.NORMAL, 100.0, 5, thisWeekStart + 180_000L)
                    )
            )

        // Semana pasada: 4 series efectivas
        val prevWeekStart = LocalDate.of(2026, 11, 11).atTime(10, 0).toInstant(utc).toEpochMilli()
        val w2 =
            completedWorkout(
                id = "w-prev-week",
                name = "Empuje previo",
                startedAt = prevWeekStart,
                endedAt = prevWeekStart + 70 * 60_000L,
                exerciseId = "ex-bench",
                sets =
                    listOf(
                        completedSet("s-p1", "we-w-prev-week", 0, SetType.NORMAL, 90.0, 5, prevWeekStart + 60_000L),
                        completedSet("s-p2", "we-w-prev-week", 1, SetType.NORMAL, 90.0, 5, prevWeekStart + 120_000L),
                        completedSet("s-p3", "we-w-prev-week", 2, SetType.NORMAL, 90.0, 5, prevWeekStart + 180_000L),
                        completedSet("s-p4", "we-w-prev-week", 3, SetType.NORMAL, 90.0, 5, prevWeekStart + 240_000L)
                    )
            )

        workoutRepository.saveWorkoutWithExercises(w1)
        workoutRepository.saveWorkoutWithExercises(w2)

        val settingsRepo = FakeSettingsRepository(Settings(firstDayOfWeek = DayOfWeek.MONDAY))
        val viewModel =
            ProgressViewModel(
                workoutRepository = workoutRepository,
                settingsRepository = settingsRepo,
                clock = fixedClock,
                zoneId = utc
            )

        viewModel.uiState.test {
            var state = awaitItem()
            while (!state.isLoaded) {
                state = awaitItem()
            }
            assertTrue(state.isLoaded)
            assertTrue(state.hasAnyData)
            assertEquals(12, state.weeklyVolume.size)
            assertEquals(1_000.0, state.weeklyVolume.last().volumeKg, 1e-4)
            assertEquals(1_800.0, state.weeklyVolume[10].volumeKg, 1e-4)
            assertEquals(1, state.summaryStats.workoutsThisWeek)
            assertEquals(60, state.summaryStats.avgDurationMinutes)

            val byMuscle = state.muscleVolumeComparison.associateBy { it.muscle }
            assertEquals(2.0, byMuscle.getValue(Muscle.CHEST).thisWeekSets, 1e-4)
            assertEquals(1.0, byMuscle.getValue(Muscle.CHEST).previous4WeeksAvgSets, 1e-4)
            assertEquals(1.0, byMuscle.getValue(Muscle.TRICEPS).thisWeekSets, 1e-4)
            assertEquals(0.5, byMuscle.getValue(Muscle.TRICEPS).previous4WeeksAvgSets, 1e-4)

            val recoveryByMuscle = state.muscleRecovery.associateBy { it.muscle }
            assertEquals(RecoveryStatus.RECENT, recoveryByMuscle.getValue(Muscle.CHEST).status)
            assertEquals(RecoveryStatus.RECENT, recoveryByMuscle.getValue(Muscle.TRICEPS).status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun completedWorkout(
        id: String,
        name: String,
        startedAt: Long,
        endedAt: Long,
        exerciseId: String,
        sets: List<WorkoutSet>
    ): WorkoutWithExercises = WorkoutWithExercises(
        workout =
            Workout(
                id = id,
                name = name,
                routineId = null,
                startedAt = startedAt,
                endedAt = endedAt,
                notes = "",
                status = WorkoutStatus.COMPLETED,
                createdAt = startedAt,
                updatedAt = endedAt
            ),
        exercises =
            listOf(
                WorkoutExerciseWithSets(
                    workoutExercise =
                        WorkoutExercise(
                            id = "we-$id",
                            workoutId = id,
                            exerciseId = exerciseId,
                            position = 0,
                            supersetGroup = null,
                            restSeconds = null,
                            notes = "",
                            createdAt = startedAt,
                            updatedAt = endedAt
                        ),
                    sets = sets
                )
            )
    )

    private fun completedSet(
        id: String,
        workoutExerciseId: String,
        position: Int,
        setType: SetType,
        weightKg: Double,
        reps: Int,
        completedAt: Long
    ): WorkoutSet = WorkoutSet(
        id = id,
        workoutExerciseId = workoutExerciseId,
        position = position,
        setType = setType,
        weightKg = weightKg,
        reps = reps,
        rpe = null,
        durationS = null,
        distanceM = null,
        completed = true,
        completedAt = completedAt,
        createdAt = completedAt,
        updatedAt = completedAt
    )

    private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<Settings> = state

        override suspend fun setLanguage(language: AppLanguage) {
            state.update { it.copy(language = language) }
        }

        override suspend fun setWeightUnit(unit: WeightUnit) {
            state.update { it.copy(weightUnit = unit) }
        }

        override suspend fun setThemeMode(mode: ThemeMode) = Unit

        override suspend fun setDefaultRestSeconds(seconds: Int) = Unit

        override suspend fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
            state.update { it.copy(firstDayOfWeek = dayOfWeek) }
        }

        override suspend fun setTimerSoundEnabled(enabled: Boolean) = Unit

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) = Unit
    }
}
