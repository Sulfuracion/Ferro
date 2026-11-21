package com.davidguijo.ferro.core.data.repository

import com.davidguijo.ferro.core.data.local.dao.RoutineDao
import com.davidguijo.ferro.core.data.mapper.toDomain
import com.davidguijo.ferro.core.data.mapper.toEntity
import com.davidguijo.ferro.core.data.seed.RoutineTemplateCatalog
import com.davidguijo.ferro.core.data.seed.TemplateSeedStreamProvider
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.repository.RoutineRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomRoutineRepository
@Inject
constructor(
    private val routineDao: RoutineDao,
    private val clock: Clock,
    private val templateCatalog: RoutineTemplateCatalog
) : RoutineRepository {
    internal constructor(
        routineDao: RoutineDao,
        clock: Clock
    ) : this(
        routineDao = routineDao,
        clock = clock,
        templateCatalog = RoutineTemplateCatalog(TemplateSeedStreamProvider { "[]".byteInputStream() })
    )

    override fun observeFolders(): Flow<List<RoutineFolder>> =
        routineDao.observeFolders().map { list -> list.map { it.toDomain() } }

    override fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeRoutines().map { list -> list.map { it.toDomain() } }

    override fun observeAllRoutinesWithExercises(): Flow<List<RoutineWithExercises>> =
        routineDao.observeAllRoutinesWithExercises().map { list -> list.map { it.toDomain() } }

    override fun observeRoutinesInFolder(folderId: String?): Flow<List<Routine>> =
        routineDao.observeRoutinesInFolder(folderId).map { list -> list.map { it.toDomain() } }

    override fun observeRoutineWithExercises(routineId: String): Flow<RoutineWithExercises?> =
        routineDao.observeRoutineWithExercises(routineId).map { it?.toDomain() }

    override suspend fun getRoutineWithExercises(routineId: String): RoutineWithExercises? =
        routineDao.getRoutineWithExercises(routineId)?.toDomain()

    override suspend fun upsertFolder(folder: RoutineFolder) {
        routineDao.upsertFolder(folder.toEntity())
    }

    override suspend fun deleteFolder(folderId: String) {
        routineDao.deleteFolderById(folderId)
    }

    override suspend fun upsertRoutines(routines: List<Routine>) {
        if (routines.isNotEmpty()) {
            routineDao.upsertRoutines(routines.map { it.toEntity() })
        }
    }

    override suspend fun saveRoutineWithExercises(routineWithExercises: RoutineWithExercises) {
        val routineEntity = routineWithExercises.routine.toEntity()
        val exerciseEntities = routineWithExercises.exercises.map { it.routineExercise.toEntity() }
        val setEntities = routineWithExercises.exercises.flatMap { exercise -> exercise.sets.map { it.toEntity() } }
        routineDao.saveRoutineWithExercises(
            routine = routineEntity,
            exercises = exerciseEntities,
            sets = setEntities
        )
    }

    override suspend fun saveFolderWithRoutines(
        folder: RoutineFolder?,
        routinesWithExercises: List<RoutineWithExercises>
    ) {
        val folderEntity = folder?.toEntity()
        val routineEntities = routinesWithExercises.map { it.routine.toEntity() }
        val exerciseEntities =
            routinesWithExercises.flatMap { rwe ->
                rwe.exercises.map { it.routineExercise.toEntity() }
            }
        val setEntities =
            routinesWithExercises.flatMap { rwe ->
                rwe.exercises.flatMap { ex -> ex.sets.map { it.toEntity() } }
            }
        routineDao.saveFolderWithRoutines(
            folder = folderEntity,
            routines = routineEntities,
            exercises = exerciseEntities,
            sets = setEntities
        )
    }

    override suspend fun duplicateRoutine(
        sourceRoutineId: String,
        newName: String,
        newRoutineId: String?
    ): RoutineWithExercises? = routineDao
        .duplicateRoutine(
            sourceRoutineId = sourceRoutineId,
            newRoutineId = newRoutineId ?: UUID.randomUUID().toString(),
            newName = newName,
            now = clock.millis()
        )
        ?.toDomain()

    override suspend fun deleteRoutine(routineId: String) {
        routineDao.deleteRoutineById(routineId)
    }

    override suspend fun getTemplatePrograms(): List<RoutineTemplateProgram> = templateCatalog.getTemplatePrograms()
}
