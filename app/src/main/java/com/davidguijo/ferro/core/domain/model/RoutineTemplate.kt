package com.davidguijo.ferro.core.domain.model

/** Programa predefinido de plantillas (p. ej. Full Body 3 días, Torso/Pierna 4 días). */
data class RoutineTemplateProgram(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val descriptionEs: String,
    val descriptionEn: String,
    val routines: List<RoutineTemplate>
) {
    fun localizedName(isSpanish: Boolean): String = if (isSpanish) {
        nameEs.ifBlank { nameEn }
    } else {
        nameEn.ifBlank { nameEs }
    }

    fun localizedDescription(isSpanish: Boolean): String = if (isSpanish) {
        descriptionEs.ifBlank { descriptionEn }
    } else {
        descriptionEn.ifBlank { descriptionEs }
    }
}

/** Rutina individual dentro de un programa de plantillas. */
data class RoutineTemplate(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val notesEs: String,
    val notesEn: String,
    val exercises: List<RoutineTemplateExercise>
) {
    fun localizedName(isSpanish: Boolean): String = if (isSpanish) {
        nameEs.ifBlank { nameEn }
    } else {
        nameEn.ifBlank { nameEs }
    }

    fun localizedNotes(isSpanish: Boolean): String = if (isSpanish) {
        notesEs.ifBlank { notesEn }
    } else {
        notesEn.ifBlank { notesEs }
    }
}

/** Ejercicio objetivo dentro de una plantilla de rutina. */
data class RoutineTemplateExercise(
    val exerciseId: String,
    val supersetGroup: String?,
    val restSeconds: Int?,
    val notesEs: String,
    val notesEn: String,
    val sets: List<RoutineTemplateSet>
) {
    fun localizedNotes(isSpanish: Boolean): String = if (isSpanish) {
        notesEs.ifBlank { notesEn }
    } else {
        notesEn.ifBlank { notesEs }
    }
}

/** Serie objetivo dentro de un ejercicio de plantilla. */
data class RoutineTemplateSet(
    val setType: SetType,
    val targetWeightKg: Double? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationS: Int? = null,
    val targetDistanceM: Double? = null
)
