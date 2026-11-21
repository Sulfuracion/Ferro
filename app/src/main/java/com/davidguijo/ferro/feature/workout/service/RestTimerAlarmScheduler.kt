package com.davidguijo.ferro.feature.workout.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa la alarma `RTC_WAKEUP` para el fin del descanso con pantalla apagada.
 * Usa alarma exacta cuando está permitida y degrada a `setAndAllowWhileIdle` sin lanzar excepciones.
 */
@Singleton
class RestTimerAlarmScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(workoutId: String, workoutName: String, endAtMillis: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(workoutId, workoutName, endAtMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (manager.canScheduleExactAlarms()) {
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
                } catch (_: SecurityException) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
                }
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
            }
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        }
    }

    fun cancel() {
        val manager = alarmManager ?: return
        val intent = Intent(context, RestTimerAlarmReceiver::class.java)
        val existing =
            PendingIntent.getBroadcast(
                context,
                REQUEST_ALARM,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
        manager.cancel(existing)
        existing.cancel()
    }

    private fun buildAlarmPendingIntent(workoutId: String, workoutName: String, endAtMillis: Long): PendingIntent {
        val intent =
            Intent(context, RestTimerAlarmReceiver::class.java).apply {
                putExtra(RestTimerAlarmReceiver.EXTRA_WORKOUT_ID, workoutId)
                putExtra(RestTimerAlarmReceiver.EXTRA_WORKOUT_NAME, workoutName)
                putExtra(RestTimerAlarmReceiver.EXTRA_END_AT_MILLIS, endAtMillis)
            }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_ALARM = 4201
    }
}
