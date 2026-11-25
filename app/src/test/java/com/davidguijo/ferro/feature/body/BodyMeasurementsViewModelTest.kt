package com.davidguijo.ferro.feature.body

import app.cash.turbine.test
import com.davidguijo.ferro.core.domain.BodyMetric
import com.davidguijo.ferro.core.domain.LengthUnit
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMeasurementsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(1_000_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `guardar registro en lb convierte peso a kg y perimetros de pulgadas a cm`() = runTest(testDispatcher) {
        val bodyRepo = FakeBodyMeasurementRepository()
        val settingsRepo = FakeSettingsRepository(Settings(weightUnit = WeightUnit.LB))
        val vm =
            BodyMeasurementsViewModel(
                bodyMeasurementRepository = bodyRepo,
                settingsRepository = settingsRepo,
                photoStorage = null,
                clock = fixedClock,
                zoneId = ZoneOffset.UTC,
                localeProvider = { Locale.US }
            )

        vm.uiState.test {
            val initial = awaitItem()
            val loaded = if (initial.isLoaded) initial else awaitItem()
            assertEquals(LengthUnit.IN, loaded.lengthUnit)

            vm.onOpenAddForm()
            awaitItem()
            vm.onUpdateFormField(BodyMetric.WEIGHT, "200")
            vm.onUpdateFormField(BodyMetric.WAIST, "32")
            vm.onUpdateFormNotes("En ayunas")
            vm.onSaveForm()
            advanceUntilIdle()

            val afterSave = expectMostRecentItem()
            assertNull(afterSave.formDraft)
            assertEquals(1, afterSave.measurements.size)
            val saved = afterSave.measurements.first()
            assertEquals(200.0 * 0.45359237, saved.weightKg!!, 1e-6)
            assertEquals(32.0 * 2.54, saved.waistCm!!, 1e-6)
            assertEquals("En ayunas", saved.notes)
        }
    }

    @Test
    fun `editar y borrar registro actualiza la lista y los resumenes`() = runTest(testDispatcher) {
        val bodyRepo = FakeBodyMeasurementRepository()
        val settingsRepo = FakeSettingsRepository(Settings(weightUnit = WeightUnit.KG))
        val m1 =
            BodyMeasurement(
                id = "m-1",
                measuredAt = 100_000L,
                weightKg = 80.0,
                bodyFatPct = 15.0,
                neckCm = null,
                chestCm = null,
                waistCm = 82.0,
                hipsCm = null,
                armLCm = null,
                armRCm = null,
                thighLCm = null,
                thighRCm = null,
                calfCm = null,
                notes = "",
                createdAt = 100_000L,
                updatedAt = 100_000L
            )
        bodyRepo.measurementsFlow.value = listOf(m1)

        val vm =
            BodyMeasurementsViewModel(
                bodyMeasurementRepository = bodyRepo,
                settingsRepository = settingsRepo,
                photoStorage = null,
                clock = fixedClock,
                zoneId = ZoneOffset.UTC,
                localeProvider = { Locale.forLanguageTag("es-ES") }
            )

        vm.uiState.test {
            val loaded = awaitItem().let { if (it.isLoaded) it else awaitItem() }
            assertEquals(1, loaded.measurements.size)

            vm.onOpenEditForm(m1)
            val editing = awaitItem()
            assertNotNull(editing.formDraft)
            assertEquals("80", editing.formDraft!!.fieldTexts[BodyMetric.WEIGHT])

            vm.onUpdateFormField(BodyMetric.WEIGHT, "79,5")
            vm.onSaveForm()
            advanceUntilIdle()

            val updated = expectMostRecentItem()
            assertEquals(79.5, updated.measurements.first().weightKg!!, 1e-6)

            vm.onRequestDeleteMeasurement("m-1")
            awaitItem()
            vm.onConfirmDeleteMeasurement()
            advanceUntilIdle()

            val afterDelete = expectMostRecentItem()
            assertTrue(afterDelete.measurements.isEmpty())
        }
    }

    @Test
    fun `modo comparador selecciona dos fotos y permite intercambiar slots`() = runTest(testDispatcher) {
        val bodyRepo = FakeBodyMeasurementRepository()
        val settingsRepo = FakeSettingsRepository(Settings())
        bodyRepo.photosFlow.value =
            listOf(
                ProgressPhoto("p-new", 200_000L, "new.jpg", "front", null, 200_000L, 200_000L),
                ProgressPhoto("p-old", 100_000L, "old.jpg", "front", null, 100_000L, 100_000L)
            )
        val vm =
            BodyMeasurementsViewModel(
                bodyMeasurementRepository = bodyRepo,
                settingsRepository = settingsRepo,
                photoStorage = null,
                clock = fixedClock,
                zoneId = ZoneOffset.UTC,
                localeProvider = { Locale.US }
            )

        vm.uiState.test {
            awaitItem().let { if (it.isLoaded) it else awaitItem() }
            vm.onToggleCompareMode()
            val compare = awaitItem()
            assertTrue(compare.isCompareMode)
            assertEquals("p-old", compare.compareLeftPhotoId)
            assertEquals("p-new", compare.compareRightPhotoId)

            vm.onPickPhotoForCompare("p-new")
            val swapped = awaitItem()
            assertEquals("p-new", swapped.compareLeftPhotoId)
            assertEquals(CompareSlot.RIGHT, swapped.activeCompareSlot)
        }
    }

    private class FakeBodyMeasurementRepository : BodyMeasurementRepository {
        val measurementsFlow = MutableStateFlow<List<BodyMeasurement>>(emptyList())
        val photosFlow = MutableStateFlow<List<ProgressPhoto>>(emptyList())

        override fun observeMeasurements(): Flow<List<BodyMeasurement>> =
            measurementsFlow.map { list -> list.sortedByDescending { it.measuredAt } }

        override fun observeLatestMeasurement(): Flow<BodyMeasurement?> = observeMeasurements().map { it.firstOrNull() }

        override suspend fun getMeasurementById(id: String): BodyMeasurement? =
            measurementsFlow.value.firstOrNull { it.id == id }

        override suspend fun upsertMeasurement(measurement: BodyMeasurement) {
            val current = measurementsFlow.value.filterNot { it.id == measurement.id }
            measurementsFlow.value = current + measurement
        }

        override suspend fun deleteMeasurement(id: String) {
            measurementsFlow.value = measurementsFlow.value.filterNot { it.id == id }
        }

        override fun observeProgressPhotos(): Flow<List<ProgressPhoto>> =
            photosFlow.map { list -> list.sortedByDescending { it.takenAt } }

        override fun observePhotosForMeasurement(measurementId: String): Flow<List<ProgressPhoto>> =
            observeProgressPhotos().map { list -> list.filter { it.measurementId == measurementId } }

        override suspend fun upsertPhoto(photo: ProgressPhoto) {
            val current = photosFlow.value.filterNot { it.id == photo.id }
            photosFlow.value = current + photo
        }

        override suspend fun deletePhoto(id: String) {
            photosFlow.value = photosFlow.value.filterNot { it.id == id }
        }
    }

    private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<Settings> = state

        override suspend fun setLanguage(language: com.davidguijo.ferro.core.domain.model.AppLanguage) {}

        override suspend fun setWeightUnit(unit: WeightUnit) {}

        override suspend fun setThemeMode(mode: com.davidguijo.ferro.core.domain.model.ThemeMode) {}

        override suspend fun setDefaultRestSeconds(seconds: Int) {}

        override suspend fun setFirstDayOfWeek(dayOfWeek: java.time.DayOfWeek) {}

        override suspend fun setTimerSoundEnabled(enabled: Boolean) {}

        override suspend fun setTimerVibrationEnabled(enabled: Boolean) {}
    }
}
