package com.davidguijo.ferro.core.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.davidguijo.ferro.core.data.local.dao.BackupDao
import com.davidguijo.ferro.core.data.local.dao.BodyMeasurementDao
import com.davidguijo.ferro.core.data.local.dao.ExerciseDao
import com.davidguijo.ferro.core.data.local.dao.PersonalRecordDao
import com.davidguijo.ferro.core.data.local.dao.RoutineDao
import com.davidguijo.ferro.core.data.local.dao.WorkoutDao
import com.davidguijo.ferro.core.data.local.entity.BodyMeasurementEntity
import com.davidguijo.ferro.core.data.local.entity.ExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.PersonalRecordEntity
import com.davidguijo.ferro.core.data.local.entity.ProgressPhotoEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineFolderEntity
import com.davidguijo.ferro.core.data.local.entity.RoutineSetEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutExerciseEntity
import com.davidguijo.ferro.core.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineFolderEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class
    ],
    version = FerroDatabase.SCHEMA_VERSION,
    exportSchema = true
)
@ColumnTypeConverters(FerroTypeConverters::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class FerroDatabase : RoomDatabase() {
    internal abstract fun exerciseDao(): ExerciseDao

    internal abstract fun routineDao(): RoutineDao

    internal abstract fun workoutDao(): WorkoutDao

    internal abstract fun personalRecordDao(): PersonalRecordDao

    internal abstract fun bodyMeasurementDao(): BodyMeasurementDao

    internal abstract fun backupDao(): BackupDao

    companion object {
        const val DATABASE_NAME: String = "ferro.db"
        const val SCHEMA_VERSION: Int = 1
    }
}
