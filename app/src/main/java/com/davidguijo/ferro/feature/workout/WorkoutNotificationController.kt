package com.davidguijo.ferro.feature.workout

import com.davidguijo.ferro.core.domain.RestTimerState
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises

/** Puerto de notificaciones, servicio en primer plano y alarmas para el entreno activo. */
interface WorkoutNotificationController {
    fun syncWorkoutNotification(workout: WorkoutWithExercises, restTimer: RestTimerState?)

    fun stopWorkoutNotification()

    fun scheduleRestAlarm(workoutId: String, workoutName: String, endAtMillis: Long)

    fun cancelRestAlarm()

    fun notifyRestFinished(workoutName: String, settings: Settings, isWorkoutScreenVisible: Boolean)

    fun cancelRestFinishedNotification()

    fun canScheduleExactAlarms(): Boolean
}
