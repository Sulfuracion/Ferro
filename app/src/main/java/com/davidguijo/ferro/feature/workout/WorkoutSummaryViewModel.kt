package com.davidguijo.ferro.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidguijo.ferro.core.domain.Records
import com.davidguijo.ferro.core.domain.RoutineLastSessionComparison
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.applyWorkoutValuesToRoutine
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeNormalSetNumbers
import com.davidguijo.ferro.core.domain.computeRoutineComparison
import com.davidguijo.ferro.core.domain.computeWorkoutVolumeKg
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.hasWorkoutRoutineValuesChanged
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutSummarySetUi(
    val id: String,
    val setType: SetType,
    val normalSetNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationS: Int?,
    val distanceM: Double?,
    val rpe: Double?,
    val recordTypes: List<RecordType> = emptyList()
) {
    val isRecord: Boolean
        get() = recordTypes.isNotEmpty()
}

data class WorkoutSummaryExerciseUi(
    val id: String,
    val exerciseId: String,
    val name: String,
    val trackingType: TrackingType,
    val supersetGroup: String?,
    val completedSetsCount: Int,
    val sets: List<WorkoutSummarySetUi> = emptyList()
)

data class WorkoutSummaryRecordUi(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val trackingType: TrackingType,
    val recordType: RecordType,
    val value: Double,
    val setId: String,
    val setWeightKg: Double?,
    val setReps: Int?,
    val setDurationS: Int?,
    val setDistanceM: Double?
)

data class WorkoutSummaryComparisonUi(
    val durationDeltaMillis: Long,
    val volumeDeltaKg: Double,
    val completedSetsDelta: Int
)

data class WorkoutSummaryUiState(
    val workoutId: String = "",
    val isLoaded: Boolean = false,
    val workoutName: String = "",
    val routineId: String? = null,
    val routineName: String = "",
    val durationText: String = "00:00",
    val volumeText: String = "0 kg",
    val completedSetsCount: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isSpanish: Boolean = true,
    val exercises: List<WorkoutSummaryExerciseUi> = emptyList(),
    val brokenRecords: List<WorkoutSummaryRecordUi> = emptyList(),
    val routineComparison: WorkoutSummaryComparisonUi? = null,
    val canUpdateRoutineValues: Boolean = false,
    val routineUpdated: Boolean = false
)

@HiltViewModel
class WorkoutSummaryViewModel
internal constructor(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val systemLanguageProvider: () -> String,
    bodyMeasurementRepository: BodyMeasurementRepository? = null
) : ViewModel() {
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        workoutRepository: WorkoutRepository,
        routineRepository: RoutineRepository,
        personalRecordRepository: PersonalRecordRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        bodyMeasurementRepository: BodyMeasurementRepository,
        clock: Clock
    ) : this(
        workoutId = savedStateHandle.toRoute<WorkoutSummaryRoute>().workoutId,
        workoutRepository = workoutRepository,
        routineRepository = routineRepository,
        personalRecordRepository = personalRecordRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        systemLanguageProvider = { Locale.getDefault().language },
        bodyMeasurementRepository = bodyMeasurementRepository
    )

    private val routineUpdatedFlag = MutableStateFlow(false)

    private val workoutFlow = workoutRepository.observeWorkoutWithExercises(workoutId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val routineAndPreviousFlow =
        workoutFlow.flatMapLatest { workout ->
            val routineId = workout?.workout?.routineId
            if (workout == null || routineId == null) {
                flowOf(RoutineContext(routine = null, previousRoutineWorkout = null))
            } else {
                combine(
                    routineRepository.observeRoutineWithExercises(routineId),
                    flow {
                        emit(
                            workoutRepository.getLastCompletedWorkoutForRoutine(
                                routineId = routineId,
                                excludeWorkoutId = workout.workout.id,
                                beforeStartedAt = workout.workout.startedAt
                            )
                        )
                    }
                ) { routine, previousWorkout ->
                    RoutineContext(routine = routine, previousRoutineWorkout = previousWorkout)
                }
            }
        }

    private val latestBodyWeightFlow =
        bodyMeasurementRepository?.observeLatestMeasurement()?.map { it?.weightKg } ?: flowOf(null)

    private val baseDataFlow =
        combine(
            workoutFlow,
            personalRecordRepository.observeRecordsForWorkout(workoutId),
            exerciseRepository.observeAllExercises(),
            settingsRepository.observeSettings(),
            latestBodyWeightFlow
        ) { workout, records, exercises, settings, latestBodyWeightKg ->
            BaseSummaryData(
                workout = workout,
                records = records,
                exercises = exercises,
                settings = settings,
                latestBodyWeightKg = latestBodyWeightKg
            )
        }

    val uiState: StateFlow<WorkoutSummaryUiState> =
        combine(
            baseDataFlow,
            routineAndPreviousFlow,
            routineUpdatedFlag
        ) { base, routineContext, wasUpdated ->
            val workoutWithExercises = base.workout
            if (workoutWithExercises == null) {
                return@combine WorkoutSummaryUiState(
                    workoutId = workoutId,
                    isLoaded = true
                )
            }

            val isSpanish = resolveIsSpanish(base.settings.language, systemLanguageProvider())
            val locale = if (isSpanish) Locale.forLanguageTag("es-ES") else Locale.US
            val weightUnit = base.settings.weightUnit
            val exercisesById = base.exercises.associateBy { it.id }
            val trackingTypesById = exercisesById.mapValues { (_, ex) -> ex.trackingType }

            val endedAt = workoutWithExercises.workout.endedAt ?: workoutWithExercises.workout.updatedAt
            val durationMs = (endedAt - workoutWithExercises.workout.startedAt).coerceAtLeast(0L)
            val volumeKg = computeWorkoutVolumeKg(workoutWithExercises, trackingTypesById, base.latestBodyWeightKg)

            val recordsBySetId = Records.groupRecordTypesBySetId(base.records)
            val setsById =
                workoutWithExercises.exercises
                    .asSequence()
                    .flatMap { it.sets.asSequence() }
                    .associateBy { it.id }

            val brokenRecordsUi =
                base.records.map { pr ->
                    val ex = exercisesById[pr.exerciseId]
                    val trackingType = ex?.trackingType ?: TrackingType.WEIGHT_REPS
                    val matchingSet = setsById[pr.workoutSetId]
                    WorkoutSummaryRecordUi(
                        id = pr.id,
                        exerciseId = pr.exerciseId,
                        exerciseName = ex?.localizedName(isSpanish) ?: pr.exerciseId,
                        trackingType = trackingType,
                        recordType = pr.recordType,
                        value = pr.value,
                        setId = pr.workoutSetId,
                        setWeightKg = matchingSet?.weightKg,
                        setReps = matchingSet?.reps,
                        setDurationS = matchingSet?.durationS,
                        setDistanceM = matchingSet?.distanceM
                    )
                }

            val exercisesUi =
                workoutWithExercises.exercises.map { ex ->
                    val domainEx = exercisesById[ex.workoutExercise.exerciseId]
                    val trackingType = domainEx?.trackingType ?: TrackingType.WEIGHT_REPS
                    val completedSets = ex.sets.filter { it.completed }.sortedBy { it.position }
                    val normalNumbers = computeNormalSetNumbers(completedSets.map { it.setType })
                    val setsUi =
                        completedSets.mapIndexed { idx, set ->
                            WorkoutSummarySetUi(
                                id = set.id,
                                setType = set.setType,
                                normalSetNumber = normalNumbers.getOrElse(idx) { idx + 1 },
                                weightKg = set.weightKg,
                                reps = set.reps,
                                durationS = set.durationS,
                                distanceM = set.distanceM,
                                rpe = set.rpe,
                                recordTypes = recordsBySetId[set.id].orEmpty()
                            )
                        }
                    WorkoutSummaryExerciseUi(
                        id = ex.workoutExercise.id,
                        exerciseId = ex.workoutExercise.exerciseId,
                        name = domainEx?.localizedName(isSpanish) ?: ex.workoutExercise.exerciseId,
                        trackingType = trackingType,
                        supersetGroup = ex.workoutExercise.supersetGroup,
                        completedSetsCount = completedSets.size,
                        sets = setsUi
                    )
                }

            val comparisonUi =
                routineContext.previousRoutineWorkout?.let { prevWorkout ->
                    val comp: RoutineLastSessionComparison =
                        computeRoutineComparison(
                            currentWorkout = workoutWithExercises,
                            previousWorkout = prevWorkout,
                            trackingTypesByExerciseId = trackingTypesById
                        )
                    WorkoutSummaryComparisonUi(
                        durationDeltaMillis = comp.durationDeltaMillis,
                        volumeDeltaKg = comp.volumeDeltaKg,
                        completedSetsDelta = comp.completedSetsDelta
                    )
                }

            val routine = routineContext.routine
            val canUpdateRoutine =
                routine != null &&
                    !wasUpdated &&
                    hasWorkoutRoutineValuesChanged(workoutWithExercises, routine)

            WorkoutSummaryUiState(
                workoutId = workoutWithExercises.workout.id,
                isLoaded = true,
                workoutName = workoutWithExercises.workout.name,
                routineId = routine?.routine?.id,
                routineName = routine?.routine?.name.orEmpty(),
                durationText = formatElapsedDuration(durationMs),
                volumeText =
                    Units.formatWeight(
                        weightKg = volumeKg,
                        unit = weightUnit,
                        locale = locale,
                        roundingStep = null,
                        includeUnit = true
                    ),
                completedSetsCount = computeCompletedSetsCount(workoutWithExercises),
                weightUnit = weightUnit,
                isSpanish = isSpanish,
                exercises = exercisesUi,
                brokenRecords = brokenRecordsUi,
                routineComparison = comparisonUi,
                canUpdateRoutineValues = canUpdateRoutine,
                routineUpdated = wasUpdated
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutSummaryUiState(workoutId = workoutId)
        )

    fun onUpdateRoutineWithWorkoutValues() {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutWithExercises(workoutId) ?: return@launch
            val routineId = workout.workout.routineId ?: return@launch
            val routine = routineRepository.getRoutineWithExercises(routineId) ?: return@launch
            if (!hasWorkoutRoutineValuesChanged(workout, routine)) return@launch

            val updatedRoutine =
                applyWorkoutValuesToRoutine(
                    routine = routine,
                    workout = workout,
                    nowMillis = clock.millis()
                )
            routineRepository.saveRoutineWithExercises(updatedRoutine)
            routineUpdatedFlag.value = true
        }
    }

    private data class BaseSummaryData(
        val workout: WorkoutWithExercises?,
        val records: List<PersonalRecord>,
        val exercises: List<Exercise>,
        val settings: Settings,
        val latestBodyWeightKg: Double?
    )

    private data class RoutineContext(
        val routine: com.davidguijo.ferro.core.domain.model.RoutineWithExercises?,
        val previousRoutineWorkout: WorkoutWithExercises?
    )
}
