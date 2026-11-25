package com.davidguijo.ferro.feature.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.ProgressCalculations
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.ExerciseChartMetric
import com.davidguijo.ferro.core.domain.model.ExerciseChartPoint
import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.ExerciseRecordDetail
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import com.davidguijo.ferro.core.domain.model.RepMaxEntry
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isSpanish: Boolean = true,
    val isLoaded: Boolean = false,
    val isEditSheetVisible: Boolean = false,
    val isArchiveDialogVisible: Boolean = false,
    val selectedMetric: ExerciseChartMetric = ExerciseChartMetric.ESTIMATED_1RM,
    val selectedRange: ProgressTimeRange = ProgressTimeRange.THREE_MONTHS,
    val chartPoints: List<ExerciseChartPoint> = emptyList(),
    val historyEntries: List<ExerciseHistoryEntry> = emptyList(),
    val recordSetIds: Set<String> = emptySet(),
    val bestRecords: List<ExerciseRecordDetail> = emptyList(),
    val repMaxTable: Map<Int, RepMaxEntry> = emptyMap()
)

private data class ExerciseDetailBaseState(
    val exercise: Exercise?,
    val weightUnit: WeightUnit,
    val isSpanish: Boolean,
    val isEditSheetVisible: Boolean,
    val isArchiveDialogVisible: Boolean
)

private data class ExerciseDetailStatsState(
    val selectedMetric: ExerciseChartMetric,
    val selectedRange: ProgressTimeRange,
    val chartPoints: List<ExerciseChartPoint>,
    val historyEntries: List<ExerciseHistoryEntry>,
    val recordDetails: List<ExerciseRecordDetail>,
    val repMaxTable: Map<Int, RepMaxEntry>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseDetailViewModel
internal constructor(
    private val exerciseId: String,
    private val exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    personalRecordRepository: PersonalRecordRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        personalRecordRepository: PersonalRecordRepository,
        settingsRepository: SettingsRepository,
        clock: Clock
    ) : this(
        exerciseId = savedStateHandle.toRoute<ExerciseDetailRoute>().exerciseId,
        exerciseRepository = exerciseRepository,
        workoutRepository = workoutRepository,
        personalRecordRepository = personalRecordRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        zoneId = ZoneId.systemDefault(),
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val editSheetVisible = MutableStateFlow(false)
    private val archiveDialogVisible = MutableStateFlow(false)
    private val selectedMetric = MutableStateFlow(ExerciseChartMetric.ESTIMATED_1RM)
    private val selectedRange = MutableStateFlow(ProgressTimeRange.THREE_MONTHS)

    private val baseFlow: Flow<ExerciseDetailBaseState> =
        combine(
            exerciseRepository.observeExerciseById(exerciseId),
            settingsRepository.observeSettings(),
            editSheetVisible,
            archiveDialogVisible
        ) { exercise, settings, editVisible, archiveVisible ->
            ExerciseDetailBaseState(
                exercise = exercise,
                weightUnit = settings.weightUnit,
                isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider()),
                isEditSheetVisible = editVisible,
                isArchiveDialogVisible = archiveVisible
            )
        }

    private val chartRowsFlow =
        selectedRange.flatMapLatest { range ->
            val sinceMillis = ProgressCalculations.timeRangeStartMillis(range, clock.millis(), zoneId)
            workoutRepository.observeExerciseChartMetrics(exerciseId = exerciseId, sinceMillis = sinceMillis)
        }

    private val chartSelectionFlow =
        combine(selectedMetric, selectedRange, chartRowsFlow) { metric, range, rows ->
            Triple(metric, range, ProgressCalculations.buildExerciseChartSeries(rows, metric))
        }

    private val statsFlow: Flow<ExerciseDetailStatsState> =
        combine(
            chartSelectionFlow,
            workoutRepository.observeExerciseHistory(exerciseId),
            personalRecordRepository.observeRecordDetailsForExercise(exerciseId),
            workoutRepository.observeExerciseRepMaxes(exerciseId)
        ) { chartTriple, history, recordDetails, repMaxEntries ->
            ExerciseDetailStatsState(
                selectedMetric = chartTriple.first,
                selectedRange = chartTriple.second,
                chartPoints = chartTriple.third,
                historyEntries = history,
                recordDetails = recordDetails,
                repMaxTable = ProgressCalculations.buildRepMaxTable(repMaxEntries)
            )
        }

    val uiState: StateFlow<ExerciseDetailUiState> =
        combine(baseFlow, statsFlow) { base, stats ->
            val trackingType = base.exercise?.trackingType ?: TrackingType.WEIGHT_REPS
            ExerciseDetailUiState(
                exercise = base.exercise,
                weightUnit = base.weightUnit,
                isSpanish = base.isSpanish,
                isLoaded = true,
                isEditSheetVisible = base.isEditSheetVisible,
                isArchiveDialogVisible = base.isArchiveDialogVisible,
                selectedMetric = stats.selectedMetric,
                selectedRange = stats.selectedRange,
                chartPoints = stats.chartPoints,
                historyEntries = stats.historyEntries,
                recordSetIds = stats.recordDetails.mapTo(mutableSetOf()) { it.record.workoutSetId },
                bestRecords = ProgressCalculations.selectBestRecordsByType(stats.recordDetails, trackingType),
                repMaxTable = stats.repMaxTable
            )
        }.catch {
            emit(ExerciseDetailUiState(isLoaded = true))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExerciseDetailUiState()
        )

    fun onSelectMetric(metric: ExerciseChartMetric) {
        selectedMetric.value = metric
    }

    fun onSelectRange(range: ProgressTimeRange) {
        selectedRange.value = range
    }

    fun onOpenEditSheet() {
        if (uiState.value.exercise?.isCustom == true) {
            editSheetVisible.value = true
        }
    }

    fun onDismissEditSheet() {
        editSheetVisible.value = false
    }

    fun onUpdateCustomExercise(draft: CustomExerciseDraft) {
        val current = uiState.value.exercise ?: return
        if (!current.isCustom) return
        val isSpanish = uiState.value.isSpanish
        val sameNames = current.nameEs == current.nameEn
        val sameInstructions = current.instructionsEs == current.instructionsEn

        viewModelScope.launch {
            val updated =
                current.copy(
                    nameEs = if (isSpanish || sameNames) draft.name else current.nameEs,
                    nameEn = if (!isSpanish || sameNames) draft.name else current.nameEn,
                    primaryMuscle = draft.primaryMuscle,
                    secondaryMuscles = draft.secondaryMuscles,
                    equipment = draft.equipment,
                    trackingType = draft.trackingType,
                    instructionsEs =
                        if (isSpanish || sameInstructions) {
                            draft.instructions
                        } else {
                            current.instructionsEs
                        },
                    instructionsEn =
                        if (!isSpanish || sameInstructions) {
                            draft.instructions
                        } else {
                            current.instructionsEn
                        },
                    updatedAt = clock.millis()
                )
            runCatching {
                exerciseRepository.upsertExercise(updated)
            }.onSuccess {
                editSheetVisible.value = false
            }
        }
    }

    fun onRequestToggleArchive() {
        archiveDialogVisible.value = true
    }

    fun onDismissArchiveDialog() {
        archiveDialogVisible.value = false
    }

    fun onConfirmToggleArchive() {
        val current = uiState.value.exercise ?: return
        viewModelScope.launch {
            runCatching {
                exerciseRepository.setArchived(id = current.id, archived = !current.archived)
            }.onSuccess {
                archiveDialogVisible.value = false
            }
        }
    }
}
