package com.davidguijo.ferro.core.data.local

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.entity.ExerciseEntity
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExerciseDaoTest {
    private lateinit var database: FerroDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObserveActiveExercisesFiltersArchived() = runTest {
        val bench = sampleExercise(id = "ex-1", nameEs = "Press banca", nameEn = "Bench press")
        val archived =
            sampleExercise(
                id = "ex-2",
                nameEs = "Aperturas",
                nameEn = "Chest fly",
                archived = true
            )
        dao.upsertAll(listOf(bench, archived))

        dao.observeActiveExercises().test {
            val active = awaitItem()
            assertEquals(listOf("ex-1"), active.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        dao.setArchived(id = "ex-1", archived = true, updatedAt = 2_000L)
        assertEquals(true, dao.getById("ex-1")?.archived)
        assertEquals(2_000L, dao.getById("ex-1")?.updatedAt)

        dao.deleteById("ex-2")
        assertNull(dao.getById("ex-2"))
    }

    @Test
    fun searchExercisesFiltersByQueryEsEnMuscleAndEquipment() = runTest {
        dao.upsertAll(
            listOf(
                sampleExercise(
                    id = "bench",
                    nameEs = "Press banca",
                    nameEn = "Barbell bench press",
                    primaryMuscle = Muscle.CHEST,
                    secondaryMuscles = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
                    equipment = Equipment.BARBELL
                ),
                sampleExercise(
                    id = "squat",
                    nameEs = "Sentadilla",
                    nameEn = "Back squat",
                    primaryMuscle = Muscle.QUADS,
                    secondaryMuscles = listOf(Muscle.GLUTES),
                    equipment = Equipment.BARBELL
                ),
                sampleExercise(
                    id = "curl",
                    nameEs = "Curl bíceps",
                    nameEn = "Dumbbell curl",
                    primaryMuscle = Muscle.BICEPS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.DUMBBELL
                )
            )
        )

        dao.searchExercises(query = "squat", muscle = null, equipment = null, includeArchived = false).test {
            assertEquals(listOf("squat"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        dao.searchExercises(query = "", muscle = Muscle.CHEST, equipment = Equipment.BARBELL, includeArchived = false)
            .test {
                val results = awaitItem()
                assertEquals(listOf("bench"), results.map { it.id })
                assertEquals(listOf(Muscle.TRICEPS, Muscle.SHOULDERS), results.first().secondaryMuscles)
                cancelAndIgnoreRemainingEvents()
            }

        dao.searchExercises(query = "", muscle = null, equipment = Equipment.DUMBBELL, includeArchived = false).test {
            assertEquals(listOf("curl"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        internal fun sampleExercise(
            id: String,
            nameEs: String = "Press banca",
            nameEn: String = "Bench press",
            primaryMuscle: Muscle = Muscle.CHEST,
            secondaryMuscles: List<Muscle> = listOf(Muscle.TRICEPS),
            equipment: Equipment = Equipment.BARBELL,
            trackingType: TrackingType = TrackingType.WEIGHT_REPS,
            archived: Boolean = false
        ): ExerciseEntity = ExerciseEntity(
            id = id,
            nameEs = nameEs,
            nameEn = nameEn,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
            trackingType = trackingType,
            instructionsEs = "Baja la barra controlada al pecho.",
            instructionsEn = "Lower the bar under control to your chest.",
            isCustom = false,
            archived = archived,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )
    }
}
