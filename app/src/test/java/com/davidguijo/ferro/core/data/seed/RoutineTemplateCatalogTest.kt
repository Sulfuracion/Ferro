package com.davidguijo.ferro.core.data.seed

import com.davidguijo.ferro.core.domain.toRoutineWithExercises
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplateCatalogTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `templates json contiene las cuatro plantillas predefinidas con IDs de ejercicios semilla validos`() = runTest {
        val exercisesFile = locateAssetFile("exercises.json")
        val templatesFile = locateAssetFile("templates.json")

        val seedExercises =
            json.decodeFromString<List<SeedExerciseDto>>(exercisesFile.readText())
        val validExerciseIds = seedExercises.map { it.id }.toSet()

        val catalog =
            RoutineTemplateCatalog(
                streamProvider = { templatesFile.inputStream() }
            )
        val programs = catalog.getTemplatePrograms()

        assertEquals(4, programs.size)
        assertEquals(
            listOf(
                "Full Body 3 días",
                "Torso/Pierna 4 días",
                "Push/Pull/Legs 6 días",
                "Fuerza 5x5"
            ),
            programs.map { it.nameEs }
        )

        programs.forEach { program ->
            assertTrue(program.nameEs.isNotBlank() && program.nameEn.isNotBlank())
            assertTrue(program.routines.isNotEmpty())
            program.routines.forEachIndexed { index, routineTemplate ->
                assertTrue(routineTemplate.nameEs.isNotBlank() && routineTemplate.nameEn.isNotBlank())
                assertTrue(routineTemplate.exercises.isNotEmpty())
                routineTemplate.exercises.forEach { exercise ->
                    assertTrue(
                        "El ejercicio ${exercise.exerciseId} en ${routineTemplate.nameEs} no existe en exercises.json",
                        exercise.exerciseId in validExerciseIds
                    )
                    assertTrue(exercise.sets.isNotEmpty())
                    val rest = exercise.restSeconds
                    assertTrue(rest != null && rest in 30..300)
                }

                val converted =
                    routineTemplate.toRoutineWithExercises(
                        folderId = "folder-1",
                        position = index,
                        isSpanish = true,
                        now = 10_000L
                    )
                assertEquals(routineTemplate.nameEs, converted.routine.name)
                assertEquals(routineTemplate.exercises.size, converted.exercises.size)
            }
        }
    }

    private fun locateAssetFile(fileName: String): File {
        val candidates =
            listOf(
                File("src/main/assets/seed/$fileName"),
                File("app/src/main/assets/seed/$fileName")
            )
        return candidates.firstOrNull { it.exists() }
            ?: error("No se encontró $fileName en ${candidates.map { it.absolutePath }}")
    }
}
