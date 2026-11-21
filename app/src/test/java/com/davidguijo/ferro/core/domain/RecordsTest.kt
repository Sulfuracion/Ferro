package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsTest {
    @Test
    fun `epley 1RM devuelve el propio peso con 1 rep, aplica formula de 2 a 12 y no estima por encima de 12`() {
        assertEquals(100.0, Records.estimateOneRepMaxEpley(100.0, 1) ?: 0.0, 1e-6)
        assertEquals(100.0 * (1.0 + 5.0 / 30.0), Records.estimateOneRepMaxEpley(100.0, 5) ?: 0.0, 1e-6)
        assertEquals(140.0, Records.estimateOneRepMaxEpley(100.0, 12) ?: 0.0, 1e-6)

        // Límite superior (> 12 reps) y entradas no válidas
        assertNull(Records.estimateOneRepMaxEpley(100.0, 13))
        assertNull(Records.estimateOneRepMaxEpley(100.0, 20))
        assertNull(Records.estimateOneRepMaxEpley(100.0, 0))
        assertNull(Records.estimateOneRepMaxEpley(100.0, -3))
        assertNull(Records.estimateOneRepMaxEpley(0.0, 5))
        assertNull(Records.estimateOneRepMaxEpley(-50.0, 5))
        assertNull(Records.estimateOneRepMaxEpley(Double.NaN, 5))
        assertNull(Records.estimateOneRepMaxEpley(Double.POSITIVE_INFINITY, 5))
    }

    @Test
    fun `ignora calentamientos y series no completadas en todos los calculos`() {
        val sets =
            listOf(
                workoutSet(id = "s-warm", setType = SetType.WARMUP, weightKg = 140.0, reps = 5, completed = true),
                workoutSet(id = "s-inc", setType = SetType.NORMAL, weightKg = 150.0, reps = 5, completed = false),
                workoutSet(id = "s-valid", setType = SetType.NORMAL, weightKg = 100.0, reps = 5, completed = true)
            )

        assertEquals(100.0, Records.computeBestMaxWeight(sets, TrackingType.WEIGHT_REPS) ?: 0.0, 1e-6)
        assertEquals(
            100.0 * (1.0 + 5.0 / 30.0),
            Records.computeBestEstimated1Rm(sets, TrackingType.WEIGHT_REPS) ?: 0.0,
            1e-6
        )
        assertEquals(500.0, Records.computeBestSetVolume(sets, TrackingType.WEIGHT_REPS) ?: 0.0, 1e-6)
        assertEquals(5, Records.computeMaxRepsAtWeight(sets, TrackingType.WEIGHT_REPS, 100.0))
        assertNull(Records.computeMaxRepsAtWeight(sets, TrackingType.WEIGHT_REPS, 140.0))

        // Si solo hay calentamientos o incompletas no se detecta ningún récord
        val onlyWarmupAndIncomplete = sets.take(2)
        val broken =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = onlyWarmupAndIncomplete,
                historicalSets = emptyList(),
                achievedAtMillis = 10_000L
            )
        assertTrue(broken.isEmpty())
    }

    @Test
    fun `empates contra el historial no baten record y empates dentro de la misma sesion eligen la primera serie`() {
        val historical =
            listOf(
                workoutSet(id = "h-1", weightKg = 100.0, reps = 5, completed = true)
            )

        // Sesión actual con exactamente el mismo peso y reps (empate total)
        val tieSession =
            listOf(
                workoutSet(id = "c-1", weightKg = 100.0, reps = 5, completed = true)
            )
        val brokenOnTie =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = tieSession,
                historicalSets = historical,
                achievedAtMillis = 20_000L
            )
        assertTrue(brokenOnTie.isEmpty())

        // Dos series idénticas dentro de la misma sesión que sí superan el histórico: gana la primera
        val doubleRecordSession =
            listOf(
                workoutSet(id = "c-first", position = 0, weightKg = 105.0, reps = 5, completed = true),
                workoutSet(id = "c-second", position = 1, weightKg = 105.0, reps = 5, completed = true)
            )
        val brokenDouble =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = doubleRecordSession,
                historicalSets = historical,
                achievedAtMillis = 25_000L
            )
        assertEquals(4, brokenDouble.size)
        assertTrue(brokenDouble.all { it.set.id == "c-first" })
    }

    @Test
    fun `series con mas de 12 reps pueden batir peso, volumen y reps a ese peso pero nunca 1RM estimado`() {
        val historical =
            listOf(
                workoutSet(id = "h-1", weightKg = 80.0, reps = 10, completed = true)
            )
        // >12 reps -> sin 1RM estimado, pero vol = 1275 y peso = 85
        val current =
            listOf(
                workoutSet(id = "c-high-reps", weightKg = 85.0, reps = 15, completed = true)
            )

        val broken =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-squat",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = current,
                historicalSets = historical,
                achievedAtMillis = 30_000L
            )

        val types = broken.map { it.record.recordType }.toSet()
        assertEquals(
            setOf(RecordType.MAX_WEIGHT, RecordType.BEST_SET_VOLUME, RecordType.MAX_REPS_AT_WEIGHT),
            types
        )
    }

    @Test
    fun `cambio de unidades kg y lb mantiene equivalencia exacta en peso maximo y mas reps a un peso dado`() {
        val weightFromKg = 100.0
        // Usuario cambia la app a lb, ve ~220.462262 lb, o introduce 225 lb convertidos a kg y luego vuelve a kg
        val exactLb = Units.kgToLb(weightFromKg)
        val roundTripKg = Units.lbToKg(exactLb)

        val historical =
            listOf(
                workoutSet(id = "h-kg", weightKg = weightFromKg, reps = 5, completed = true)
            )

        // Misma carga tras ida y vuelta kg -> lb -> kg con las mismas 5 reps: empate, no debe dar falso récord
        val sameInLbSession =
            listOf(
                workoutSet(id = "c-lb-tie", weightKg = roundTripKg, reps = 5, completed = true)
            )
        assertTrue(
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = sameInLbSession,
                historicalSets = historical,
                achievedAtMillis = 40_000L
            ).isEmpty()
        )

        // Misma carga tras ida y vuelta kg -> lb -> kg pero con 6 reps:
        // bate ESTIMATED_1RM, BEST_SET_VOLUME y MAX_REPS_AT_WEIGHT (no MAX_WEIGHT)
        val moreRepsInLbSession =
            listOf(
                workoutSet(id = "c-lb-more", weightKg = roundTripKg, reps = 6, completed = true)
            )
        val broken =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                currentSets = moreRepsInLbSession,
                historicalSets = historical,
                achievedAtMillis = 45_000L
            )
        val brokenTypes = broken.map { it.record.recordType }.toSet()
        assertEquals(
            setOf(RecordType.ESTIMATED_1RM, RecordType.BEST_SET_VOLUME, RecordType.MAX_REPS_AT_WEIGHT),
            brokenTypes
        )
        val maxRepsRecord = broken.first { it.record.recordType == RecordType.MAX_REPS_AT_WEIGHT }
        assertEquals(6.0, maxRepsRecord.record.value, 1e-6)
        assertEquals(5.0, maxRepsRecord.previousValue ?: 0.0, 1e-6)
    }

    @Test
    fun `peso corporal lastrado cuenta el lastre y peso corporal puro premia maximas repeticiones`() {
        val weightedHistory =
            listOf(
                workoutSet(id = "h-w1", weightKg = 20.0, reps = 6, completed = true)
            )
        val weightedCurrent =
            listOf(
                workoutSet(id = "c-w1", weightKg = 25.0, reps = 5, completed = true)
            )
        val brokenWeighted =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-weighted-dips",
                trackingType = TrackingType.WEIGHTED_BODYWEIGHT,
                currentSets = weightedCurrent,
                historicalSets = weightedHistory,
                achievedAtMillis = 50_000L
            )
        val maxWeightPr = brokenWeighted.firstOrNull { it.record.recordType == RecordType.MAX_WEIGHT }
        assertNotNull(maxWeightPr)
        assertEquals(25.0, maxWeightPr?.record?.value ?: 0.0, 1e-6)
        assertEquals(20.0, maxWeightPr?.previousValue ?: 0.0, 1e-6)

        // Bodyweight puro (dominadas sin lastre): solo aplica MAX_REPS_AT_WEIGHT
        val bwHistory =
            listOf(
                workoutSet(id = "h-bw", weightKg = null, reps = 12, completed = true)
            )
        val bwCurrent =
            listOf(
                workoutSet(id = "c-bw", weightKg = null, reps = 15, completed = true)
            )
        val brokenBw =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-pullups",
                trackingType = TrackingType.BODYWEIGHT_REPS,
                currentSets = bwCurrent,
                historicalSets = bwHistory,
                achievedAtMillis = 55_000L
            )
        assertEquals(1, brokenBw.size)
        assertEquals(RecordType.MAX_REPS_AT_WEIGHT, brokenBw.first().record.recordType)
        assertEquals(15.0, brokenBw.first().record.value, 1e-6)
    }

    @Test
    fun `en peso corporal asistido menos asistencia es mejor y mas asistencia no bate record`() {
        val history =
            listOf(
                workoutSet(id = "h-a1", weightKg = 25.0, reps = 8, completed = true)
            )

        // Más asistencia (30 kg de ayuda con 8 reps): es más fácil, NO debe batir ningún récord
        val easierSession =
            listOf(
                workoutSet(id = "c-easier", weightKg = 30.0, reps = 8, completed = true)
            )
        assertTrue(
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-assisted-pullup",
                trackingType = TrackingType.ASSISTED_BODYWEIGHT,
                currentSets = easierSession,
                historicalSets = history,
                achievedAtMillis = 60_000L
            ).isEmpty()
        )

        // Menos asistencia (15 kg de ayuda con 8 reps): bate récord de mínima asistencia (MAX_WEIGHT)
        val harderSession =
            listOf(
                workoutSet(id = "c-harder", weightKg = 15.0, reps = 8, completed = true)
            )
        val brokenHarder =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-assisted-pullup",
                trackingType = TrackingType.ASSISTED_BODYWEIGHT,
                currentSets = harderSession,
                historicalSets = history,
                achievedAtMillis = 65_000L
            )
        val minAssistPr = brokenHarder.firstOrNull { it.record.recordType == RecordType.MAX_WEIGHT }
        assertNotNull(minAssistPr)
        assertEquals(15.0, minAssistPr?.record?.value ?: -1.0, 1e-6)
        assertEquals(25.0, minAssistPr?.previousValue ?: -1.0, 1e-6)

        // Misma asistencia (25 kg) pero más repeticiones (10 reps vs 8): bate MAX_REPS_AT_WEIGHT
        val moreRepsSameAssist =
            listOf(
                workoutSet(id = "c-more-reps", weightKg = 25.0, reps = 10, completed = true)
            )
        val brokenMoreReps =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-assisted-pullup",
                trackingType = TrackingType.ASSISTED_BODYWEIGHT,
                currentSets = moreRepsSameAssist,
                historicalSets = history + harderSession,
                achievedAtMillis = 70_000L
            )
        assertEquals(1, brokenMoreReps.size)
        assertEquals(RecordType.MAX_REPS_AT_WEIGHT, brokenMoreReps.first().record.recordType)
        assertEquals(10.0, brokenMoreReps.first().record.value, 1e-6)
    }

    @Test
    fun `duracion y distancia tienen sus propios records`() {
        // Plancha (DURATION): mejor duración en BEST_SET_VOLUME
        val plankHistory =
            listOf(
                workoutSet(id = "h-p1", weightKg = null, reps = null, durationS = 60, completed = true)
            )
        val plankCurrent =
            listOf(
                workoutSet(id = "c-p1", weightKg = null, reps = null, durationS = 90, completed = true)
            )
        val brokenPlank =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-plank",
                trackingType = TrackingType.DURATION,
                currentSets = plankCurrent,
                historicalSets = plankHistory,
                achievedAtMillis = 80_000L
            )
        assertEquals(1, brokenPlank.size)
        assertEquals(RecordType.BEST_SET_VOLUME, brokenPlank.first().record.recordType)
        assertEquals(90.0, brokenPlank.first().record.value, 1e-6)

        // Remo / Carrera (DISTANCE_DURATION): mayor distancia en MAX_WEIGHT y mayor duración en BEST_SET_VOLUME
        val cardioHistory =
            listOf(
                workoutSet(
                    id = "h-c1",
                    weightKg = null,
                    reps = null,
                    distanceM = 1000.0,
                    durationS = 240,
                    completed = true
                )
            )
        val cardioCurrent =
            listOf(
                workoutSet(
                    id = "c-c1",
                    weightKg = null,
                    reps = null,
                    distanceM = 1500.0,
                    durationS = 350,
                    completed = true
                )
            )
        val brokenCardio =
            Records.detectNewRecordsForExercise(
                exerciseId = "ex-rower",
                trackingType = TrackingType.DISTANCE_DURATION,
                currentSets = cardioCurrent,
                historicalSets = cardioHistory,
                achievedAtMillis = 85_000L
            )
        assertEquals(
            mapOf(
                RecordType.MAX_WEIGHT to 1500.0,
                RecordType.BEST_SET_VOLUME to 350.0
            ),
            brokenCardio.associate { it.record.recordType to it.record.value }
        )
    }

    @Test
    fun `recalcular records reconstruye la cronologia al editar o borrar una sesion pasada`() {
        val session1Set =
            workoutSet(
                id = "s1",
                workoutExerciseId = "we-1",
                weightKg = 100.0,
                reps = 5,
                completed = true
            )
        val session2Set =
            workoutSet(
                id = "s2",
                workoutExerciseId = "we-2",
                weightKg = 110.0,
                reps = 5,
                completed = true
            )
        val session3Set =
            workoutSet(
                id = "s3",
                workoutExerciseId = "we-3",
                weightKg = 105.0,
                reps = 5,
                completed = true
            )

        val s1 =
            ExerciseHistoryEntry(
                workoutId = "w-1",
                workoutName = "Día 1",
                workoutStartedAt = 10_000L,
                workoutEndedAt = 12_000L,
                workoutExercise = sampleWorkoutExercise("we-1", "w-1", "ex-bench"),
                sets = listOf(session1Set)
            )
        val s2 =
            ExerciseHistoryEntry(
                workoutId = "w-2",
                workoutName = "Día 2",
                workoutStartedAt = 20_000L,
                workoutEndedAt = 22_000L,
                workoutExercise = sampleWorkoutExercise("we-2", "w-2", "ex-bench"),
                sets = listOf(session2Set)
            )
        val s3 =
            ExerciseHistoryEntry(
                workoutId = "w-3",
                workoutName = "Día 3",
                workoutStartedAt = 30_000L,
                workoutEndedAt = 32_000L,
                workoutExercise = sampleWorkoutExercise("we-3", "w-3", "ex-bench"),
                sets = listOf(session3Set)
            )

        // Con las 3 sesiones: s1 inaugura récords (100x5), s2 los bate (110x5), s3 (105x5) no bate nada
        val fullHistoryRecords =
            Records.recalculateRecordsForExerciseHistory(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                sessions = listOf(s3, s1, s2) // desordenados a propósito para verificar ordenación cronológica
            )
        assertTrue(fullHistoryRecords.none { it.workoutSetId == "s3" })
        assertTrue(fullHistoryRecords.any { it.workoutSetId == "s2" && it.recordType == RecordType.MAX_WEIGHT })

        // Si se borra la sesión 2 (110x5), al recalcular ahora la sesión 3 (105x5) SÍ pasa a ser récord
        val afterDeletingSession2 =
            Records.recalculateRecordsForExerciseHistory(
                exerciseId = "ex-bench",
                trackingType = TrackingType.WEIGHT_REPS,
                sessions = listOf(s1, s3)
            )
        val maxWeightInS3 =
            afterDeletingSession2.firstOrNull {
                it.workoutSetId == "s3" && it.recordType == RecordType.MAX_WEIGHT
            }
        assertNotNull(maxWeightInS3)
        assertEquals(105.0, maxWeightInS3?.value ?: 0.0, 1e-6)
    }

    @Test
    fun `detectNewRecordsForWorkout unifica bloques del mismo ejercicio en la sesion`() {
        val workout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-now",
                        name = "Fuerza",
                        routineId = null,
                        startedAt = 50_000L,
                        endedAt = 55_000L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 50_000L,
                        updatedAt = 55_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-a", "w-now", "ex-bench", position = 0),
                            sets = listOf(workoutSet(id = "s-a", weightKg = 100.0, reps = 5, completed = true))
                        ),
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-b", "w-now", "ex-bench", position = 1),
                            sets = listOf(workoutSet(id = "s-b", weightKg = 102.5, reps = 5, completed = true))
                        )
                    )
            )

        val broken =
            Records.detectNewRecordsForWorkout(
                completedWorkout = workout,
                trackingTypesByExerciseId = mapOf("ex-bench" to TrackingType.WEIGHT_REPS),
                historicalSetsByExerciseId =
                    mapOf(
                        "ex-bench" to listOf(workoutSet(id = "h-1", weightKg = 95.0, reps = 5, completed = true))
                    )
            )

        val maxWeightRecord = broken.first { it.record.recordType == RecordType.MAX_WEIGHT }
        assertEquals("s-b", maxWeightRecord.set.id)
        assertEquals(102.5, maxWeightRecord.record.value, 1e-6)

        val groupedBySet = Records.groupRecordTypesBySetId(broken.map { it.record })
        assertTrue(RecordType.MAX_WEIGHT in groupedBySet["s-b"].orEmpty())
    }

    private fun sampleWorkoutExercise(id: String, workoutId: String, exerciseId: String, position: Int = 0) =
        WorkoutExercise(
            id = id,
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = position,
            supersetGroup = null,
            restSeconds = 90,
            notes = "",
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

    private fun workoutSet(
        id: String,
        workoutExerciseId: String = "we-1",
        position: Int = 0,
        setType: SetType = SetType.NORMAL,
        weightKg: Double? = null,
        reps: Int? = null,
        durationS: Int? = null,
        distanceM: Double? = null,
        completed: Boolean = true
    ) = WorkoutSet(
        id = id,
        workoutExerciseId = workoutExerciseId,
        position = position,
        setType = setType,
        weightKg = weightKg,
        reps = reps,
        rpe = null,
        durationS = durationS,
        distanceM = distanceM,
        completed = completed,
        completedAt = if (completed) 2_000L else null,
        createdAt = 1_000L,
        updatedAt = 2_000L
    )
}
