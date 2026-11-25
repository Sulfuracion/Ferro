package com.davidguijo.ferro.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.FerroDatabaseMigrations
import com.davidguijo.ferro.core.data.local.dao.BackupDao
import com.davidguijo.ferro.core.data.local.dao.BodyMeasurementDao
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.PersonalRecordDao
import com.davidguijo.ferro.core.data.local.dao.RoutineDao
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

private const val USER_PREFERENCES_FILE = "user_preferences"

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** Base de datos principal de Ferro (Room 3 con AndroidSQLiteDriver y migraciones explícitas). */
    @Provides
    @Singleton
    fun provideFerroDatabase(@ApplicationContext context: Context): FerroDatabase = Room.databaseBuilder<FerroDatabase>(
        context = context,
        name = FerroDatabase.DATABASE_NAME
    )
        .setDriver(AndroidSQLiteDriver())
        .addMigrations(*FerroDatabaseMigrations.ALL)
        .build()

    @Provides
    internal fun provideExerciseDao(database: FerroDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    internal fun provideRoutineDao(database: FerroDatabase): RoutineDao = database.routineDao()

    @Provides
    internal fun provideWorkoutDao(database: FerroDatabase): WorkoutDao = database.workoutDao()

    @Provides
    internal fun providePersonalRecordDao(database: FerroDatabase): PersonalRecordDao = database.personalRecordDao()

    @Provides
    internal fun provideBodyMeasurementDao(database: FerroDatabase): BodyMeasurementDao = database.bodyMeasurementDao()

    @Provides
    internal fun provideBackupDao(database: FerroDatabase): BackupDao = database.backupDao()

    /** DataStore de preferencias de usuario (ajustes). Solo lo usan los repositorios de core.data. */
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES_FILE) }
        )

    /** Reloj inyectable (UTC) para que los cálculos con fechas sean testeables. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    /** Ámbito de aplicación compartido para operaciones de persistencia que deben sobrevivir a pantallas. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
