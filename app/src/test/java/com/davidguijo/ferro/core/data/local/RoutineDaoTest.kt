package com.davidguijo.ferro.core.data.local

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.RoutineDao
import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineFolderEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity
import com.davidguijo.ferro.core.domain.model.SetType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoutineDaoTest {
    private lateinit var database: FerroDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var routineDao: RoutineDao

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        exerciseDao = database.exerciseDao()
        routineDao = database.routineDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveRoutineWithExercisesAndDuplicateRoutineAtomically() = runTest {
        exerciseDao.upsertAll(
            listOf(
                ExerciseDaoTest.sampleExercise(id = "ex-bench"),
                ExerciseDaoTest.sampleExercise(id = "ex-row", nameEs = "Remo", nameEn = "Row")
            )
        )
        routineDao.upsertFolder(
            RoutineFolderEntity(
                id = "folder-1",
                name = "Torso / Pierna",
                position = 0,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        )

        val routine =
            RoutineEntity(
                id = "routine-1",
                folderId = "folder-1",
                name = "Torso A",
                notes = "Enfoque fuerza",
                position = 0,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        val exercises =
            listOf(
                RoutineExerciseEntity(
                    id = "re-1",
                    routineId = "routine-1",
                    exerciseId = "ex-bench",
                    position = 0,
                    supersetGroup = "A",
                    restSeconds = 120,
                    notes = "Pausa en pecho",
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                ),
                RoutineExerciseEntity(
                    id = "re-2",
                    routineId = "routine-1",
                    exerciseId = "ex-row",
                    position = 1,
                    supersetGroup = "A",
                    restSeconds = 90,
                    notes = "",
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                )
            )
        val sets =
            listOf(
                RoutineSetEntity(
                    id = "rs-1",
                    routineExerciseId = "re-1",
                    position = 0,
                    setType = SetType.WARMUP,
                    targetWeightKg = 40.0,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetDurationS = null,
                    targetDistanceM = null,
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                ),
                RoutineSetEntity(
                    id = "rs-2",
                    routineExerciseId = "re-1",
                    position = 1,
                    setType = SetType.NORMAL,
                    targetWeightKg = 80.0,
                    targetRepsMin = 5,
                    targetRepsMax = 5,
                    targetDurationS = null,
                    targetDistanceM = null,
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                ),
                RoutineSetEntity(
                    id = "rs-3",
                    routineExerciseId = "re-2",
                    position = 0,
                    setType = SetType.NORMAL,
                    targetWeightKg = 70.0,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetDurationS = null,
                    targetDistanceM = null,
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                )
            )

        routineDao.saveRoutineWithExercises(routine, exercises, sets)

        val duplicated =
            routineDao.duplicateRoutine(
                sourceRoutineId = "routine-1",
                newRoutineId = "routine-copy",
                newName = "Torso A (copia)",
                now = 5_000L
            )

        assertNotNull(duplicated)
        val copy = duplicated!!
        assertEquals("routine-copy", copy.routine.id)
        assertEquals("Torso A (copia)", copy.routine.name)
        assertEquals("folder-1", copy.routine.folderId)
        assertEquals(1, copy.routine.position)
        assertEquals(2, copy.exercises.size)

        val sortedExercises = copy.exercises.sortedBy { it.routineExercise.position }
        assertNotEquals("re-1", sortedExercises[0].routineExercise.id)
        assertEquals("ex-bench", sortedExercises[0].routineExercise.exerciseId)
        assertEquals("A", sortedExercises[0].routineExercise.supersetGroup)
        assertEquals(2, sortedExercises[0].sets.size)
        assertEquals(SetType.WARMUP, sortedExercises[0].sets.sortedBy { it.position }[0].setType)
        assertEquals(1, sortedExercises[1].sets.size)
    }

    @Test
    fun deletingFolderSetsRoutineFolderIdToNullAndDeletingRoutineCascades() = runTest {
        exerciseDao.upsert(ExerciseDaoTest.sampleExercise(id = "ex-1"))
        routineDao.upsertFolder(
            RoutineFolderEntity(
                id = "folder-1",
                name = "Empuje",
                position = 0,
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        )
        routineDao.saveRoutineWithExercises(
            routine =
                RoutineEntity(
                    id = "routine-1",
                    folderId = "folder-1",
                    name = "Push A",
                    notes = "",
                    position = 0,
                    createdAt = 1_000L,
                    updatedAt = 1_000L
                ),
            exercises =
                listOf(
                    RoutineExerciseEntity(
                        id = "re-1",
                        routineId = "routine-1",
                        exerciseId = "ex-1",
                        position = 0,
                        supersetGroup = null,
                        restSeconds = 90,
                        notes = "",
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    )
                ),
            sets =
                listOf(
                    RoutineSetEntity(
                        id = "rs-1",
                        routineExerciseId = "re-1",
                        position = 0,
                        setType = SetType.NORMAL,
                        targetWeightKg = 60.0,
                        targetRepsMin = 8,
                        targetRepsMax = 12,
                        targetDurationS = null,
                        targetDistanceM = null,
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    )
                )
        )

        routineDao.deleteFolderById("folder-1")

        routineDao.observeRoutinesInFolder(null).test {
            val ungrouped = awaitItem()
            assertEquals(listOf("routine-1"), ungrouped.map { it.id })
            assertNull(ungrouped.first().folderId)
            cancelAndIgnoreRemainingEvents()
        }

        routineDao.deleteRoutineById("routine-1")
        assertNull(routineDao.getRoutineWithExercises("routine-1"))
    }
}
