package com.davidguijo.ferro

import android.app.Application
import com.davidguijo.ferro.core.data.di.ApplicationScope
import com.davidguijo.ferro.core.data.seed.ExerciseSeeder
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import com.davidguijo.ferro.feature.workout.service.WorkoutNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class FerroApplication : Application() {
    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    @Inject
    lateinit var activeWorkoutManager: ActiveWorkoutManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        WorkoutNotificationChannels.ensureCreated(this)
        applicationScope.launch {
            exerciseSeeder.seedIfNeeded()
        }
        applicationScope.launch {
            activeWorkoutManager.restoreIfNeeded()
        }
    }
}
