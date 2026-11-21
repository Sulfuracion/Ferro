package com.davidguijo.ferro.feature.workout.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import com.davidguijo.ferro.feature.workout.WorkoutNotificationController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Implementación Android de [WorkoutNotificationController]. */
@Singleton
class AndroidWorkoutNotificationController
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmScheduler: RestTimerAlarmScheduler,
    private val alertNotifier: RestTimerAlertNotifier
) : WorkoutNotificationController {
    override fun syncWorkoutNotification(workout: WorkoutWithExercises, restTimer: RestTimerState?) {
        val intent =
            Intent(context, ActiveWorkoutService::class.java).apply {
                action = ActiveWorkoutService.ACTION_START_OR_UPDATE
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: ForegroundServiceStartNotAllowedException) {
                // Si el proceso fue despertado en segundo plano sin exención FGS, evita crash;
                // al abrir MainActivity.onStart() se re-sincronizará la notificación en primer plano.
            } catch (_: IllegalStateException) {
                // Defensa adicional ante restricciones de inicio en segundo plano.
            }
        } else {
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun stopWorkoutNotification() {
        context.stopService(Intent(context, ActiveWorkoutService::class.java))
    }

    override fun scheduleRestAlarm(workoutId: String, workoutName: String, endAtMillis: Long) {
        alarmScheduler.schedule(
            workoutId = workoutId,
            workoutName = workoutName,
            endAtMillis = endAtMillis
        )
    }

    override fun cancelRestAlarm() {
        alarmScheduler.cancel()
    }

    override fun notifyRestFinished(workoutName: String, settings: Settings, isWorkoutScreenVisible: Boolean) {
        alertNotifier.notifyRestFinished(
            workoutName = workoutName,
            settings = settings,
            isWorkoutScreenVisible = isWorkoutScreenVisible
        )
    }

    override fun cancelRestFinishedNotification() {
        alertNotifier.cancelRestFinishedNotification()
    }

    override fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()
}
