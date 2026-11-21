package com.davidguijo.ferro.core.data.local

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.PersonalRecordDao
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WorkoutDaoTest {
    private lateinit var database: FerroDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var personalRecordDao: PersonalRecordDao

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        exerciseDao = database.exerciseDao()
        workoutDao = database.workoutDao()
        personalRecordDao = database.personalRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeActiveWorkoutAndExerciseHistoryAndLastSession() = runTest {
        exerciseDao.upsert(ExerciseDaoTest.sampleExercise(id = "ex-bench"))

        // Sesión completada antigua
        insertWorkout(
            workoutId = "w-old",
            name = "Torso 1",
            startedAt = 10_000L,
            endedAt = 13_600L,
            status = WorkoutStatus.COMPLETED,
            workoutExerciseId = "we-old",
            exerciseId = "ex-bench",
            setId = "ws-old-1",
            weightKg = 75.0,
            reps = 8
        )
        // Sesión completada más reciente
        insertWorkout(
            workoutId = "w-recent",
            name = "Torso 2",
            startedAt = 20_000L,
            endedAt = 23_600L,
            status = WorkoutStatus.COMPLETED,
            workoutExerciseId = "we-recent",
            exerciseId = "ex-bench",
            setId = "ws-recent-1",
            weightKg = 80.0,
            reps = 6
        )
        // Sesión descartada posterior (no debe contar en historial ni en última sesión)
        insertWorkout(
            workoutId = "w-discarded",
            name = "Descartado",
            startedAt = 30_000L,
            endedAt = 30_500L,
            status = WorkoutStatus.DISCARDED,
            workoutExerciseId = "we-discarded",
            exerciseId = "ex-bench",
            setId = "ws-discarded-1",
            weightKg = 90.0,
            reps = 1
        )
        // Entreno actualmente en curso
        insertWorkout(
            workoutId = "w-active",
            name = "Torso hoy",
            startedAt = 40_000L,
            endedAt = null,
            status = WorkoutStatus.IN_PROGRESS,
            workoutExerciseId = "we-active",
            exerciseId = "ex-bench",
            setId = "ws-active-1",
            weightKg = 82.5,
            reps = 5
        )

        workoutDao.observeActiveWorkout().test {
            val active = awaitItem()
            assertNotNull(active)
            assertEquals("w-active", active?.workout?.id)
            assertEquals(1, active?.exercises?.size)
            cancelAndIgnoreRemainingEvents()
        }

        val lastSession = workoutDao.getLastSessionForExercise(exerciseId = "ex-bench")
        assertNotNull(lastSession)
        assertEquals("w-recent", lastSession?.workoutExercise?.workoutId)
        assertEquals("Torso 2", lastSession?.workoutName)
        assertEquals(80.0, lastSession?.sets?.first()?.weightKg)

        val previousBeforeRecent =
            workoutDao.getLastSessionForExercise(
                exerciseId = "ex-bench",
                excludeWorkoutId = "w-recent"
            )
        assertEquals("w-old", previousBeforeRecent?.workoutExercise?.workoutId)

        workoutDao.observeExerciseHistory("ex-bench").test {
            val history = awaitItem()
            assertEquals(listOf("w-recent", "w-old"), history.map { it.workoutExercise.workoutId })
            cancelAndIgnoreRemainingEvents()
        }

        workoutDao.observeCompletedWorkoutsInRange(startMillis = 15_000L, endMillis = 25_000L).test {
            val inRange = awaitItem()
            assertEquals(listOf("w-recent"), inRange.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveWorkoutWithExercisesPreservesExistingPersonalRecordsOnUpdate() = runTest {
        exerciseDao.upsert(ExerciseDaoTest.sampleExercise(id = "ex-squat"))
        insertWorkout(
            workoutId = "w-1",
            name = "Pierna",
            startedAt = 10_000L,
            endedAt = 14_000L,
            status = WorkoutStatus.COMPLETED,
            workoutExerciseId = "we-1",
            exerciseId = "ex-squat",
            setId = "ws-1",
            weightKg = 100.0,
            reps = 5
        )
        personalRecordDao.upsert(
            PersonalRecordEntity(
                id = "pr-1",
                exerciseId = "ex-squat",
                recordType = RecordType.MAX_WEIGHT,
                value = 100.0,
                workoutSetId = "ws-1",
                achievedAt = 14_000L,
                createdAt = 14_000L,
                updatedAt = 14_000L
            )
        )

        // Editamos notas del entreno y añadimos una serie manteniendo ws-1
        val existing = workoutDao.getWorkoutWithExercises("w-1")!!
        val updatedWorkout = existing.workout.copy(notes = "Buenas sensaciones", updatedAt = 15_000L)
        val exercise = existing.exercises.first().workoutExercise
        val existingSet = existing.exercises.first().sets.first()
        val secondSet =
            existingSet.copy(
                id = "ws-2",
                position = 1,
                weightKg = 102.5,
                updatedAt = 15_000L
            )

        workoutDao.saveWorkoutWithExercises(
            workout = updatedWorkout,
            exercises = listOf(exercise),
            sets = listOf(existingSet, secondSet)
        )

        // El récord vinculado a ws-1 debe seguir intacto tras actualizar el entreno
        val records = personalRecordDao.getRecordsForWorkout("w-1")
        assertEquals(listOf("pr-1"), records.map { it.id })

        // Al borrar el entreno, la cascada limpia ejercicios, series y récords vinculados
        workoutDao.deleteWorkoutById("w-1")
        assertNull(workoutDao.getWorkoutWithExercises("w-1"))
        assertEquals(emptyList<PersonalRecordEntity>(), personalRecordDao.getRecordsForWorkout("w-1"))
    }

    private suspend fun insertWorkout(
        workoutId: String,
        name: String,
        startedAt: Long,
        endedAt: Long?,
        status: WorkoutStatus,
        workoutExerciseId: String,
        exerciseId: String,
        setId: String,
        weightKg: Double,
        reps: Int
    ) {
        workoutDao.saveWorkoutWithExercises(
            workout =
                WorkoutEntity(
                    id = workoutId,
                    name = name,
                    routineId = null,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    notes = "",
                    status = status,
                    createdAt = startedAt,
                    updatedAt = endedAt ?: startedAt
                ),
            exercises =
                listOf(
                    WorkoutExerciseEntity(
                        id = workoutExerciseId,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        position = 0,
                        supersetGroup = null,
                        restSeconds = 90,
                        notes = "",
                        createdAt = startedAt,
                        updatedAt = startedAt
                    )
                ),
            sets =
                listOf(
                    WorkoutSetEntity(
                        id = setId,
                        workoutExerciseId = workoutExerciseId,
                        position = 0,
                        setType = SetType.NORMAL,
                        weightKg = weightKg,
                        reps = reps,
                        rpe = 8.0,
                        durationS = null,
                        distanceM = null,
                        completed = true,
                        completedAt = endedAt ?: startedAt,
                        createdAt = startedAt,
                        updatedAt = startedAt
                    )
                )
        )
    }
}
