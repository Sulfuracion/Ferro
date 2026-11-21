package com.davidguijo.ferro.feature.exercises

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/** Modo de apertura de la biblioteca de ejercicios. */
@Keep
enum class ExerciseSelectionMode {
    NONE,
    SINGLE,
    MULTIPLE
}

/** Claves de resultado devueltas en el `SavedStateHandle` de la entrada previa del back stack. */
object ExerciseLibraryResult {
    const val SELECTED_EXERCISE_IDS_KEY = "selected_exercise_ids"
}

/**
 * Biblioteca de ejercicios (se abre desde Rutinas, el entreno activo y Perfil).
 * En modo selector devuelve los IDs elegidos a través del `SavedStateHandle` de la pantalla anterior.
 */
@Serializable
data class ExerciseLibraryRoute(val selectionMode: ExerciseSelectionMode = ExerciseSelectionMode.NONE)
