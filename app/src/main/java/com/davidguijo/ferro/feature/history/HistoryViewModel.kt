package com.davidguijo.ferro.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeWorkoutVolumeKg
import com.davidguijo.ferro.core.domain.localizedName
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import com.davidguijo.ferro.core.domain.weekEndFor
import com.davidguijo.ferro.core.domain.weekStartFor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

sealed interface HistoryListItem {
    val stableKey: String

    data class WeekHeader(val weekStart: LocalDate, val weekEnd: LocalDate, val label: String) : HistoryListItem {
        override val stableKey: String = "week_$weekStart"
    }

    data class Session(
        val workoutId: String,
        val name: String,
        val dateTimeText: String,
        val durationText: String?,
        val volumeText: String,
        val completedSetsCount: Int,
        val mainExerciseNames: List<String>,
        val extraExercisesCount: Int,
        val recordCount: Int,
        val weekStart: LocalDate
    ) : HistoryListItem {
        override val stableKey: String = "session_$workoutId"
    }
}

private data class HistoryMetadata(
    val exercisesById: Map<String, Exercise>,
    val recordCountsByWorkout: Map<String, Int>,
    val settings: Settings,
    val isSpanish: Boolean
)

@HiltViewModel
class HistoryViewModel
internal constructor(
    workoutRepository: WorkoutRepository,
    personalRecordRepository: PersonalRecordRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val zoneId: ZoneId,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        workoutRepository: WorkoutRepository,
        personalRecordRepository: PersonalRecordRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository
    ) : this(
        workoutRepository = workoutRepository,
        personalRecordRepository = personalRecordRepository,
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
        zoneId = ZoneId.systemDefault(),
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val cachedWorkouts = workoutRepository.pagingCompletedWorkouts().cachedIn(viewModelScope)

    private val metadataFlow =
        combine(
            exerciseRepository.observeAllExercises(),
            personalRecordRepository.observeRecordCountsByWorkout(),
            settingsRepository.observeSettings()
        ) { exercises, recordCountsByWorkout, settings ->
            HistoryMetadata(
                exercisesById = exercises.associateBy { it.id },
                recordCountsByWorkout = recordCountsByWorkout,
                settings = settings,
                isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            )
        }

    val pagedItems: Flow<PagingData<HistoryListItem>> =
        combine(cachedWorkouts, metadataFlow) { pagingData, meta ->
            pagingData
                .map { workout ->
                    workout.toHistorySessionItem(
                        exercisesById = meta.exercisesById,
                        recordCount = meta.recordCountsByWorkout[workout.workout.id] ?: 0,
                        weightUnit = meta.settings.weightUnit,
                        firstDayOfWeek = meta.settings.firstDayOfWeek,
                        isSpanish = meta.isSpanish,
                        zoneId = zoneId
                    )
                }.insertSeparators { before, after ->
                    when {
                        after == null -> null

                        before == null || before.weekStart != after.weekStart -> {
                            val weekEnd = weekEndFor(after.weekStart)
                            HistoryListItem.WeekHeader(
                                weekStart = after.weekStart,
                                weekEnd = weekEnd,
                                label = formatWeekRangeLabel(after.weekStart, weekEnd, meta.isSpanish)
                            )
                        }

                        else -> null
                    }
                }
        }
}

private const val MAX_MAIN_EXERCISES = 3

private fun WorkoutWithExercises.toHistorySessionItem(
    exercisesById: Map<String, Exercise>,
    recordCount: Int,
    weightUnit: WeightUnit,
    firstDayOfWeek: DayOfWeek,
    isSpanish: Boolean,
    zoneId: ZoneId
): HistoryListItem.Session {
    val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
    val trackingTypes = exercisesById.mapValues { it.value.trackingType }
    val volumeKg = computeWorkoutVolumeKg(this, trackingTypes)
    val volumeText = Units.formatWeight(volumeKg, weightUnit, locale = locale, includeUnit = true)
    val completedSets = computeCompletedSetsCount(this)

    val durationMillis =
        workout.endedAt?.let { ended ->
            (ended - workout.startedAt).coerceAtLeast(0L)
        }
    val durationText = durationMillis?.let { formatHistoryDuration(it) }

    val distinctNames =
        exercises
            .sortedBy { it.workoutExercise.position }
            .mapNotNull { ex -> exercisesById[ex.workoutExercise.exerciseId]?.localizedName(isSpanish) }
            .distinct()

    val mainExercises = distinctNames.take(MAX_MAIN_EXERCISES)
    val extraCount = (distinctNames.size - MAX_MAIN_EXERCISES).coerceAtLeast(0)

    return HistoryListItem.Session(
        workoutId = workout.id,
        name = workout.name,
        dateTimeText = formatSessionDateTime(workout.startedAt, isSpanish, zoneId),
        durationText = durationText,
        volumeText = volumeText,
        completedSetsCount = completedSets,
        mainExerciseNames = mainExercises,
        extraExercisesCount = extraCount,
        recordCount = recordCount,
        weekStart = weekStartFor(workout.startedAt, firstDayOfWeek, zoneId)
    )
}

internal fun formatWeekRangeLabel(weekStart: LocalDate, weekEnd: LocalDate, isSpanish: Boolean): String {
    val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
    val sameMonth = weekStart.month == weekEnd.month && weekStart.year == weekEnd.year
    val sameYear = weekStart.year == weekEnd.year
    return if (sameMonth) {
        val monthYear = weekEnd.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
        "${weekStart.dayOfMonth}–${weekEnd.dayOfMonth} $monthYear"
    } else if (sameYear) {
        val startFmt = DateTimeFormatter.ofPattern("d MMM", locale)
        val endFmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        "${weekStart.format(startFmt)} – ${weekEnd.format(endFmt)}"
    } else {
        val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        "${weekStart.format(fullFmt)} – ${weekEnd.format(fullFmt)}"
    }
}

internal fun formatSessionDateTime(epochMillis: Long, isSpanish: Boolean, zoneId: ZoneId): String {
    val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    val pattern = if (isSpanish) "EEE, d MMM · HH:mm" else "EEE, MMM d · HH:mm"
    return dateTime.format(DateTimeFormatter.ofPattern(pattern, locale))
}

internal fun formatHistoryDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis / 60_000L).toInt().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "$hours h ${minutes.toString().padStart(2, '0')} min"
    } else {
        "$totalMinutes min"
    }
}
