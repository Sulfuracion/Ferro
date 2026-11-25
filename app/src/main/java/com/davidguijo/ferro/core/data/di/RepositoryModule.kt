package com.davidguijo.ferro.core.data.di

import com.davidguijo.ferro.core.data.repository.DataStoreActiveRestTimerStore
import com.davidguijo.ferro.core.data.repository.DataStoreSettingsRepository
import com.davidguijo.ferro.core.data.repository.RoomBackupRepository
import com.davidguijo.ferro.core.data.repository.RoomBodyMeasurementRepository
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.data.repository.RoomPersonalRecordRepository
import com.davidguijo.ferro.core.data.repository.RoomRoutineRepository
import com.davidguijo.ferro.core.data.repository.RoomWorkoutRepository
import com.davidguijo.ferro.core.domain.repository.ActiveRestTimerStore
import com.davidguijo.ferro.core.domain.repository.BackupRepository
import com.davidguijo.ferro.core.domain.repository.BodyMeasurementRepository
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import com.davidguijo.ferro.core.domain.repository.PersonalRecordRepository
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import com.davidguijo.ferro.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: RoomExerciseRepository): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoomRoutineRepository): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: RoomWorkoutRepository): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindPersonalRecordRepository(impl: RoomPersonalRecordRepository): PersonalRecordRepository

    @Binds
    @Singleton
    abstract fun bindBodyMeasurementRepository(impl: RoomBodyMeasurementRepository): BodyMeasurementRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindActiveRestTimerStore(impl: DataStoreActiveRestTimerStore): ActiveRestTimerStore

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository
}
