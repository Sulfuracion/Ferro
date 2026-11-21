package com.davidguijo.ferro.feature.workout.service

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.data.di.ApplicationScope
import com.davidguijo.ferro.core.domain.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Emite el aviso de fin de descanso: vibración con atributo `USAGE_ALARM` (válida en segundo plano
 * y con pantalla apagada) y notificación alta / aviso sonoro según visibilidad y ajustes.
 */
@Singleton
class RestTimerAlertNotifier
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationFactory: WorkoutNotificationFactory,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    fun notifyRestFinished(workoutName: String, settings: Settings, isWorkoutScreenVisible: Boolean) {
        if (settings.timerVibrationEnabled) {
            vibrateAlarm()
        }

        val powerManager = context.getSystemService(PowerManager::class.java)
        val isScreenInteractive = powerManager?.isInteractive ?: true

        if (isWorkoutScreenVisible && isScreenInteractive) {
            if (settings.timerSoundEnabled) {
                playForegroundCueSound()
            }
            return
        }

        postRestFinishedNotification(
            workoutName = workoutName,
            soundEnabled = settings.timerSoundEnabled
        )
    }

    fun cancelRestFinishedNotification() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(REST_FINISHED_NOTIFICATION_ID)
    }

    private fun postRestFinishedNotification(workoutName: String, soundEnabled: Boolean) {
        if (!hasPostNotificationsPermission()) return
        WorkoutNotificationChannels.ensureCreated(context)

        val channelId =
            if (soundEnabled) {
                WorkoutNotificationChannels.CHANNEL_REST_FINISHED_SOUND
            } else {
                WorkoutNotificationChannels.CHANNEL_REST_FINISHED_SILENT
            }
        val displayWorkoutName = workoutName.ifBlank { context.getString(R.string.active_workout_default_name) }

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.rest_finished_notification_title))
                .setContentText(context.getString(R.string.rest_finished_notification_body, displayWorkoutName))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(notificationFactory.createOpenWorkoutPendingIntent())
                .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(REST_FINISHED_NOTIFICATION_ID, notification)
    }

    private fun hasPostNotificationsPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    private fun vibrateAlarm() {
        val vibrator = resolveVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(REST_VIBRATION_PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs =
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
            vibrator.vibrate(effect, attrs)
        } else {
            vibrateLegacyWithAudioAttributes(vibrator, effect)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacyWithAudioAttributes(vibrator: Vibrator, effect: VibrationEffect) {
        val attrs =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        vibrator.vibrate(effect, attrs)
    }

    private fun playForegroundCueSound() {
        runCatching {
            val uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: return
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
            }
            ringtone.play()
            scope.launch {
                delay(FOREGROUND_RING_DURATION_MS)
                runCatching {
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                    }
                }
            }
        }
    }

    private companion object {
        const val REST_FINISHED_NOTIFICATION_ID = 1002
        const val FOREGROUND_RING_DURATION_MS = 1_400L
        val REST_VIBRATION_PATTERN = longArrayOf(0L, 260L, 140L, 260L)
    }
}
