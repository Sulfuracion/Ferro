package com.davidguijo.ferro.core.data.seed

import android.app.Application
import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.data.local.createInMemoryFerroDatabase
import com.davidguijo.ferro.core.data.repository.RoomExerciseRepository
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExerciseSeederTest {
    private lateinit var database: FerroDatabase
    private lateinit var repository: RoomExerciseRepository
    private val clock = Clock.fixed(Instant.ofEpochMilli(10_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = createInMemoryFerroDatabase()
        repository = RoomExerciseRepository(database.exerciseDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `el archivo seed exercises json contiene 120 ejercicios válidos`() = runTest {
        val seedFile = locateSeedFile()
        val seeder =
            ExerciseSeeder(
                exerciseRepository = repository,
                streamProvider = { seedFile.inputStream() },
                clock = clock
            )

        val inserted = seeder.seedIfNeeded()
        val all = repository.observeAllExercises().first()

        assertEquals(120, inserted)
        assertEquals(120, all.size)
        assertEquals(120, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.nameEs.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(all.all { it.instructionsEs.isNotBlank() && it.instructionsEn.isNotBlank() })
        assertTrue(all.all { !it.isCustom && !it.archived })
        assertEquals(Muscle.entries.toSet(), all.map { it.primaryMuscle }.toSet())
        assertEquals(Equipment.entries.toSet(), all.map { it.equipment }.toSet())
    }

    @Test
    fun `la siembra es idempotente y respeta archivados, personalizados y nuevos semilla`() = runTest {
        val v1Json =
            """
            [
              {
                "id": "seed-1",
                "nameEs": "Press banca",
                "nameEn": "Bench press",
                "primaryMuscle": "CHEST",
                "secondaryMuscles": ["TRICEPS"],
                "equipment": "BARBELL",
                "trackingType": "WEIGHT_REPS",
                "instructionsEs": "1. Baja.\n2. Sube.",
                "instructionsEn": "1. Lower.\n2. Press."
              }
            ]
            """.trimIndent()

        val seederV1 =
            ExerciseSeeder(
                exerciseRepository = repository,
                streamProvider = { v1Json.byteInputStream() },
                clock = clock
            )
        assertEquals(1, seederV1.seedIfNeeded())

        // El usuario archiva un ejercicio semilla y crea uno personalizado
        repository.setArchived("seed-1", archived = true)
        val custom =
            Exercise(
                id = "custom-1",
                nameEs = "Press propio",
                nameEn = "Custom press",
                primaryMuscle = Muscle.CHEST,
                secondaryMuscles = emptyList(),
                equipment = Equipment.DUMBBELL,
                trackingType = TrackingType.WEIGHT_REPS,
                instructionsEs = "1. Empuja.",
                instructionsEn = "1. Press.",
                isCustom = true,
                archived = false,
                createdAt = 12_000L,
                updatedAt = 12_000L
            )
        repository.upsertExercise(custom)

        // Segunda siembra con v1: no inserta ni sobrescribe nada
        assertEquals(0, seederV1.seedIfNeeded())
        assertEquals(true, repository.getExerciseById("seed-1")?.archived)
        assertEquals(true, repository.getExerciseById("custom-1")?.isCustom)

        // Futura versión v2 añade un nuevo ejercicio semilla
        val v2Json =
            """
            [
              {
                "id": "seed-1",
                "nameEs": "Press banca modificado",
                "nameEn": "Modified bench",
                "primaryMuscle": "CHEST",
                "secondaryMuscles": [],
                "equipment": "BARBELL",
                "trackingType": "WEIGHT_REPS",
                "instructionsEs": "Nuevo",
                "instructionsEn": "New"
              },
              {
                "id": "seed-2",
                "nameEs": "Sentadilla",
                "nameEn": "Squat",
                "primaryMuscle": "QUADS",
                "secondaryMuscles": ["GLUTES"],
                "equipment": "BARBELL",
                "trackingType": "WEIGHT_REPS",
                "instructionsEs": "1. Baja.\n2. Sube.",
                "instructionsEn": "1. Squat.\n2. Stand."
              }
            ]
            """.trimIndent()

        val seederV2 =
            ExerciseSeeder(
                exerciseRepository = repository,
                streamProvider = { v2Json.byteInputStream() },
                clock = clock
            )
        assertEquals(1, seederV2.seedIfNeeded())

        val seed1 = repository.getExerciseById("seed-1")
        assertEquals("Press banca", seed1?.nameEs)
        assertEquals(true, seed1?.archived)
        assertEquals("Sentadilla", repository.getExerciseById("seed-2")?.nameEs)
        assertEquals("Press propio", repository.getExerciseById("custom-1")?.nameEs)
    }

    private fun locateSeedFile(): File {
        val candidates =
            listOf(
                File("src/main/assets/seed/exercises.json"),
                File("app/src/main/assets/seed/exercises.json")
            )
        return candidates.firstOrNull { it.exists() }
            ?: error("No se encontró exercises.json en ${candidates.map { it.absolutePath }}")
    }
}
