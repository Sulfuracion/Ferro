package com.davidguijo.ferro.feature.workout.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.davidguijo.ferro.core.domain.RestTimer
import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.computeCompletedSetsCount
import com.davidguijo.ferro.core.domain.computeTotalSetsCount
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano (`specialUse`) que mantiene la notificación persistente del entreno
 * activo y vigila la finalización del descanso mientras el proceso sigue en memoria.
 */
@AndroidEntryPoint
class ActiveWorkoutService : Service() {
    @Inject
    lateinit var activeWorkoutManager: ActiveWorkoutManager

    @Inject
    lateinit var notificationFactory: WorkoutNotificationFactory

    @Inject
    lateinit var clock: Clock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null
    private var restWatcherJob: Job? = null
    private var watchedRestEndMillis: Long? = null
    private var isForegroundStarted: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WorkoutNotificationChannels.ensureCreated(this)

        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }

            ACTION_REST_ADD_15 -> {
                promoteToForeground()
                ensureObserversStarted()
                serviceScope.launch {
                    activeWorkoutManager.restoreIfNeeded()
                    activeWorkoutManager.adjustRestTimer(REST_STEP_SECONDS)
                }
            }

            ACTION_REST_SKIP -> {
                promoteToForeground()
                ensureObserversStarted()
                serviceScope.launch {
                    activeWorkoutManager.restoreIfNeeded()
                    activeWorkoutManager.skipRestTimer()
                }
            }

            else -> {
                promoteToForeground()
                val snapshot = activeWorkoutManager.state.value
                if (snapshot.isRestored && snapshot.workout == null) {
                    stopForegroundAndSelf()
                    return START_NOT_STICKY
                }
                ensureObserversStarted()
            }
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch(Dispatchers.IO) {
            activeWorkoutManager.flushNow()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        restWatcherJob?.cancel()
        stateObserverJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val current = activeWorkoutManager.state.value
        val notification =
            notificationFactory.buildOngoingNotification(
                workout = current.workout,
                restTimer = current.restTimer
            )
        val foregroundType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        ServiceCompat.startForeground(
            this,
            WorkoutNotificationFactory.WORKOUT_NOTIFICATION_ID,
            notification,
            foregroundType
        )
        isForegroundStarted = true
    }

    private fun ensureObserversStarted() {
        if (stateObserverJob != null) return
        stateObserverJob =
            serviceScope.launch {
                activeWorkoutManager.state
                    .distinctUntilChangedBy { sessionState ->
                        val w = sessionState.workout
                        NotificationKey(
                            isRestored = sessionState.isRestored,
                            workoutId = w?.workout?.id,
                            workoutName = w?.workout?.name,
                            completedSets = w?.let { computeCompletedSetsCount(it) } ?: 0,
                            totalSets = w?.let { computeTotalSetsCount(it) } ?: 0,
                            restEndMillis = sessionState.restTimer?.endAtMillis
                        )
                    }
                    .collect { sessionState ->
                        if (!sessionState.isRestored) return@collect
                        val workout = sessionState.workout
                        if (workout == null) {
                            stopForegroundAndSelf()
                            return@collect
                        }
                        updateOngoingNotification(workout, sessionState.restTimer)
                        syncRestWatcher(sessionState.restTimer)
                    }
            }
    }

    private fun updateOngoingNotification(
        workout: com.davidguijo.ferro.core.domain.model.WorkoutWithExercises,
        restTimer: RestTimerState?
    ) {
        if (!isForegroundStarted) return
        val notification = notificationFactory.buildOngoingNotification(workout, restTimer)
        val foregroundType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        ServiceCompat.startForeground(
            this,
            WorkoutNotificationFactory.WORKOUT_NOTIFICATION_ID,
            notification,
            foregroundType
        )
    }

    private fun syncRestWatcher(restTimer: RestTimerState?) {
        val endMillis = restTimer?.endAtMillis
        if (endMillis == null) {
            watchedRestEndMillis = null
            restWatcherJob?.cancel()
            restWatcherJob = null
            return
        }
        if (watchedRestEndMillis == endMillis && restWatcherJob?.isActive == true) {
            return
        }
        watchedRestEndMillis = endMillis
        restWatcherJob?.cancel()
        restWatcherJob =
            serviceScope.launch {
                var lastRenderedMinute = -1L
                while (isActive) {
                    val remainingMs = RestTimer.remainingMillis(restTimer, clock)
                    if (remainingMs <= 0L) {
                        activeWorkoutManager.onRestTimerExpired(
                            expectedEndAtMillis = endMillis,
                            fromBackgroundReceiver = false
                        )
                        break
                    }
                    val activeWorkout = activeWorkoutManager.state.value.workout
                    if (activeWorkout != null) {
                        val elapsedMinutes =
                            ((clock.millis() - activeWorkout.workout.startedAt).coerceAtLeast(0L)) / 60_000L
                        if (lastRenderedMinute >= 0L && elapsedMinutes != lastRenderedMinute) {
                            updateOngoingNotification(activeWorkout, restTimer)
                        }
                        lastRenderedMinute = elapsedMinutes
                    }
                    delay(minOf(remainingMs, WATCHER_POLL_MS))
                }
            }
    }

    private fun stopForegroundAndSelf() {
        restWatcherJob?.cancel()
        stateObserverJob?.cancel()
        isForegroundStarted = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private data class NotificationKey(
        val isRestored: Boolean,
        val workoutId: String?,
        val workoutName: String?,
        val completedSets: Int,
        val totalSets: Int,
        val restEndMillis: Long?
    )

    companion object {
        const val ACTION_START_OR_UPDATE = "com.davidguijo.ferro.action.START_OR_UPDATE_WORKOUT"
        const val ACTION_REST_ADD_15 = "com.davidguijo.ferro.action.REST_ADD_15"
        const val ACTION_REST_SKIP = "com.davidguijo.ferro.action.REST_SKIP"
        const val ACTION_STOP = "com.davidguijo.ferro.action.STOP_WORKOUT_SERVICE"

        private const val REST_STEP_SECONDS = 15
        private const val WATCHER_POLL_MS = 1_000L
    }
}
