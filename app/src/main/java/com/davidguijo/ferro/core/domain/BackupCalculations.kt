package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Cálculos puros para copias de seguridad: antigüedad del aviso (> 30 días) y generación RFC 4180
 * del CSV de series para hojas de cálculo.
 */
object BackupCalculations {
    const val STALE_BACKUP_THRESHOLD_DAYS: Long = 30L
    const val MILLIS_PER_DAY: Long = 86_400_000L

    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Devuelve `true` si la última copia tiene más de 30 días respecto a [nowMillis],
     * o si nunca se ha hecho una copia (`lastBackupAtMillis == null`) y el usuario ya tiene algún
     * entreno completado ([hasCompletedWorkouts]).
     */
    fun isBackupReminderDue(lastBackupAtMillis: Long?, nowMillis: Long, hasCompletedWorkouts: Boolean): Boolean {
        if (lastBackupAtMillis == null) {
            return hasCompletedWorkouts
        }
        val elapsedMillis = (nowMillis - lastBackupAtMillis).coerceAtLeast(0L)
        return elapsedMillis > STALE_BACKUP_THRESHOLD_DAYS * MILLIS_PER_DAY
    }

    /** Días enteros transcurridos desde [lastBackupAtMillis] hasta [nowMillis], o `null` si no hay copia. */
    fun daysSinceLastBackup(lastBackupAtMillis: Long?, nowMillis: Long): Int? {
        if (lastBackupAtMillis == null) return null
        val elapsedMillis = (nowMillis - lastBackupAtMillis).coerceAtLeast(0L)
        return (elapsedMillis / MILLIS_PER_DAY).toInt()
    }

    /**
     * Construye un CSV RFC 4180 con todas las series completadas de entrenos completados.
     * Columnas: fecha, entreno, ejercicio, tipo de serie, peso en la unidad elegida, reps, RPE,
     * duración, distancia, notas.
     */
    fun buildSeriesCsv(
        workouts: List<Workout>,
        workoutExercises: List<WorkoutExercise>,
        workoutSets: List<WorkoutSet>,
        exercisesById: Map<String, Exercise>,
        weightUnit: WeightUnit,
        zoneId: ZoneId,
        locale: Locale = Locale.US,
        useEnglishExerciseNames: Boolean = false
    ): String {
        val weightHeader =
            when (weightUnit) {
                WeightUnit.KG -> "weight_kg"
                WeightUnit.LB -> "weight_lb"
            }
        val headers =
            listOf(
                "date",
                "workout",
                "exercise",
                "set_type",
                weightHeader,
                "reps",
                "rpe",
                "duration_s",
                "distance_m",
                "notes"
            )

        val completedWorkouts =
            workouts
                .filter { it.status == WorkoutStatus.COMPLETED }
                .sortedWith(compareBy<Workout> { it.startedAt }.thenBy { it.id })
        val exercisesByWorkoutId =
            workoutExercises
                .groupBy { it.workoutId }
                .mapValues { (_, list) -> list.sortedWith(compareBy<WorkoutExercise> { it.position }.thenBy { it.id }) }
        val setsByWorkoutExerciseId =
            workoutSets
                .filter { it.completed }
                .groupBy { it.workoutExerciseId }
                .mapValues { (_, list) -> list.sortedWith(compareBy<WorkoutSet> { it.position }.thenBy { it.id }) }

        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escapeCsvField(it) })

        for (workout in completedWorkouts) {
            val dateStr = Instant.ofEpochMilli(workout.startedAt).atZone(zoneId).toLocalDate().format(isoDateFormatter)
            val workoutExercisesForSession = exercisesByWorkoutId[workout.id].orEmpty()
            for (we in workoutExercisesForSession) {
                val sets = setsByWorkoutExerciseId[we.id].orEmpty()
                if (sets.isEmpty()) continue
                val exercise = exercisesById[we.exerciseId]
                val exerciseName =
                    when {
                        exercise == null -> we.exerciseId
                        useEnglishExerciseNames && exercise.nameEn.isNotBlank() -> exercise.nameEn
                        else -> exercise.nameEs.ifBlank { exercise.nameEn }
                    }
                val combinedNotes = combineNotes(workout.notes, we.notes)
                for (set in sets) {
                    val weightText =
                        set.weightKg?.let { kg ->
                            Units.formatWeight(
                                weightKg = kg,
                                unit = weightUnit,
                                locale = locale,
                                roundingStep = null,
                                includeUnit = false
                            )
                        }.orEmpty()
                    val repsText = set.reps?.toString().orEmpty()
                    val rpeText = set.rpe?.let { formatCsvDecimal(it, locale) }.orEmpty()
                    val durationText = set.durationS?.toString().orEmpty()
                    val distanceText = set.distanceM?.let { formatCsvDecimal(it, locale) }.orEmpty()
                    val row =
                        listOf(
                            dateStr,
                            workout.name,
                            exerciseName,
                            set.setType.name,
                            weightText,
                            repsText,
                            rpeText,
                            durationText,
                            distanceText,
                            combinedNotes
                        )
                    sb.appendLine(row.joinToString(",") { escapeCsvField(it) })
                }
            }
        }
        return sb.toString()
    }

    internal fun combineNotes(workoutNotes: String, exerciseNotes: String): String {
        val w = workoutNotes.trim()
        val e = exerciseNotes.trim()
        return when {
            w.isEmpty() -> e
            e.isEmpty() -> w
            w == e -> w
            else -> "$w | $e"
        }
    }

    internal fun escapeCsvField(value: String): String {
        val needsQuotes =
            value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun formatCsvDecimal(value: Double, locale: Locale): String = Units.formatWeight(
        weightKg = value,
        unit = WeightUnit.KG,
        locale = locale,
        roundingStep = null,
        includeUnit = false
    )
}
