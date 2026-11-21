package com.davidguijo.ferro.core.data.repository

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomRepositoriesTest {
    private lateinit var database: FerroDatabase
    private val fixedClock: Clock = Clock.fixed(Instant.ofEpochMilli(99_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exerciseAndRoutineRepositoriesMapDomainModelsAndUseInjectedClock() = runTest {
        val exerciseRepository = RoomExerciseRepository(database.exerciseDao(), fixedClock)
        val routineRepository = RoomRoutineRepository(database.routineDao(), fixedClock)

        val exercise =
            Exercise(
                id = "ex-1",
                nameEs = "Dominadas",
                nameEn = "Pull-ups",
                primaryMuscle = Muscle.LATS,
                secondaryMuscles = listOf(Muscle.BICEPS, Muscle.UPPER_BACK),
                equipment = Equipment.PULL_UP_BAR,
                trackingType = TrackingType.BODYWEIGHT_REPS,
                instructionsEs = "Sube el pecho hacia la barra.",
                instructionsEn = "Pull your chest toward the bar.",
                isCustom = false,
                archived = false,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        exerciseRepository.upsertExercise(exercise)

        val routineWithExercises =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "r-1",
                        folderId = null,
                        name = "Tirón",
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
                                    id = "re-1",
                                    routineId = "r-1",
                                    exerciseId = "ex-1",
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
                                        id = "rs-2",
                                        routineExerciseId = "re-1",
                                        position = 1,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = null,
                                        targetRepsMin = 8,
                                        targetRepsMax = 10,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 1_000L,
                                        updatedAt = 1_000L
                                    ),
                                    RoutineSet(
                                        id = "rs-1",
                                        routineExerciseId = "re-1",
                                        position = 0,
                                        setType = SetType.WARMUP,
                                        targetWeightKg = null,
                                        targetRepsMin = 5,
                                        targetRepsMax = 5,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 1_000L,
                                        updatedAt = 1_000L
                                    )
                                )
                        )
                    )
            )

        routineRepository.saveRoutineWithExercises(routineWithExercises)

        // Los mappers ordenan siempre las series por position
        routineRepository.observeRoutineWithExercises("r-1").test {
            val loaded = awaitItem()
            assertNotNull(loaded)
            assertEquals(listOf("rs-1", "rs-2"), loaded?.exercises?.first()?.sets?.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        val duplicated =
            routineRepository.duplicateRoutine(
                sourceRoutineId = "r-1",
                newName = "Tirón B",
                newRoutineId = "r-2"
            )
        assertNotNull(duplicated)
        assertEquals(99_000L, duplicated?.routine?.createdAt)
        assertEquals(99_000L, duplicated?.routine?.updatedAt)

        exerciseRepository.setArchived("ex-1", archived = true)
        assertEquals(99_000L, exerciseRepository.getExerciseById("ex-1")?.updatedAt)
    }
}
