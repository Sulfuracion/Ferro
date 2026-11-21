package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSearchTest {
    private val bench =
        sampleExercise(
            id = "1",
            nameEs = "Press banca con barra",
            nameEn = "Barbell bench press",
            primaryMuscle = Muscle.CHEST,
            equipment = Equipment.BARBELL
        )
    private val biceps =
        sampleExercise(
            id = "2",
            nameEs = "Curl de bíceps con mancuernas",
            nameEn = "Dumbbell biceps curl",
            primaryMuscle = Muscle.BICEPS,
            equipment = Equipment.DUMBBELL
        )
    private val backExt =
        sampleExercise(
            id = "3",
            nameEs = "Extensión lumbar en banco",
            nameEn = "Back extension",
            primaryMuscle = Muscle.LOWER_BACK,
            equipment = Equipment.BENCH
        )

    @Test
    fun `normalizeForSearch elimina tildes, diacríticos y mayúsculas`() {
        assertEquals("curl de biceps", normalizeForSearch("  Curl de BÍCEPS "))
        assertEquals("extension cuadriceps", normalizeForSearch("Extensión Cuádriceps"))
        assertEquals("pinon", normalizeForSearch("Piñón"))
    }

    @Test
    fun `filterExercises busca solo en el idioma activo ignorando tildes`() {
        val all = listOf(bench, biceps, backExt)

        val esPress = filterExercises(all, query = "press banca", muscle = null, equipment = null, isSpanish = true)
        assertEquals(listOf("1"), esPress.map { it.id })

        val esBicepsNoAccent = filterExercises(all, query = "biceps", muscle = null, equipment = null, isSpanish = true)
        assertEquals(listOf("2"), esBicepsNoAccent.map { it.id })

        val esBench = filterExercises(all, query = "bench", muscle = null, equipment = null, isSpanish = true)
        assertEquals(emptyList<String>(), esBench.map { it.id })

        val enBench = filterExercises(all, query = "bench", muscle = null, equipment = null, isSpanish = false)
        assertEquals(listOf("1"), enBench.map { it.id })

        val enPressBanca =
            filterExercises(all, query = "press banca", muscle = null, equipment = null, isSpanish = false)
        assertEquals(emptyList<String>(), enPressBanca.map { it.id })
    }

    @Test
    fun `filterExercises combina consulta, músculo y equipo`() {
        val all = listOf(bench, biceps, backExt)

        val filtered =
            filterExercises(
                exercises = all,
                query = "",
                muscle = Muscle.CHEST,
                equipment = Equipment.BARBELL,
                isSpanish = true
            )
        assertEquals(listOf("1"), filtered.map { it.id })

        val mismatch =
            filterExercises(
                exercises = all,
                query = "banca",
                muscle = Muscle.CHEST,
                equipment = Equipment.DUMBBELL,
                isSpanish = true
            )
        assertEquals(emptyList<String>(), mismatch.map { it.id })
    }

    @Test
    fun `groupExercisesByLetter agrupa y ordena según el idioma activo`() {
        val accentedA =
            sampleExercise(
                id = "4",
                nameEs = "Ángel en pared",
                nameEn = "Wall angel",
                primaryMuscle = Muscle.SHOULDERS,
                equipment = Equipment.BODYWEIGHT
            )
        val groupsEs = groupExercisesByLetter(listOf(bench, biceps, accentedA), isSpanish = true)
        assertEquals(listOf('A', 'C', 'P'), groupsEs.map { it.letter })

        val groupsEn = groupExercisesByLetter(listOf(bench, biceps, accentedA), isSpanish = false)
        assertEquals(listOf('B', 'D', 'W'), groupsEn.map { it.letter })
    }

    @Test
    fun `resolveIsSpanish resuelve idioma explícito y del sistema`() {
        assertEquals(true, resolveIsSpanish(AppLanguage.ES, "en"))
        assertEquals(false, resolveIsSpanish(AppLanguage.EN, "es"))
        assertEquals(true, resolveIsSpanish(AppLanguage.SYSTEM, "es"))
        assertEquals(false, resolveIsSpanish(AppLanguage.SYSTEM, "en"))
    }

    private fun sampleExercise(
        id: String,
        nameEs: String,
        nameEn: String,
        primaryMuscle: Muscle,
        equipment: Equipment
    ): Exercise = Exercise(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        trackingType = TrackingType.WEIGHT_REPS,
        instructionsEs = "",
        instructionsEn = "",
        isCustom = false,
        archived = false,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )
}
