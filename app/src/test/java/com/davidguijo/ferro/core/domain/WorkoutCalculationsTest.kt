package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.ExerciseHistoryEntry
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutExerciseWithSets
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.model.WorkoutWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCalculationsTest {
    @Test
    fun `volumen y series completadas excluyen calentamiento, incompletas y tipos sin peso x reps`() {
        val workout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-1",
                        name = "Torso",
                        routineId = null,
                        startedAt = 1_000L,
                        endedAt = null,
                        notes = "",
                        status = WorkoutStatus.IN_PROGRESS,
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-1", "ex-bench", 0),
                            sets =
                                listOf(
                                    sampleSet("s-warmup", "we-1", 0, SetType.WARMUP, 60.0, 10, completed = true),
                                    sampleSet("s-normal", "we-1", 1, SetType.NORMAL, 100.0, 5, completed = true),
                                    sampleSet("s-fail", "we-1", 2, SetType.FAILURE, 100.0, 4, completed = true),
                                    sampleSet("s-drop", "we-1", 3, SetType.DROP, 70.0, 8, completed = true),
                                    sampleSet("s-pending", "we-1", 4, SetType.NORMAL, 100.0, 5, completed = false)
                                )
                        ),
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-2", "ex-dips", 1),
                            sets =
                                listOf(
                                    sampleSet("s-weighted", "we-2", 0, SetType.NORMAL, 20.0, 10, completed = true)
                                )
                        ),
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-3", "ex-assisted", 2),
                            sets =
                                listOf(
                                    sampleSet("s-assist", "we-3", 0, SetType.NORMAL, 30.0, 8, completed = true)
                                )
                        ),
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-4", "ex-plank", 3),
                            sets =
                                listOf(
                                    sampleSet(
                                        id = "s-dur",
                                        workoutExerciseId = "we-4",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        weightKg = null,
                                        reps = null,
                                        completed = true,
                                        durationS = 60
                                    )
                                )
                        )
                    )
            )

        val trackingMap =
            mapOf(
                "ex-bench" to TrackingType.WEIGHT_REPS,
                "ex-dips" to TrackingType.WEIGHTED_BODYWEIGHT,
                "ex-assisted" to TrackingType.ASSISTED_BODYWEIGHT,
                "ex-plank" to TrackingType.DURATION
            )

        // 100*5 + 100*4 + 70*8 + 20*10 = 500 + 400 + 560 + 200 = 1660 kg
        assertEquals(1660.0, computeWorkoutVolumeKg(workout, trackingMap), 1e-6)
        assertEquals(7, computeCompletedSetsCount(workout))
        assertEquals(8, computeTotalSetsCount(workout))
        assertEquals(1, computeIncompleteSetsCount(workout))
    }

    @Test
    fun `copiado desde rutina conserva ejercicios, superseries, notas, descansos y placeholders`() {
        val routine =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "r-1",
                        folderId = null,
                        name = "Torso Pesado",
                        notes = "Calentar hombros antes",
                        position = 0,
                        createdAt = 500L,
                        updatedAt = 500L
                    ),
                exercises =
                    listOf(
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-1",
                                    routineId = "r-1",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = "A",
                                    restSeconds = 150,
                                    notes = "Pausa de 1 s en pecho",
                                    createdAt = 500L,
                                    updatedAt = 500L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-1",
                                        routineExerciseId = "re-1",
                                        position = 0,
                                        setType = SetType.WARMUP,
                                        targetWeightKg = 60.0,
                                        targetRepsMin = 8,
                                        targetRepsMax = 8,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 500L,
                                        updatedAt = 500L
                                    ),
                                    RoutineSet(
                                        id = "rs-2",
                                        routineExerciseId = "re-1",
                                        position = 1,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 100.0,
                                        targetRepsMin = 6,
                                        targetRepsMax = 8,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 500L,
                                        updatedAt = 500L
                                    )
                                )
                        ),
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-2",
                                    routineId = "r-1",
                                    exerciseId = "ex-row",
                                    position = 1,
                                    supersetGroup = "A",
                                    restSeconds = 120,
                                    notes = "Espalda neutra",
                                    createdAt = 500L,
                                    updatedAt = 500L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-3",
                                        routineExerciseId = "re-2",
                                        position = 0,
                                        setType = SetType.FAILURE,
                                        targetWeightKg = 80.0,
                                        targetRepsMin = 10,
                                        targetRepsMax = 12,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 500L,
                                        updatedAt = 500L
                                    )
                                )
                        )
                    )
            )

        var counter = 0
        val created =
            createWorkoutFromRoutine(
                routine = routine,
                nowMillis = 50_000L,
                idGenerator = { "gen-${++counter}" }
            )

        assertEquals("Torso Pesado", created.workout.workout.name)
        assertEquals("r-1", created.workout.workout.routineId)
        assertEquals("Calentar hombros antes", created.workout.workout.notes)
        assertEquals(WorkoutStatus.IN_PROGRESS, created.workout.workout.status)
        assertEquals(2, created.workout.exercises.size)

        val ex0 = created.workout.exercises[0]
        val ex1 = created.workout.exercises[1]
        assertEquals("A", ex0.workoutExercise.supersetGroup)
        assertEquals(150, ex0.workoutExercise.restSeconds)
        assertEquals("Pausa de 1 s en pecho", ex0.workoutExercise.notes)
        assertEquals("A", ex1.workoutExercise.supersetGroup)
        assertEquals(120, ex1.workoutExercise.restSeconds)

        // Valores reales comienzan vacíos (null) y objetivos quedan en targetPlaceholdersBySetId
        assertEquals(2, ex0.sets.size)
        assertEquals(SetType.WARMUP, ex0.sets[0].setType)
        assertNull(ex0.sets[0].weightKg)
        assertNull(ex0.sets[0].reps)
        assertFalse(ex0.sets[0].completed)

        val ph1 = created.targetPlaceholdersBySetId[ex0.sets[1].id]
        assertNotNull(ph1)
        assertEquals(100.0, ph1?.targetWeightKg ?: 0.0, 1e-6)
        assertEquals(6, ph1?.targetRepsMin)
        assertEquals(8, ph1?.targetRepsMax)

        // Reconstrucción tras muerte del proceso
        val rebuilt = rebuildTargetPlaceholders(created.workout, routine)
        assertEquals(created.targetPlaceholdersBySetId, rebuilt)
    }

    @Test
    fun `valores anterior usan solo series completadas de la ultima sesion`() {
        val history =
            ExerciseHistoryEntry(
                workoutId = "w-prev",
                workoutName = "Torso anterior",
                workoutStartedAt = 10_000L,
                workoutEndedAt = 12_000L,
                workoutExercise = sampleWorkoutExercise("we-prev", "ex-bench", 0),
                sets =
                    listOf(
                        sampleSet("p-0", "we-prev", 0, SetType.WARMUP, 60.0, 10, completed = true),
                        sampleSet("p-1", "we-prev", 1, SetType.NORMAL, 100.0, 5, completed = true),
                        sampleSet("p-2", "we-prev", 2, SetType.NORMAL, 105.0, 3, completed = false)
                    )
            )

        val resolved = resolvePreviousSetsForExercise(lastSession = history, currentSetCount = 3)
        assertEquals(3, resolved.size)
        assertEquals(60.0, resolved[0]?.weightKg ?: 0.0, 1e-6)
        assertEquals(100.0, resolved[1]?.weightKg ?: 0.0, 1e-6)
        assertNull(resolved[2])

        assertEquals(listOf<PreviousSetSnapshot?>(null, null), resolvePreviousSetsForExercise(null, 2))
    }

    @Test
    fun `en superseries el descanso solo arranca en el ultimo ejercicio del grupo`() {
        val exercises =
            listOf(
                sampleWorkoutExercise("we-1", "ex-1", 0, supersetGroup = "A"),
                sampleWorkoutExercise("we-2", "ex-2", 1, supersetGroup = "A"),
                sampleWorkoutExercise("we-3", "ex-3", 2, supersetGroup = null)
            )

        assertFalse(shouldStartRestTimer(exercises, "we-1"))
        assertTrue(shouldStartRestTimer(exercises, "we-2"))
        assertTrue(shouldStartRestTimer(exercises, "we-3"))
    }

    @Test
    fun `terminar con series incompletas descarta o marca segun la estrategia`() {
        val workout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-1",
                        name = "Push",
                        routineId = "r-1",
                        startedAt = 1_000L,
                        endedAt = null,
                        notes = "",
                        status = WorkoutStatus.IN_PROGRESS,
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-1", "ex-1", 0, supersetGroup = "A"),
                            sets =
                                listOf(
                                    sampleSet("s-1", "we-1", 0, SetType.NORMAL, 80.0, 8, completed = true),
                                    sampleSet("s-2", "we-1", 1, SetType.NORMAL, null, null, completed = false)
                                )
                        ),
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-2", "ex-2", 1, supersetGroup = "A"),
                            sets =
                                listOf(
                                    sampleSet("s-3", "we-2", 0, SetType.NORMAL, null, null, completed = false)
                                )
                        )
                    )
            )
        val placeholders =
            mapOf(
                "s-2" to SetTargetPlaceholder(targetWeightKg = 82.5, targetRepsMin = 8, targetRepsMax = 10),
                "s-3" to SetTargetPlaceholder(targetWeightKg = 40.0, targetRepsMin = 12, targetRepsMax = 12)
            )

        val discarded =
            prepareWorkoutForCompletion(
                workout = workout,
                incompleteStrategy = IncompleteSetsStrategy.DISCARD_INCOMPLETE,
                targetPlaceholdersBySetId = placeholders,
                endedAtMillis = 9_000L
            )
        assertEquals(WorkoutStatus.COMPLETED, discarded.workout.status)
        assertEquals(9_000L, discarded.workout.endedAt)
        assertEquals(1, discarded.exercises.size)
        assertEquals(1, discarded.exercises[0].sets.size)
        assertNull(discarded.exercises[0].workoutExercise.supersetGroup)

        val markedAll =
            prepareWorkoutForCompletion(
                workout = workout,
                incompleteStrategy = IncompleteSetsStrategy.MARK_ALL_COMPLETED,
                targetPlaceholdersBySetId = placeholders,
                endedAtMillis = 9_000L
            )
        assertEquals(2, markedAll.exercises.size)
        val filledS2 = markedAll.exercises[0].sets[1]
        assertTrue(filledS2.completed)
        assertEquals(82.5, filledS2.weightKg ?: 0.0, 1e-6)
        assertEquals(8, filledS2.reps)
    }

    @Test
    fun `comparacion con la ultima sesion de la rutina y actualizacion de valores de rutina`() {
        val routine =
            RoutineWithExercises(
                routine =
                    Routine(
                        id = "r-1",
                        folderId = null,
                        name = "Push A",
                        notes = "",
                        position = 0,
                        createdAt = 1_000L,
                        updatedAt = 1_000L
                    ),
                exercises =
                    listOf(
                        RoutineExerciseWithSets(
                            routineExercise =
                                RoutineExercise(
                                    id = "re-1",
                                    routineId = "r-1",
                                    exerciseId = "ex-bench",
                                    position = 0,
                                    supersetGroup = null,
                                    restSeconds = 90,
                                    notes = "",
                                    createdAt = 1_000L,
                                    updatedAt = 1_000L
                                ),
                            sets =
                                listOf(
                                    RoutineSet(
                                        id = "rs-1",
                                        routineExerciseId = "re-1",
                                        position = 0,
                                        setType = SetType.NORMAL,
                                        targetWeightKg = 80.0,
                                        targetRepsMin = 8,
                                        targetRepsMax = 8,
                                        targetDurationS = null,
                                        targetDistanceM = null,
                                        createdAt = 1_000L,
                                        updatedAt = 1_000L
                                    )
                                )
                        )
                    )
            )

        val prevWorkout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-prev",
                        name = "Push A",
                        routineId = "r-1",
                        startedAt = 10_000L,
                        endedAt = 13_000L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 10_000L,
                        updatedAt = 13_000L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-prev", "ex-bench", 0),
                            sets =
                                listOf(
                                    sampleSet("sp-1", "we-prev", 0, SetType.NORMAL, 80.0, 8, completed = true)
                                )
                        )
                    )
            )

        // Si el entreno tiene exactamente los mismos valores que la rutina, no ofrece actualizar
        assertFalse(hasWorkoutRoutineValuesChanged(prevWorkout, routine))

        val currentWorkout =
            WorkoutWithExercises(
                workout =
                    Workout(
                        id = "w-curr",
                        name = "Push A",
                        routineId = "r-1",
                        startedAt = 20_000L,
                        endedAt = 23_600L,
                        notes = "",
                        status = WorkoutStatus.COMPLETED,
                        createdAt = 20_000L,
                        updatedAt = 23_600L
                    ),
                exercises =
                    listOf(
                        WorkoutExerciseWithSets(
                            workoutExercise = sampleWorkoutExercise("we-curr", "ex-bench", 0),
                            sets =
                                listOf(
                                    sampleSet("sc-1", "we-curr", 0, SetType.NORMAL, 85.0, 8, completed = true),
                                    sampleSet("sc-2", "we-curr", 1, SetType.NORMAL, 85.0, 6, completed = true)
                                )
                        )
                    )
            )

        val comparison =
            computeRoutineComparison(
                currentWorkout = currentWorkout,
                previousWorkout = prevWorkout,
                trackingTypesByExerciseId = mapOf("ex-bench" to TrackingType.WEIGHT_REPS)
            )
        assertEquals(600L, comparison.durationDeltaMillis)
        assertEquals((85.0 * 8 + 85.0 * 6) - (80.0 * 8), comparison.volumeDeltaKg, 1e-6)
        assertEquals(1, comparison.completedSetsDelta)

        assertTrue(hasWorkoutRoutineValuesChanged(currentWorkout, routine))

        val updatedRoutine =
            applyWorkoutValuesToRoutine(
                routine = routine,
                workout = currentWorkout,
                nowMillis = 30_000L
            )
        assertEquals(2, updatedRoutine.exercises.first().sets.size)
        assertEquals(85.0, updatedRoutine.exercises.first().sets[0].targetWeightKg ?: 0.0, 1e-6)
        assertEquals(8, updatedRoutine.exercises.first().sets[0].targetRepsMin)
        assertEquals(6, updatedRoutine.exercises.first().sets[1].targetRepsMin)
        assertFalse(hasWorkoutRoutineValuesChanged(currentWorkout, updatedRoutine))
    }

    private fun sampleWorkoutExercise(id: String, exerciseId: String, position: Int, supersetGroup: String? = null) =
        WorkoutExercise(
            id = id,
            workoutId = "w-1",
            exerciseId = exerciseId,
            position = position,
            supersetGroup = supersetGroup,
            restSeconds = 90,
            notes = "",
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

    private fun sampleSet(
        id: String,
        workoutExerciseId: String,
        position: Int,
        setType: SetType,
        weightKg: Double?,
        reps: Int?,
        completed: Boolean,
        durationS: Int? = null
    ) = WorkoutSet(
        id = id,
        workoutExerciseId = workoutExerciseId,
        position = position,
        setType = setType,
        weightKg = weightKg,
        reps = reps,
        rpe = null,
        durationS = durationS,
        distanceM = null,
        completed = completed,
        completedAt = if (completed) 2_000L else null,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )
}
