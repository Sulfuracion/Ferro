package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineOrderingTest {
    @Test
    fun `moveRoutineInFolder reordena arriba y abajo normalizando posiciones`() {
        val routines =
            listOf(
                sampleRoutine(id = "r1", folderId = "f1", name = "A", position = 0),
                sampleRoutine(id = "r2", folderId = "f1", name = "B", position = 1),
                sampleRoutine(id = "r3", folderId = "f1", name = "C", position = 2)
            )

        val movedDown =
            moveRoutineInFolder(
                routinesInFolder = routines,
                routineId = "r1",
                direction = MoveDirection.DOWN,
                updatedAt = 5_000L
            )
        assertEquals(listOf("r2", "r1", "r3"), movedDown.map { it.id })
        assertEquals(listOf(0, 1, 2), movedDown.map { it.position })

        val movedUp =
            moveRoutineInFolder(
                routinesInFolder = movedDown,
                routineId = "r3",
                direction = MoveDirection.UP,
                updatedAt = 6_000L
            )
        assertEquals(listOf("r2", "r3", "r1"), movedUp.map { it.id })
        assertEquals(listOf(0, 1, 2), movedUp.map { it.position })

        // En el borde superior o inferior no cambia el orden
        val atTop =
            moveRoutineInFolder(
                routinesInFolder = movedUp,
                routineId = "r2",
                direction = MoveDirection.UP,
                updatedAt = 7_000L
            )
        assertEquals(listOf("r2", "r3", "r1"), atTop.map { it.id })
        val atBottom =
            moveRoutineInFolder(
                routinesInFolder = movedUp,
                routineId = "r1",
                direction = MoveDirection.DOWN,
                updatedAt = 7_000L
            )
        assertEquals(listOf("r2", "r3", "r1"), atBottom.map { it.id })
    }

    @Test
    fun `moveRoutineToFolder mueve rutina entre carpetas y reindexa origen y destino`() {
        val allRoutines =
            listOf(
                sampleRoutine(id = "r1", folderId = "f1", name = "Torso A", position = 0),
                sampleRoutine(id = "r2", folderId = "f1", name = "Torso B", position = 1),
                sampleRoutine(id = "r3", folderId = "f2", name = "Pierna A", position = 0)
            )

        val changed =
            moveRoutineToFolder(
                allRoutines = allRoutines,
                routineId = "r1",
                targetFolderId = "f2",
                updatedAt = 9_000L
            )

        val byId = (allRoutines.associateBy { it.id } + changed.associateBy { it.id })
        assertEquals("f1", byId.getValue("r2").folderId)
        assertEquals(0, byId.getValue("r2").position)

        assertEquals("f2", byId.getValue("r3").folderId)
        assertEquals(0, byId.getValue("r3").position)

        assertEquals("f2", byId.getValue("r1").folderId)
        assertEquals(1, byId.getValue("r1").position)
    }

    @Test
    fun `canGroupAsSuperset exige dos o más ejercicios consecutivos`() {
        val ids = listOf("e1", "e2", "e3", "e4")

        assertFalse(canGroupAsSuperset(ids, emptySet()))
        assertFalse(canGroupAsSuperset(ids, setOf("e1")))
        assertFalse(canGroupAsSuperset(ids, setOf("e1", "e3")))
        assertFalse(canGroupAsSuperset(ids, setOf("e1", "e2", "e4")))
        assertFalse(canGroupAsSuperset(ids, setOf("e1", "inexistente")))

        assertTrue(canGroupAsSuperset(ids, setOf("e1", "e2")))
        assertTrue(canGroupAsSuperset(ids, setOf("e2", "e3", "e4")))
        assertTrue(canGroupAsSuperset(ids, setOf("e1", "e2", "e3", "e4")))
    }

    @Test
    fun `agrupar y desagrupar superseries asigna etiquetas canonicas y limpia huerfanos`() {
        val initial =
            listOf(
                sampleExerciseWithSets(id = "e1", position = 0),
                sampleExerciseWithSets(id = "e2", position = 1),
                sampleExerciseWithSets(id = "e3", position = 2),
                sampleExerciseWithSets(id = "e4", position = 3)
            )

        // Agrupar e1 y e2 -> Superserie A
        val firstGroup = groupRoutineExercisesAsSuperset(initial, setOf("e1", "e2"), updatedAt = 2_000L)
        assertEquals(listOf("A", "A", null, null), firstGroup.map { it.routineExercise.supersetGroup })

        // Agrupar e3 y e4 -> Superserie B
        val twoGroups = groupRoutineExercisesAsSuperset(firstGroup, setOf("e3", "e4"), updatedAt = 3_000L)
        assertEquals(listOf("A", "A", "B", "B"), twoGroups.map { it.routineExercise.supersetGroup })

        // Si agrupamos e2 y e3 (solapando A y B), e1 y e4 quedan solos y pierden su superserie
        val overlapped = groupRoutineExercisesAsSuperset(twoGroups, setOf("e2", "e3"), updatedAt = 4_000L)
        assertEquals(listOf(null, "A", "A", null), overlapped.map { it.routineExercise.supersetGroup })

        // Desagrupar A deja todos en null
        val ungrouped = ungroupRoutineSuperset(overlapped, "A", updatedAt = 5_000L)
        assertEquals(listOf(null, null, null, null), ungrouped.map { it.routineExercise.supersetGroup })
    }

    @Test
    fun `reordenar ejercicios mantiene superserie si siguen contiguos y la rompe si se separan`() {
        val grouped =
            listOf(
                sampleExerciseWithSets(id = "e1", position = 0, supersetGroup = "A"),
                sampleExerciseWithSets(id = "e2", position = 1, supersetGroup = "A"),
                sampleExerciseWithSets(id = "e3", position = 2, supersetGroup = null)
            )

        // Intercambiar e1 y e2 dentro de la misma superserie mantiene la superserie A
        val swappedInside = moveRoutineExercise(grouped, fromIndex = 0, toIndex = 1, updatedAt = 2_000L)
        assertEquals(listOf("e2", "e1", "e3"), swappedInside.map { it.routineExercise.id })
        assertEquals(listOf("A", "A", null), swappedInside.map { it.routineExercise.supersetGroup })

        // Mover e1 abajo del todo separa a e2 y e1 (con e3 en medio), deshaciendo la superserie de 2
        val separated = moveRoutineExercise(swappedInside, fromIndex = 1, toIndex = 2, updatedAt = 3_000L)
        assertEquals(listOf("e2", "e3", "e1"), separated.map { it.routineExercise.id })
        assertNull(separated[0].routineExercise.supersetGroup)
        assertNull(separated[1].routineExercise.supersetGroup)
        assertNull(separated[2].routineExercise.supersetGroup)
    }

    @Test
    fun `separar un ejercicio de una superserie de tres conserva los otros dos agrupados`() {
        val triSet =
            listOf(
                sampleExerciseWithSets(id = "e1", position = 0, supersetGroup = "A"),
                sampleExerciseWithSets(id = "e2", position = 1, supersetGroup = "A"),
                sampleExerciseWithSets(id = "e3", position = 2, supersetGroup = "A"),
                sampleExerciseWithSets(id = "e4", position = 3, supersetGroup = null)
            )

        val movedOut = moveRoutineExercise(triSet, fromIndex = 2, toIndex = 3, updatedAt = 4_000L)
        assertEquals(listOf("e1", "e2", "e4", "e3"), movedOut.map { it.routineExercise.id })
        assertEquals(listOf("A", "A", null, null), movedOut.map { it.routineExercise.supersetGroup })
    }

    @Test
    fun `computeNormalSetNumbers numera solo las series normales`() {
        val types =
            listOf(
                SetType.WARMUP,
                SetType.NORMAL,
                SetType.NORMAL,
                SetType.FAILURE,
                SetType.NORMAL,
                SetType.DROP
            )
        val numbers = computeNormalSetNumbers(types)
        assertEquals(1, numbers[1])
        assertEquals(2, numbers[2])
        assertEquals(3, numbers[4])
    }

    private fun sampleRoutine(id: String, folderId: String?, name: String, position: Int): Routine = Routine(
        id = id,
        folderId = folderId,
        name = name,
        notes = "",
        position = position,
        createdAt = 1_000L,
        updatedAt = 1_000L
    )

    private fun sampleExerciseWithSets(
        id: String,
        position: Int,
        supersetGroup: String? = null
    ): RoutineExerciseWithSets = RoutineExerciseWithSets(
        routineExercise =
            RoutineExercise(
                id = id,
                routineId = "r1",
                exerciseId = "ex-$id",
                position = position,
                supersetGroup = supersetGroup,
                restSeconds = 90,
                notes = "",
                createdAt = 1_000L,
                updatedAt = 1_000L
            ),
        sets = emptyList()
    )
}
