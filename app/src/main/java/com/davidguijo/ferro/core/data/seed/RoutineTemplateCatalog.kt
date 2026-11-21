package com.davidguijo.ferro.core.data.seed

import android.content.Context
import com.davidguijo.ferro.core.domain.model.RoutineTemplate
import com.davidguijo.ferro.core.domain.model.RoutineTemplateExercise
import com.davidguijo.ferro.core.domain.model.RoutineTemplateProgram
import com.davidguijo.ferro.core.domain.model.RoutineTemplateSet
import com.davidguijo.ferro.core.domain.model.SetType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SeedTemplateProgramDto(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val descriptionEs: String = "",
    val descriptionEn: String = "",
    val routines: List<SeedTemplateRoutineDto> = emptyList()
)

@Serializable
internal data class SeedTemplateRoutineDto(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val notesEs: String = "",
    val notesEn: String = "",
    val exercises: List<SeedTemplateExerciseDto> = emptyList()
)

@Serializable
internal data class SeedTemplateExerciseDto(
    val exerciseId: String,
    val supersetGroup: String? = null,
    val restSeconds: Int? = null,
    val notesEs: String = "",
    val notesEn: String = "",
    val sets: List<SeedTemplateSetDto> = emptyList()
)

@Serializable
internal data class SeedTemplateSetDto(
    val setType: SetType = SetType.NORMAL,
    val targetWeightKg: Double? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationS: Int? = null,
    val targetDistanceM: Double? = null
)

internal fun interface TemplateSeedStreamProvider {
    fun openTemplatesStream(): InputStream
}

/**
 * Carga y cachea en memoria las plantillas predefinidas desde `assets/seed/templates.json`.
 */
@Singleton
class RoutineTemplateCatalog
internal constructor(
    private val streamProvider: TemplateSeedStreamProvider,
    private val json: Json = DefaultJson
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(
        streamProvider = TemplateSeedStreamProvider { context.assets.open(TEMPLATES_ASSET_PATH) },
        json = DefaultJson
    )

    private val mutex = Mutex()
    private var cachedPrograms: List<RoutineTemplateProgram>? = null

    suspend fun getTemplatePrograms(): List<RoutineTemplateProgram> = mutex.withLock {
        cachedPrograms?.let { return@withLock it }
        val loaded =
            withContext(Dispatchers.IO) {
                val rawJson = streamProvider.openTemplatesStream().bufferedReader().use { it.readText() }
                val dtos = json.decodeFromString<List<SeedTemplateProgramDto>>(rawJson)
                dtos.map { it.toDomain() }
            }
        cachedPrograms = loaded
        loaded
    }

    private fun SeedTemplateProgramDto.toDomain(): RoutineTemplateProgram = RoutineTemplateProgram(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        descriptionEs = descriptionEs,
        descriptionEn = descriptionEn,
        routines = routines.map { it.toDomain() }
    )

    private fun SeedTemplateRoutineDto.toDomain(): RoutineTemplate = RoutineTemplate(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        notesEs = notesEs,
        notesEn = notesEn,
        exercises = exercises.map { it.toDomain() }
    )

    private fun SeedTemplateExerciseDto.toDomain(): RoutineTemplateExercise = RoutineTemplateExercise(
        exerciseId = exerciseId,
        supersetGroup = supersetGroup,
        restSeconds = restSeconds,
        notesEs = notesEs,
        notesEn = notesEn,
        sets = sets.map { it.toDomain() }
    )

    private fun SeedTemplateSetDto.toDomain(): RoutineTemplateSet = RoutineTemplateSet(
        setType = setType,
        targetWeightKg = targetWeightKg,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        targetDurationS = targetDurationS,
        targetDistanceM = targetDistanceM
    )

    private companion object {
        const val TEMPLATES_ASSET_PATH = "seed/templates.json"
        val DefaultJson = Json { ignoreUnknownKeys = true }
    }
}
