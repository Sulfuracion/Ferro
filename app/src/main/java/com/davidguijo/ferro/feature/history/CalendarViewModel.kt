package com.davidguijo.ferro.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import com.davidguijo.ferro.core.domain.resolveIsSpanish
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CalendarDayCellUi(
    val date: LocalDate?,
    val dayNumber: Int? = null,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val workoutCount: Int = 0
) {
    val hasWorkouts: Boolean
        get() = workoutCount > 0
}

data class CalendarSessionItemUi(
    val workoutId: String,
    val name: String,
    val timeText: String,
    val durationText: String?
)

data class CalendarUiState(
    val monthTitle: String = "",
    val isCurrentMonth: Boolean = true,
    val weekdayLabels: List<String> = emptyList(),
    val dayCells: List<CalendarDayCellUi> = emptyList(),
    val selectedDateLabel: String = "",
    val selectedDaySessions: List<CalendarSessionItemUi> = emptyList()
)

private data class CalendarNavState(val displayedMonth: YearMonth, val selectedDate: LocalDate)

@HiltViewModel
class CalendarViewModel
internal constructor(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
    private val systemLanguageProvider: () -> String
) : ViewModel() {
    @Inject
    constructor(
        workoutRepository: WorkoutRepository,
        settingsRepository: SettingsRepository,
        clock: Clock
    ) : this(
        workoutRepository = workoutRepository,
        settingsRepository = settingsRepository,
        clock = clock,
        zoneId = ZoneId.systemDefault(),
        systemLanguageProvider = { Locale.getDefault().language }
    )

    private val today: LocalDate
        get() = Instant.ofEpochMilli(clock.millis()).atZone(zoneId).toLocalDate()

    private val navState =
        MutableStateFlow(
            CalendarNavState(
                displayedMonth = YearMonth.from(today),
                selectedDate = today
            )
        )

    val uiState: StateFlow<CalendarUiState> =
        combine(
            workoutRepository.observeCompletedWorkouts(),
            settingsRepository.observeSettings(),
            navState
        ) { workouts, settings, nav ->
            val isSpanish = resolveIsSpanish(settings.language, systemLanguageProvider())
            val locale = if (isSpanish) Locale.forLanguageTag("es") else Locale.ENGLISH
            val currentToday = today
            val workoutsByDate =
                workouts.groupBy { workout ->
                    Instant.ofEpochMilli(workout.startedAt).atZone(zoneId).toLocalDate()
                }

            val monthTitle =
                nav.displayedMonth
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

            val weekdayLabels = buildWeekdayLabels(settings.firstDayOfWeek, locale)
            val dayCells =
                buildMonthGridCells(
                    month = nav.displayedMonth,
                    firstDayOfWeek = settings.firstDayOfWeek,
                    today = currentToday,
                    selectedDate = nav.selectedDate,
                    workoutsByDate = workoutsByDate
                )

            val selectedSessions =
                workoutsByDate[nav.selectedDate]
                    .orEmpty()
                    .sortedByDescending { it.startedAt }
                    .map { workout ->
                        val timeText =
                            Instant
                                .ofEpochMilli(workout.startedAt)
                                .atZone(zoneId)
                                .format(DateTimeFormatter.ofPattern("HH:mm", locale))
                        val durationText =
                            workout.endedAt?.let { ended ->
                                formatHistoryDuration((ended - workout.startedAt).coerceAtLeast(0L))
                            }
                        CalendarSessionItemUi(
                            workoutId = workout.id,
                            name = workout.name,
                            timeText = timeText,
                            durationText = durationText
                        )
                    }

            val selectedDatePattern = if (isSpanish) "EEEE, d 'de' MMMM" else "EEEE, MMMM d"
            val selectedDateLabel =
                nav.selectedDate
                    .format(DateTimeFormatter.ofPattern(selectedDatePattern, locale))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

            CalendarUiState(
                monthTitle = monthTitle,
                isCurrentMonth = nav.displayedMonth == YearMonth.from(currentToday),
                weekdayLabels = weekdayLabels,
                dayCells = dayCells,
                selectedDateLabel = selectedDateLabel,
                selectedDaySessions = selectedSessions
            )
        }.catch {
            emit(CalendarUiState())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState()
        )

    fun onPreviousMonth() {
        navState.update { current ->
            val prev = current.displayedMonth.minusMonths(1)
            current.copy(
                displayedMonth = prev,
                selectedDate = prev.atDay(1)
            )
        }
    }

    fun onNextMonth() {
        navState.update { current ->
            val next = current.displayedMonth.plusMonths(1)
            current.copy(
                displayedMonth = next,
                selectedDate = next.atDay(1)
            )
        }
    }

    fun onTodayClick() {
        val currentToday = today
        navState.value =
            CalendarNavState(
                displayedMonth = YearMonth.from(currentToday),
                selectedDate = currentToday
            )
    }

    fun onSelectDate(date: LocalDate) {
        navState.update { it.copy(selectedDate = date) }
    }
}

internal fun buildWeekdayLabels(firstDayOfWeek: DayOfWeek, locale: Locale): List<String> = (0 until 7).map { offset ->
    val day = DayOfWeek.of(((firstDayOfWeek.value - 1 + offset) % 7) + 1)
    day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
}

internal fun buildMonthGridCells(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate,
    workoutsByDate: Map<LocalDate, List<Workout>>
): List<CalendarDayCellUi> {
    val firstOfMonth = month.atDay(1)
    val leadingEmpty = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val totalDays = month.lengthOfMonth()
    val cells = ArrayList<CalendarDayCellUi>(leadingEmpty + totalDays)

    repeat(leadingEmpty) {
        cells.add(CalendarDayCellUi(date = null))
    }

    for (day in 1..totalDays) {
        val date = month.atDay(day)
        cells.add(
            CalendarDayCellUi(
                date = date,
                dayNumber = day,
                isToday = date == today,
                isSelected = date == selectedDate,
                workoutCount = workoutsByDate[date]?.size ?: 0
            )
        )
    }

    while (cells.size % 7 != 0) {
        cells.add(CalendarDayCellUi(date = null))
    }

    return cells
}
