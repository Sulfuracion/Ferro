package com.davidguijo.ferro.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.ProgressCalculations
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.MuscleRecoveryItem
import com.davidguijo.ferro.core.domain.model.MuscleVolumeComparison
import com.davidguijo.ferro.core.domain.model.ProgressSummaryStats
import com.davidguijo.ferro.core.domain.model.WeeklyVolumePoint
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val isLoaded: Boolean = false,
    val hasError: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val summaryStats: ProgressSummaryStats =
        ProgressSummaryStats(
            workoutsThisWeek = 0,
            avgWorkoutsPerWeek = 0.0,
            avgDurationMinutes = null,
            totalWorkoutsInWindow = 0
        ),
    val weeklyVolume: List<WeeklyVolumePoint> = emptyList(),
    val muscleVolumeComparison: List<MuscleVolumeComparison> = emptyList(),
    val muscleRecovery: List<MuscleRecoveryItem> = emptyList()
) {
    val hasAnyData: Boolean
        get() =
            summaryStats.totalWorkoutsInWindow > 0 ||
                muscleVolumeComparison.isNotEmpty() ||
                muscleRecovery.isNotEmpty()

    val hasWeeklyVolumeData: Boolean
        get() = weeklyVolume.any { it.volumeKg > 0.0 }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel
internal constructor(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
    bodyMeasurementRepository: BodyMeasurementRepository? = null
) : ViewModel() {
    @Inject
    constructor(
        workoutRepository: WorkoutRepository,
        settingsRepository: SettingsRepository,
        bodyMeasurementRepository: BodyMeasurementRepository,
        clock: Clock
    ) : this(
        workoutRepository = workoutRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        zoneId = ZoneId.systemDefault(),
        bodyMeasurementRepository = bodyMeasurementRepository
    )

    private val latestBodyWeightFlow =
        bodyMeasurementRepository?.observeLatestMeasurement()?.map { it?.weightKg } ?: flowOf(null)

    val uiState: StateFlow<ProgressUiState> =
        combine(
            settingsRepository.observeSettings(),
            latestBodyWeightFlow
        ) { settings, latestBodyWeightKg ->
            settings to latestBodyWeightKg
        }.flatMapLatest { (settings, latestBodyWeightKg) ->
            val nowMillis = clock.millis()
            val since12Weeks =
                ProgressCalculations.rollingWeeksStartMillis(
                    nowMillis = nowMillis,
                    firstDayOfWeek = settings.firstDayOfWeek,
                    weeksCount = ProgressCalculations.WEEKLY_VOLUME_WEEKS,
                    zoneId = zoneId
                )
            val since5Weeks =
                ProgressCalculations.rollingWeeksStartMillis(
                    nowMillis = nowMillis,
                    firstDayOfWeek = settings.firstDayOfWeek,
                    weeksCount = ProgressCalculations.MUSCLE_COMPARISON_PREVIOUS_WEEKS + 1,
                    zoneId = zoneId
                )

            combine(
                workoutRepository.observeCompletedWorkoutAggregatesSince(since12Weeks, latestBodyWeightKg),
                workoutRepository.observeMuscleSetAggregatesSince(since5Weeks),
                workoutRepository.observeMuscleLastTrained()
            ) { workoutAggregates, muscleSetAggregates, muscleLastTrained ->
                val currentNow = clock.millis()
                val weeklySeries =
                    ProgressCalculations.computeWeeklyVolumeSeries(
                        sessions = workoutAggregates,
                        nowMillis = currentNow,
                        firstDayOfWeek = settings.firstDayOfWeek,
                        weeksCount = ProgressCalculations.WEEKLY_VOLUME_WEEKS,
                        zoneId = zoneId
                    )
                val summary =
                    ProgressCalculations.computeProgressSummaryStats(
                        weeklySeries = weeklySeries,
                        sessions = workoutAggregates
                    )
                val muscleComparison =
                    ProgressCalculations.computeMuscleVolumeComparison(
                        rows = muscleSetAggregates,
                        nowMillis = currentNow,
                        firstDayOfWeek = settings.firstDayOfWeek,
                        zoneId = zoneId
                    )
                val recovery =
                    ProgressCalculations.computeMuscleRecovery(
                        rows = muscleLastTrained,
                        nowMillis = currentNow,
                        zoneId = zoneId
                    )

                ProgressUiState(
                    isLoaded = true,
                    weightUnit = settings.weightUnit,
                    summaryStats = summary,
                    weeklyVolume = weeklySeries,
                    muscleVolumeComparison = muscleComparison,
                    muscleRecovery = recovery
                )
            }
        }.catch {
            emit(ProgressUiState(isLoaded = true, hasError = true))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressUiState()
        )
}
