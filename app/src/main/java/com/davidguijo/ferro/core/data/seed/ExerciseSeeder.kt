package com.davidguijo.ferro.core.data.seed

import android.content.Context
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.repository.ExerciseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SeedExerciseDto(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: Equipment,
    val trackingType: TrackingType,
    val instructionsEs: String = "",
    val instructionsEn: String = ""
)

internal fun interface ExerciseSeedStreamProvider {
    fun openSeedStream(): InputStream
}

/**
 * Siembra de forma idempotente los ejercicios semilla desde `assets/seed/exercises.json`.
 * Solo inserta los ejercicios cuyo `id` aún no existe en la base de datos, preservando
 * tanto los semilla ya existentes (incluidos los archivados) como los personalizados.
 */
@Singleton
class ExerciseSeeder
internal constructor(
    private val exerciseRepository: ExerciseRepository,
    private val streamProvider: ExerciseSeedStreamProvider,
    private val clock: Clock,
    private val json: Json = DefaultJson
) {
    @Inject
    constructor(
        exerciseRepository: ExerciseRepository,
        @ApplicationContext context: Context,
        clock: Clock
    ) : this(
        exerciseRepository = exerciseRepository,
        streamProvider = ExerciseSeedStreamProvider { context.assets.open(SEED_ASSET_PATH) },
        clock = clock,
        json = DefaultJson
    )

    suspend fun seedIfNeeded(): Int = withContext(Dispatchers.IO) {
        val rawJson = streamProvider.openSeedStream().bufferedReader().use { it.readText() }
        val dtos = json.decodeFromString<List<SeedExerciseDto>>(rawJson)
        val now = clock.millis()
        val exercises = dtos.map { it.toDomain(now) }
        exerciseRepository.insertMissingExercises(exercises)
    }

    private fun SeedExerciseDto.toDomain(timestamp: Long): Exercise = Exercise(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        trackingType = trackingType,
        instructionsEs = instructionsEs,
        instructionsEn = instructionsEn,
        isCustom = false,
        archived = false,
        createdAt = timestamp,
        updatedAt = timestamp
    )

    private companion object {
        const val SEED_ASSET_PATH = "seed/exercises.json"
        val DefaultJson = Json { ignoreUnknownKeys = true }
    }
}
