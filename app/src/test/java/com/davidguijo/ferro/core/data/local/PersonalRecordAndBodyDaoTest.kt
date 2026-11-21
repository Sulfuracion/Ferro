package com.davidguijo.ferro.core.data.local

import android.app.Application
import app.cash.turbine.test
import com.davidguijo.ferro.core.data.local.dao.BodyMeasurementDao
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.PersonalRecordDao
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import com.davidguijo.ferro.core.data.local.entity.BodyMeasurementEntity
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.data.local.entity.ProgressPhotoEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
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
class PersonalRecordAndBodyDaoTest {
    private lateinit var database: FerroDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var bodyMeasurementDao: BodyMeasurementDao

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        exerciseDao = database.exerciseDao()
        workoutDao = database.workoutDao()
        personalRecordDao = database.personalRecordDao()
        bodyMeasurementDao = database.bodyMeasurementDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun personalRecordsQueryBestByTypeAndCascadeOnSetDelete() = runTest {
        exerciseDao.upsert(ExerciseDaoTest.sampleExercise(id = "ex-1"))
        workoutDao.saveWorkoutWithExercises(
            workout =
                WorkoutEntity(
                    id = "w-1",
                    name = "Fuerza",
                    routineId = null,
                    startedAt = 1_000L,
                    endedAt = 2_000L,
                    notes = "",
                    status = WorkoutStatus.COMPLETED,
                    createdAt = 1_000L,
                    updatedAt = 2_000L
                ),
            exercises =
                listOf(
                    WorkoutExerciseEntity(
                        id = "we-1",
                        workoutId = "w-1",
                        exerciseId = "ex-1",
                        position = 0,
                        supersetGroup = null,
                        restSeconds = 120,
                        notes = "",
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    )
                ),
            sets =
                listOf(
                    WorkoutSetEntity(
                        id = "ws-1",
                        workoutExerciseId = "we-1",
                        position = 0,
                        setType = SetType.NORMAL,
                        weightKg = 100.0,
                        reps = 5,
                        rpe = null,
                        durationS = null,
                        distanceM = null,
                        completed = true,
                        completedAt = 1_500L,
                        createdAt = 1_000L,
                        updatedAt = 1_500L
                    ),
                    WorkoutSetEntity(
                        id = "ws-2",
                        workoutExerciseId = "we-1",
                        position = 1,
                        setType = SetType.NORMAL,
                        weightKg = 110.0,
                        reps = 3,
                        rpe = null,
                        durationS = null,
                        distanceM = null,
                        completed = true,
                        completedAt = 1_800L,
                        createdAt = 1_000L,
                        updatedAt = 1_800L
                    )
                )
        )

        personalRecordDao.upsertAll(
            listOf(
                PersonalRecordEntity(
                    id = "pr-1",
                    exerciseId = "ex-1",
                    recordType = RecordType.MAX_WEIGHT,
                    value = 100.0,
                    workoutSetId = "ws-1",
                    achievedAt = 1_500L,
                    createdAt = 1_500L,
                    updatedAt = 1_500L
                ),
                PersonalRecordEntity(
                    id = "pr-2",
                    exerciseId = "ex-1",
                    recordType = RecordType.MAX_WEIGHT,
                    value = 110.0,
                    workoutSetId = "ws-2",
                    achievedAt = 1_800L,
                    createdAt = 1_800L,
                    updatedAt = 1_800L
                )
            )
        )

        assertEquals(
            "pr-2",
            personalRecordDao.getLatestRecord("ex-1", RecordType.MAX_WEIGHT)?.id
        )

        workoutDao.deleteWorkoutSetById("ws-2")
        assertEquals(
            "pr-1",
            personalRecordDao.getLatestRecord("ex-1", RecordType.MAX_WEIGHT)?.id
        )
    }

    @Test
    fun bodyMeasurementsAndPhotosSetNullOnMeasurementDelete() = runTest {
        bodyMeasurementDao.upsertMeasurement(
            BodyMeasurementEntity(
                id = "m-1",
                measuredAt = 10_000L,
                weightKg = 78.4,
                bodyFatPct = 14.2,
                neckCm = 39.0,
                chestCm = 102.0,
                waistCm = 81.5,
                hipsCm = 96.0,
                armLCm = 36.5,
                armRCm = 36.8,
                thighLCm = 58.0,
                thighRCm = 58.2,
                calfCm = 38.0,
                notes = "En ayunas",
                createdAt = 10_000L,
                updatedAt = 10_000L
            )
        )
        bodyMeasurementDao.upsertPhoto(
            ProgressPhotoEntity(
                id = "photo-1",
                takenAt = 10_100L,
                fileName = "front_20261121.jpg",
                pose = "FRONT",
                measurementId = "m-1",
                createdAt = 10_100L,
                updatedAt = 10_100L
            )
        )

        bodyMeasurementDao.observeLatestMeasurement().test {
            assertEquals("m-1", awaitItem()?.id)
            cancelAndIgnoreRemainingEvents()
        }

        bodyMeasurementDao.observePhotosForMeasurement("m-1").test {
            assertEquals(listOf("photo-1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        bodyMeasurementDao.deleteMeasurementById("m-1")
        assertNull(bodyMeasurementDao.getMeasurementById("m-1"))

        bodyMeasurementDao.observeProgressPhotos().test {
            val photos = awaitItem()
            assertEquals(listOf("photo-1"), photos.map { it.id })
            assertNull(photos.first().measurementId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
