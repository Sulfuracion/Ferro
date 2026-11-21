package com.davidguijo.ferro.feature.workout.di

import com.davidguijo.ferro.feature.workout.WorkoutNotificationController
import com.davidguijo.ferro.feature.workout.service.AndroidWorkoutNotificationController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutModule {
    @Binds
    @Singleton
    abstract fun bindWorkoutNotificationController(
        impl: AndroidWorkoutNotificationController
    ): WorkoutNotificationController
}
