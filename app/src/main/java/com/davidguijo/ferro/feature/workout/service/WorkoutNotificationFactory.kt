package com.davidguijo.ferro.feature.workout.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.davidguijo.ferro.MainActivity
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeTotalSetsCount
import com.davidguijo.ferro.core.domain.formatElapsedDuration
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Construye la notificación persistente del entreno activo y sus acciones rápidas. */
@Singleton
class WorkoutNotificationFactory
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock
) {
    fun createOpenWorkoutPendingIntent(): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ACTIVE_WORKOUT
                putExtra(MainActivity.EXTRA_OPEN_ACTIVE_WORKOUT, true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_WORKOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildOngoingNotification(workout: WorkoutWithExercises?, restTimer: RestTimerState?): Notification {
        WorkoutNotificationChannels.ensureCreated(context)
        val openPendingIntent = createOpenWorkoutPendingIntent()
        val now = clock.millis()

        val title =
            workout?.workout?.name?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.active_workout_default_name)

        val builder =
            NotificationCompat.Builder(context, WorkoutNotificationChannels.CHANNEL_ACTIVE_WORKOUT)
                .setSmallIcon(R.drawable.ic_fitness_center)
                .setContentTitle(title)
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (workout == null) {
            return builder
                .setContentText(context.getString(R.string.active_workout_title))
                .build()
        }

        val activeRest = restTimer?.takeIf { it.endAtMillis > now }
        if (activeRest != null) {
            val sessionElapsed = formatElapsedDuration((now - workout.workout.startedAt).coerceAtLeast(0L))
            builder
                .setSubText(context.getString(R.string.rest_timer_label))
                .setContentText(
                    context.getString(R.string.active_workout_notification_rest_content, sessionElapsed)
                )
                .setWhen(activeRest.endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(
                    R.drawable.ic_add,
                    context.getString(R.string.active_workout_notification_action_add_15),
                    serviceActionPendingIntent(
                        action = ActiveWorkoutService.ACTION_REST_ADD_15,
                        requestCode = REQUEST_REST_ADD_15
                    )
                )
                .addAction(
                    R.drawable.ic_check,
                    context.getString(R.string.active_workout_notification_action_skip),
                    serviceActionPendingIntent(
                        action = ActiveWorkoutService.ACTION_REST_SKIP,
                        requestCode = REQUEST_REST_SKIP
                    )
                )
        } else {
            val completedSets = computeCompletedSetsCount(workout)
            val totalSets = computeTotalSetsCount(workout)
            builder
                .setContentText(
                    context.resources.getQuantityString(
                        R.plurals.active_workout_notification_sets,
                        totalSets,
                        completedSets,
                        totalSets
                    )
                )
                .setWhen(workout.workout.startedAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
        }

        return builder.build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent =
            Intent(context, ActiveWorkoutService::class.java).apply {
                this.action = action
            }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val WORKOUT_NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_WORKOUT = 2001
        private const val REQUEST_REST_ADD_15 = 2002
        private const val REQUEST_REST_SKIP = 2003
    }
}
