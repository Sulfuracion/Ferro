package com.davidguijo.ferro.feature.workout.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receptor de alarma que dispara la vibración y el aviso de fin de descanso aunque la app esté dormida. */
@AndroidEntryPoint
class RestTimerAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var activeWorkoutManager: ActiveWorkoutManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val expectedEndMillis =
            if (intent.hasExtra(EXTRA_END_AT_MILLIS)) {
                intent.getLongExtra(EXTRA_END_AT_MILLIS, -1L).takeIf { it > 0L }
            } else {
                null
            }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                activeWorkoutManager.restoreIfNeeded()
                activeWorkoutManager.onRestTimerExpired(
                    expectedEndAtMillis = expectedEndMillis,
                    fromBackgroundReceiver = true
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_WORKOUT_ID = "extra_workout_id"
        const val EXTRA_WORKOUT_NAME = "extra_workout_name"
        const val EXTRA_END_AT_MILLIS = "extra_end_at_millis"
    }
}
