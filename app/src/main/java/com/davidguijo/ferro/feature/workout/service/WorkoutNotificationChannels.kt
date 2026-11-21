package com.davidguijo.ferro.feature.workout.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.davidguijo.ferro.R

/** Canales de notificación para el entreno en curso y el fin del descanso. */
object WorkoutNotificationChannels {
    const val CHANNEL_ACTIVE_WORKOUT = "ferro_active_workout"
    const val CHANNEL_REST_FINISHED_SOUND = "ferro_rest_finished_sound"
    const val CHANNEL_REST_FINISHED_SILENT = "ferro_rest_finished_silent"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val ongoingChannel =
            NotificationChannel(
                CHANNEL_ACTIVE_WORKOUT,
                context.getString(R.string.notification_channel_active_workout_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_active_workout_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

        val alarmSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        val restFinishedSoundChannel =
            NotificationChannel(
                CHANNEL_REST_FINISHED_SOUND,
                context.getString(R.string.notification_channel_rest_finished_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_rest_finished_description)
                setShowBadge(true)
                setSound(alarmSoundUri, alarmAudioAttributes)
                // La vibración se dispara con VibrationAttributes.USAGE_ALARM para respetar el ajuste
                // y funcionar también con la pantalla apagada y en segundo plano.
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

        val restFinishedSilentChannel =
            NotificationChannel(
                CHANNEL_REST_FINISHED_SILENT,
                context.getString(R.string.notification_channel_rest_finished_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_rest_finished_description)
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

        manager.createNotificationChannels(
            listOf(ongoingChannel, restFinishedSoundChannel, restFinishedSilentChannel)
        )
    }
}
