package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Grupo de ejercicios bajo una misma letra inicial para la lista con cabeceras fijas. */
data class ExerciseLetterGroup(val letter: Char, val exercises: List<Exercise>)

/**
 * Normaliza un texto para búsqueda ignorando tildes, diacríticos y mayúsculas.
 */
fun normalizeForSearch(text: String): String {
    val decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
    return DIACRITICS_REGEX.replace(decomposed, "").lowercase(Locale.ROOT)
}

/** Devuelve el nombre del ejercicio en el idioma activo (con respaldo si está vacío). */
fun Exercise.localizedName(isSpanish: Boolean): String = if (isSpanish) {
    nameEs.ifBlank { nameEn }
} else {
    nameEn.ifBlank { nameEs }
}

/** Devuelve las instrucciones del ejercicio en el idioma activo (con respaldo si están vacías). */
fun Exercise.localizedInstructions(isSpanish: Boolean): String = if (isSpanish) {
    instructionsEs.ifBlank { instructionsEn }
} else {
    instructionsEn.ifBlank { instructionsEs }
}

/** Resuelve si el idioma activo es español a partir de la preferencia y el idioma del sistema. */
fun resolveIsSpanish(language: AppLanguage, systemLanguageCode: String = Locale.getDefault().language): Boolean =
    when (language) {
        AppLanguage.ES -> true
        AppLanguage.EN -> false
        AppLanguage.SYSTEM -> systemLanguageCode.equals("es", ignoreCase = true)
    }

/**
 * Filtra ejercicios por consulta normalizada en el idioma activo, músculo principal y equipo.
 */
fun filterExercises(
    exercises: List<Exercise>,
    query: String,
    muscle: Muscle?,
    equipment: Equipment?,
    isSpanish: Boolean
): List<Exercise> {
    val normalizedQuery = normalizeForSearch(query)
    return exercises.filter { exercise ->
        val matchesMuscle = muscle == null || exercise.primaryMuscle == muscle
        val matchesEquipment = equipment == null || exercise.equipment == equipment
        val matchesQuery =
            normalizedQuery.isEmpty() ||
                normalizeForSearch(exercise.localizedName(isSpanish)).contains(normalizedQuery)
        matchesMuscle && matchesEquipment && matchesQuery
    }
}

/**
 * Ordena los ejercicios alfabéticamente según el idioma activo (sin distinguir tildes)
 * y los agrupa por su letra inicial.
 */
fun groupExercisesByLetter(exercises: List<Exercise>, isSpanish: Boolean): List<ExerciseLetterGroup> {
    if (exercises.isEmpty()) return emptyList()
    val sorted =
        exercises.sortedWith(
            compareBy<Exercise> { normalizeForSearch(it.localizedName(isSpanish)) }
                .thenBy { it.id }
        )
    return sorted
        .groupBy { exercise ->
            val first = normalizeForSearch(exercise.localizedName(isSpanish)).firstOrNull()
            if (first != null && first.isLetter()) {
                first.uppercaseChar()
            } else {
                '#'
            }
        }
        .map { (letter, group) -> ExerciseLetterGroup(letter = letter, exercises = group) }
}
