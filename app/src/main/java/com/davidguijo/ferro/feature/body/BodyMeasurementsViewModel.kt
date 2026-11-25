package com.davidguijo.ferro.feature.body

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.data.storage.ProgressPhotoStorage
import com.davidguijo.ferro.core.domain.BodyChartPoint
import com.davidguijo.ferro.core.domain.BodyMeasurementCalculations
import com.davidguijo.ferro.core.domain.BodyMetric
import com.davidguijo.ferro.core.domain.BodyMetricSummary
import com.davidguijo.ferro.core.domain.LengthUnit
import com.davidguijo.ferro.core.domain.PhotoDateGroup
import com.davidguijo.ferro.core.domain.PhotoPose
import com.davidguijo.ferro.core.domain.ProgressCalculations
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CompareSlot { LEFT, RIGHT }

data class BodyMeasurementFormDraft(
    val editingId: String? = null,
    val createdAt: Long? = null,
    val measuredAt: Long,
    val fieldTexts: Map<BodyMetric, String> = emptyMap(),
    val fieldErrors: Set<BodyMetric> = emptySet(),
    val notes: String = "",
    val isSaving: Boolean = false
)

data class PendingPhotoDraft(val fileName: String, val takenAt: Long, val pose: PhotoPose = PhotoPose.FRONT)

data class BodyMeasurementsUiState(
    val isLoaded: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val lengthUnit: LengthUnit = LengthUnit.CM,
    val metricSummaries: List<BodyMetricSummary> = emptyList(),
    val measurements: List<BodyMeasurement> = emptyList(),
    val selectedMetric: BodyMetric = BodyMetric.WEIGHT,
    val selectedRange: ProgressTimeRange = ProgressTimeRange.THREE_MONTHS,
    val chartPoints: List<BodyChartPoint> = emptyList(),
    val photoGroups: List<PhotoDateGroup> = emptyList(),
    val allPhotos: List<ProgressPhoto> = emptyList(),
    val formDraft: BodyMeasurementFormDraft? = null,
    val deleteMeasurementConfirmId: String? = null,
    val pendingPhotoDraft: PendingPhotoDraft? = null,
    val deletePhotoConfirm: ProgressPhoto? = null,
    val isCompareMode: Boolean = false,
    val compareLeftPhotoId: String? = null,
    val compareRightPhotoId: String? = null,
    val activeCompareSlot: CompareSlot = CompareSlot.LEFT,
    val photoError: Boolean = false
)

private data class BodyOverlayState(
    val selectedMetric: BodyMetric = BodyMetric.WEIGHT,
    val selectedRange: ProgressTimeRange = ProgressTimeRange.THREE_MONTHS,
    val formDraft: BodyMeasurementFormDraft? = null,
    val deleteMeasurementConfirmId: String? = null,
    val pendingPhotoDraft: PendingPhotoDraft? = null,
    val deletePhotoConfirm: ProgressPhoto? = null,
    val isCompareMode: Boolean = false,
    val compareLeftPhotoId: String? = null,
    val compareRightPhotoId: String? = null,
    val activeCompareSlot: CompareSlot = CompareSlot.LEFT,
    val photoError: Boolean = false
)

@HiltViewModel
class BodyMeasurementsViewModel
internal constructor(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    settingsRepository: SettingsRepository,
    private val photoStorage: ProgressPhotoStorage?,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val localeProvider: () -> Locale
) : ViewModel() {
    @Inject
    constructor(
        bodyMeasurementRepository: BodyMeasurementRepository,
        settingsRepository: SettingsRepository,
        photoStorage: ProgressPhotoStorage,
        clock: Clock
    ) : this(
        bodyMeasurementRepository = bodyMeasurementRepository,
        settingsRepository = settingsRepository,
        photoStorage = photoStorage,
        clock = clock,
        zoneId = ZoneId.systemDefault(),
        localeProvider = { Locale.getDefault() }
    )

    private val overlay = MutableStateFlow(BodyOverlayState())
    private var latestWeightUnit: WeightUnit = WeightUnit.KG
    private var latestPhotos: List<ProgressPhoto> = emptyList()

    val uiState: StateFlow<BodyMeasurementsUiState> =
        combine(
            bodyMeasurementRepository.observeMeasurements(),
            bodyMeasurementRepository.observeProgressPhotos(),
            settingsRepository.observeSettings(),
            overlay
        ) { measurements, photos, settings, ov ->
            latestWeightUnit = settings.weightUnit
            latestPhotos = photos
            val lengthUnit = Units.lengthUnitFor(settings.weightUnit)
            val summaries = BodyMeasurementCalculations.computeMetricSummaries(measurements)
            val sinceMillis =
                ProgressCalculations.timeRangeStartMillis(
                    range = ov.selectedRange,
                    nowMillis = clock.millis(),
                    zoneId = zoneId
                )
            val chartPoints =
                BodyMeasurementCalculations.buildChartSeries(
                    measurements = measurements,
                    metric = ov.selectedMetric,
                    sinceMillis = sinceMillis
                )
            val photoGroups = BodyMeasurementCalculations.groupPhotosByDate(photos, zoneId)
            val validPhotoIds = photos.mapTo(mutableSetOf()) { it.id }

            BodyMeasurementsUiState(
                isLoaded = true,
                weightUnit = settings.weightUnit,
                lengthUnit = lengthUnit,
                metricSummaries = summaries,
                measurements = measurements,
                selectedMetric = ov.selectedMetric,
                selectedRange = ov.selectedRange,
                chartPoints = chartPoints,
                photoGroups = photoGroups,
                allPhotos = photos,
                formDraft = ov.formDraft,
                deleteMeasurementConfirmId = ov.deleteMeasurementConfirmId,
                pendingPhotoDraft = ov.pendingPhotoDraft,
                deletePhotoConfirm = ov.deletePhotoConfirm,
                isCompareMode = ov.isCompareMode && photos.size >= 2,
                compareLeftPhotoId = ov.compareLeftPhotoId?.takeIf { it in validPhotoIds },
                compareRightPhotoId = ov.compareRightPhotoId?.takeIf { it in validPhotoIds },
                activeCompareSlot = ov.activeCompareSlot,
                photoError = ov.photoError
            )
        }.catch {
            emit(BodyMeasurementsUiState(isLoaded = true))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BodyMeasurementsUiState()
        )

    fun onOpenAddForm() {
        overlay.update {
            it.copy(
                formDraft =
                    BodyMeasurementFormDraft(
                        measuredAt = clock.millis()
                    )
            )
        }
    }

    fun onOpenEditForm(measurement: BodyMeasurement) {
        val weightUnit = latestWeightUnit
        val lengthUnit = Units.lengthUnitFor(weightUnit)
        val locale = localeProvider()
        val texts = buildMap {
            measurement.weightKg?.let {
                put(BodyMetric.WEIGHT, Units.formatWeight(it, weightUnit, locale = locale, roundingStep = null))
            }
            measurement.bodyFatPct?.let {
                put(BodyMetric.BODY_FAT, Units.formatBodyFat(it, locale = locale))
            }
            BodyMetric.entries.forEach { metric ->
                if (metric != BodyMetric.WEIGHT && metric != BodyMetric.BODY_FAT) {
                    BodyMeasurementCalculations.valueFor(measurement, metric)?.let { cm ->
                        put(metric, Units.formatLength(cm, lengthUnit, locale = locale))
                    }
                }
            }
        }
        overlay.update {
            it.copy(
                formDraft =
                    BodyMeasurementFormDraft(
                        editingId = measurement.id,
                        createdAt = measurement.createdAt,
                        measuredAt = measurement.measuredAt,
                        fieldTexts = texts,
                        notes = measurement.notes
                    )
            )
        }
    }

    fun onDismissForm() {
        overlay.update { it.copy(formDraft = null) }
    }

    fun onUpdateFormDate(measuredAtMillis: Long) {
        overlay.update { state ->
            val draft = state.formDraft ?: return@update state
            state.copy(formDraft = draft.copy(measuredAt = measuredAtMillis))
        }
    }

    fun onUpdateFormField(metric: BodyMetric, value: String) {
        overlay.update { state ->
            val draft = state.formDraft ?: return@update state
            val updatedTexts = draft.fieldTexts.toMutableMap().apply { put(metric, value) }
            val updatedErrors = draft.fieldErrors - metric
            state.copy(formDraft = draft.copy(fieldTexts = updatedTexts, fieldErrors = updatedErrors))
        }
    }

    fun onUpdateFormNotes(notes: String) {
        overlay.update { state ->
            val draft = state.formDraft ?: return@update state
            state.copy(formDraft = draft.copy(notes = notes))
        }
    }

    fun onSaveForm() {
        val draft = overlay.value.formDraft ?: return
        if (draft.isSaving) return

        val weightUnit = latestWeightUnit
        val lengthUnit = Units.lengthUnitFor(weightUnit)
        val errors = mutableSetOf<BodyMetric>()

        fun parseWeightField(): Double? {
            val raw = draft.fieldTexts[BodyMetric.WEIGHT].orEmpty().trim()
            if (raw.isEmpty()) return null
            val parsed = Units.parseWeightToKg(raw, weightUnit)
            if (parsed == null || parsed <= 0.0) {
                errors += BodyMetric.WEIGHT
            }
            return parsed
        }

        fun parseBodyFatField(): Double? {
            val raw = draft.fieldTexts[BodyMetric.BODY_FAT].orEmpty().trim()
            if (raw.isEmpty()) return null
            val parsed = Units.parseWeightInput(raw)
            if (parsed == null || parsed < 0.0 || parsed > 100.0) {
                errors += BodyMetric.BODY_FAT
            }
            return parsed
        }

        fun parseLengthField(metric: BodyMetric): Double? {
            val raw = draft.fieldTexts[metric].orEmpty().trim()
            if (raw.isEmpty()) return null
            val parsed = Units.parseLengthToCm(raw, lengthUnit)
            if (parsed == null || parsed <= 0.0) {
                errors += metric
            }
            return parsed
        }

        val weightKg = parseWeightField()
        val bodyFatPct = parseBodyFatField()
        val neckCm = parseLengthField(BodyMetric.NECK)
        val chestCm = parseLengthField(BodyMetric.CHEST)
        val waistCm = parseLengthField(BodyMetric.WAIST)
        val hipsCm = parseLengthField(BodyMetric.HIPS)
        val armLCm = parseLengthField(BodyMetric.ARM_L)
        val armRCm = parseLengthField(BodyMetric.ARM_R)
        val thighLCm = parseLengthField(BodyMetric.THIGH_L)
        val thighRCm = parseLengthField(BodyMetric.THIGH_R)
        val calfCm = parseLengthField(BodyMetric.CALF)

        if (errors.isNotEmpty()) {
            overlay.update { state ->
                state.copy(formDraft = state.formDraft?.copy(fieldErrors = errors))
            }
            return
        }

        overlay.update { state ->
            state.copy(formDraft = state.formDraft?.copy(isSaving = true))
        }

        viewModelScope.launch {
            val now = clock.millis()
            val measurement =
                BodyMeasurement(
                    id = draft.editingId ?: UUID.randomUUID().toString(),
                    measuredAt = draft.measuredAt,
                    weightKg = weightKg,
                    bodyFatPct = bodyFatPct,
                    neckCm = neckCm,
                    chestCm = chestCm,
                    waistCm = waistCm,
                    hipsCm = hipsCm,
                    armLCm = armLCm,
                    armRCm = armRCm,
                    thighLCm = thighLCm,
                    thighRCm = thighRCm,
                    calfCm = calfCm,
                    notes = draft.notes.trim(),
                    createdAt = draft.createdAt ?: now,
                    updatedAt = now
                )
            bodyMeasurementRepository.upsertMeasurement(measurement)
            overlay.update { it.copy(formDraft = null) }
        }
    }

    fun onRequestDeleteMeasurement(id: String) {
        overlay.update { it.copy(deleteMeasurementConfirmId = id) }
    }

    fun onDismissDeleteMeasurement() {
        overlay.update { it.copy(deleteMeasurementConfirmId = null) }
    }

    fun onConfirmDeleteMeasurement() {
        val id = overlay.value.deleteMeasurementConfirmId ?: return
        overlay.update { it.copy(deleteMeasurementConfirmId = null) }
        viewModelScope.launch {
            bodyMeasurementRepository.deleteMeasurement(id)
        }
    }

    fun onSelectMetric(metric: BodyMetric) {
        overlay.update { it.copy(selectedMetric = metric) }
    }

    fun onSelectRange(range: ProgressTimeRange) {
        overlay.update { it.copy(selectedRange = range) }
    }

    fun createTempCameraUri(): Pair<Uri, File>? = photoStorage?.createTempCameraUri()

    fun onPhotoPickedFromGallery(uri: Uri) {
        val storage = photoStorage ?: return
        viewModelScope.launch {
            val fileName = storage.importFromUri(uri)
            if (fileName == null) {
                overlay.update { it.copy(photoError = true) }
            } else {
                overlay.update {
                    it.copy(
                        pendingPhotoDraft =
                            PendingPhotoDraft(
                                fileName = fileName,
                                takenAt = clock.millis(),
                                pose = PhotoPose.FRONT
                            ),
                        photoError = false
                    )
                }
            }
        }
    }

    fun onCameraPhotoCaptured(tempFile: File, success: Boolean) {
        val storage = photoStorage ?: return
        viewModelScope.launch {
            if (!success) {
                if (tempFile.exists()) tempFile.delete()
                return@launch
            }
            val fileName = storage.commitCameraFile(tempFile)
            if (fileName == null) {
                overlay.update { it.copy(photoError = true) }
            } else {
                overlay.update {
                    it.copy(
                        pendingPhotoDraft =
                            PendingPhotoDraft(
                                fileName = fileName,
                                takenAt = clock.millis(),
                                pose = PhotoPose.FRONT
                            ),
                        photoError = false
                    )
                }
            }
        }
    }

    fun onSelectPendingPhotoPose(pose: PhotoPose) {
        overlay.update { state ->
            val pending = state.pendingPhotoDraft ?: return@update state
            state.copy(pendingPhotoDraft = pending.copy(pose = pose))
        }
    }

    fun onSavePendingPhoto() {
        val pending = overlay.value.pendingPhotoDraft ?: return
        overlay.update { it.copy(pendingPhotoDraft = null) }
        viewModelScope.launch {
            val now = clock.millis()
            val photo =
                ProgressPhoto(
                    id = UUID.randomUUID().toString(),
                    takenAt = pending.takenAt,
                    fileName = pending.fileName,
                    pose = pending.pose.key,
                    measurementId = null,
                    createdAt = now,
                    updatedAt = now
                )
            bodyMeasurementRepository.upsertPhoto(photo)
        }
    }

    fun onCancelPendingPhoto() {
        val pending = overlay.value.pendingPhotoDraft ?: return
        overlay.update { it.copy(pendingPhotoDraft = null) }
        viewModelScope.launch {
            photoStorage?.deletePhotoFile(pending.fileName)
        }
    }

    fun onDismissPhotoError() {
        overlay.update { it.copy(photoError = false) }
    }

    fun onRequestDeletePhoto(photo: ProgressPhoto) {
        overlay.update { it.copy(deletePhotoConfirm = photo) }
    }

    fun onDismissDeletePhoto() {
        overlay.update { it.copy(deletePhotoConfirm = null) }
    }

    fun onConfirmDeletePhoto() {
        val photo = overlay.value.deletePhotoConfirm ?: return
        overlay.update { it.copy(deletePhotoConfirm = null) }
        viewModelScope.launch {
            bodyMeasurementRepository.deletePhoto(photo.id)
            photoStorage?.deletePhotoFile(photo.fileName)
        }
    }

    fun onToggleCompareMode() {
        val photos = latestPhotos
        overlay.update { state ->
            if (state.isCompareMode || photos.size < 2) {
                state.copy(isCompareMode = false)
            } else {
                val sortedAsc = photos.sortedBy { it.takenAt }
                val left = state.compareLeftPhotoId ?: sortedAsc.first().id
                val right = state.compareRightPhotoId ?: sortedAsc.last().id
                state.copy(
                    isCompareMode = true,
                    compareLeftPhotoId = left,
                    compareRightPhotoId = right,
                    activeCompareSlot = CompareSlot.LEFT
                )
            }
        }
    }

    fun onSelectCompareSlot(slot: CompareSlot) {
        overlay.update { it.copy(activeCompareSlot = slot) }
    }

    fun onPickPhotoForCompare(photoId: String) {
        overlay.update { state ->
            when (state.activeCompareSlot) {
                CompareSlot.LEFT ->
                    state.copy(compareLeftPhotoId = photoId, activeCompareSlot = CompareSlot.RIGHT)

                CompareSlot.RIGHT ->
                    state.copy(compareRightPhotoId = photoId, activeCompareSlot = CompareSlot.LEFT)
            }
        }
    }

    fun resolvePhotoFile(fileName: String): File? = photoStorage?.resolvePhotoFile(fileName)
}
