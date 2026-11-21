package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineExerciseWithSets
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.RoutineTemplate
import com.davidguijo.ferro.core.domain.model.RoutineWithExercises
import com.davidguijo.ferro.core.domain.model.SetType
import java.util.UUID

/** Dirección de movimiento al reordenar elementos con subir/bajar. */
enum class MoveDirection {
    UP,
    DOWN
}

/**
 * Reordena una rutina dentro de su carpeta (o de la lista sin carpeta) moviéndola una posición
 * hacia arriba o hacia abajo y normalizando `position = 0..n-1`.
 */
fun moveRoutineInFolder(
    routinesInFolder: List<Routine>,
    routineId: String,
    direction: MoveDirection,
    updatedAt: Long
): List<Routine> {
    val ordered = routinesInFolder.sortedWith(compareBy<Routine> { it.position }.thenBy { it.name.lowercase() })
    val currentIndex = ordered.indexOfFirst { it.id == routineId }
    if (currentIndex < 0) return ordered
    val targetIndex =
        when (direction) {
            MoveDirection.UP -> currentIndex - 1
            MoveDirection.DOWN -> currentIndex + 1
        }
    if (targetIndex !in ordered.indices) return ordered

    val mutable = ordered.toMutableList()
    val item = mutable.removeAt(currentIndex)
    mutable.add(targetIndex, item)
    return mutable.mapIndexed { index, routine ->
        if (routine.position != index || routine.id == routineId) {
            routine.copy(position = index, updatedAt = updatedAt)
        } else {
            routine
        }
    }
}

/**
 * Mueve una rutina a otra carpeta (o a `null` para dejarla sin carpeta), colocándola al final
 * del grupo de destino y reindexando las posiciones de origen y destino.
 * Devuelve únicamente las rutinas que cambiaron de carpeta o de posición.
 */
fun moveRoutineToFolder(
    allRoutines: List<Routine>,
    routineId: String,
    targetFolderId: String?,
    updatedAt: Long
): List<Routine> {
    val routine = allRoutines.firstOrNull { it.id == routineId } ?: return emptyList()
    val sourceFolderId = routine.folderId
    if (sourceFolderId == targetFolderId) return emptyList()

    val remainingInSource =
        allRoutines
            .filter { it.folderId == sourceFolderId && it.id != routineId }
            .sortedWith(compareBy<Routine> { it.position }.thenBy { it.name.lowercase() })
            .mapIndexed { index, item ->
                if (item.position != index) {
                    item.copy(position = index, updatedAt = updatedAt)
                } else {
                    item
                }
            }

    val existingInTarget =
        allRoutines
            .filter { it.folderId == targetFolderId }
            .sortedWith(compareBy<Routine> { it.position }.thenBy { it.name.lowercase() })

    val reindexedTarget =
        (existingInTarget + routine.copy(folderId = targetFolderId))
            .mapIndexed { index, item ->
                if (item.id == routineId || item.position != index) {
                    item.copy(folderId = targetFolderId, position = index, updatedAt = updatedAt)
                } else {
                    item
                }
            }

    val originalById = allRoutines.associateBy { it.id }
    return (remainingInSource + reindexedTarget).filter { updated ->
        originalById[updated.id] != updated
    }
}

/**
 * Comprueba si los índices seleccionados pueden agruparse en una superserie:
 * deben ser al menos 2, estar dentro del rango `0 until totalCount` y ser consecutivos.
 */
fun canGroupAsSuperset(totalCount: Int, selectedIndices: Set<Int>): Boolean {
    if (selectedIndices.size < 2 || totalCount < 2) return false
    if (selectedIndices.any { it !in 0 until totalCount }) return false
    val sorted = selectedIndices.sorted()
    return sorted.last() - sorted.first() == sorted.size - 1
}

/**
 * Comprueba si los IDs seleccionados corresponden a dos o más ejercicios consecutivos en [itemIds].
 */
fun canGroupAsSuperset(itemIds: List<String>, selectedIds: Set<String>): Boolean {
    if (selectedIds.size < 2 || itemIds.size < 2) return false
    val indices =
        selectedIds.map { id ->
            val idx = itemIds.indexOf(id)
            if (idx < 0) return false
            idx
        }.toSet()
    return canGroupAsSuperset(itemIds.size, indices)
}

/** Etiqueta canónica de superserie por orden de aparición: A, B, C… Z, AA… */
fun supersetLabelForIndex(groupIndex: Int): String {
    require(groupIndex >= 0) { "groupIndex must be non-negative" }
    val result = StringBuilder()
    var n = groupIndex
    do {
        val remainder = n % 26
        result.insert(0, ('A'.code + remainder).toChar())
        n = (n / 26) - 1
    } while (n >= 0)
    return result.toString()
}

/**
 * Normaliza posiciones (`0..n-1`) y etiquetas de superserie (`"A"`, `"B"`, `"C"`…) sobre una lista.
 * Cualquier tramo contiguo de 2 o más elementos con el mismo `supersetGroup` no nulo recibe una
 * etiqueta canónica por orden de aparición; los elementos aislados (tramo de longitud 1) quedan
 * con `supersetGroup = null`.
 */
fun <T> normalizeSupersetGroups(
    items: List<T>,
    getSupersetGroup: (T) -> String?,
    updateItem: (item: T, position: Int, supersetGroup: String?) -> T
): List<T> {
    if (items.isEmpty()) return emptyList()

    val assignedGroups = arrayOfNulls<String>(items.size)
    var nextGroupIndex = 0
    var start = 0

    while (start < items.size) {
        val rawGroup = getSupersetGroup(items[start])?.takeIf { it.isNotBlank() }
        if (rawGroup == null) {
            assignedGroups[start] = null
            start++
            continue
        }
        var end = start + 1
        while (end < items.size && getSupersetGroup(items[end])?.takeIf { it.isNotBlank() } == rawGroup) {
            end++
        }
        val runLength = end - start
        val normalizedLabel =
            if (runLength >= 2) {
                supersetLabelForIndex(nextGroupIndex++)
            } else {
                null
            }
        for (i in start until end) {
            assignedGroups[i] = normalizedLabel
        }
        start = end
    }

    return items.mapIndexed { index, item ->
        updateItem(item, index, assignedGroups[index])
    }
}

/**
 * Mueve un elemento de [fromIndex] a [toIndex] y normaliza posiciones y superseries.
 */
fun <T> moveSupersetItem(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int,
    getSupersetGroup: (T) -> String?,
    updateItem: (item: T, position: Int, supersetGroup: String?) -> T
): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
        return normalizeSupersetGroups(items, getSupersetGroup, updateItem)
    }
    val mutable = items.toMutableList()
    val moved = mutable.removeAt(fromIndex)
    mutable.add(toIndex, moved)
    return normalizeSupersetGroups(mutable, getSupersetGroup, updateItem)
}

/**
 * Agrupa los elementos cuyos IDs están en [selectedIds] en una superserie si son 2 o más consecutivos.
 * Si la selección no es consecutiva o tiene menos de 2 elementos, devuelve la lista normalizada sin cambios.
 */
fun <T> groupConsecutiveAsSuperset(
    items: List<T>,
    selectedIds: Set<String>,
    getId: (T) -> String,
    getSupersetGroup: (T) -> String?,
    updateItem: (item: T, position: Int, supersetGroup: String?) -> T
): List<T> {
    val itemIds = items.map(getId)
    if (!canGroupAsSuperset(itemIds, selectedIds)) {
        return normalizeSupersetGroups(items, getSupersetGroup, updateItem)
    }
    val tempToken = "__NEW_SUPERSET__"
    val tentativelyGrouped =
        items.mapIndexed { index, item ->
            if (getId(item) in selectedIds) {
                updateItem(item, index, tempToken)
            } else {
                item
            }
        }
    return normalizeSupersetGroups(tentativelyGrouped, getSupersetGroup, updateItem)
}

/**
 * Desagrupa todos los elementos pertenecientes a [supersetGroup] y renormaliza el resto de grupos.
 */
fun <T> ungroupSupersetGroup(
    items: List<T>,
    supersetGroup: String,
    getSupersetGroup: (T) -> String?,
    updateItem: (item: T, position: Int, supersetGroup: String?) -> T
): List<T> {
    val target = supersetGroup.trim()
    if (target.isEmpty()) return normalizeSupersetGroups(items, getSupersetGroup, updateItem)
    val cleared =
        items.mapIndexed { index, item ->
            if (getSupersetGroup(item) == target) {
                updateItem(item, index, null)
            } else {
                item
            }
        }
    return normalizeSupersetGroups(cleared, getSupersetGroup, updateItem)
}

/**
 * Saca un único elemento de su superserie; si el grupo restante queda con 1 solo elemento o partido,
 * se desagrupa automáticamente al normalizar.
 */
fun <T> removeItemFromSuperset(
    items: List<T>,
    itemId: String,
    getId: (T) -> String,
    getSupersetGroup: (T) -> String?,
    updateItem: (item: T, position: Int, supersetGroup: String?) -> T
): List<T> {
    val cleared =
        items.mapIndexed { index, item ->
            if (getId(item) == itemId) {
                updateItem(item, index, null)
            } else {
                item
            }
        }
    return normalizeSupersetGroups(cleared, getSupersetGroup, updateItem)
}

/** Sobrecarga de dominio para reordenar una lista de [RoutineExerciseWithSets]. */
fun moveRoutineExercise(
    exercises: List<RoutineExerciseWithSets>,
    fromIndex: Int,
    toIndex: Int,
    updatedAt: Long = 0L
): List<RoutineExerciseWithSets> = moveSupersetItem(
    items = exercises,
    fromIndex = fromIndex,
    toIndex = toIndex,
    getSupersetGroup = { it.routineExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.routineExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                routineExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/** Sobrecarga de dominio para agrupar ejercicios consecutivos en una superserie. */
fun groupRoutineExercisesAsSuperset(
    exercises: List<RoutineExerciseWithSets>,
    selectedExerciseIds: Set<String>,
    updatedAt: Long = 0L
): List<RoutineExerciseWithSets> = groupConsecutiveAsSuperset(
    items = exercises,
    selectedIds = selectedExerciseIds,
    getId = { it.routineExercise.id },
    getSupersetGroup = { it.routineExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.routineExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                routineExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/** Sobrecarga de dominio para desagrupar una superserie completa. */
fun ungroupRoutineSuperset(
    exercises: List<RoutineExerciseWithSets>,
    supersetGroup: String,
    updatedAt: Long = 0L
): List<RoutineExerciseWithSets> = ungroupSupersetGroup(
    items = exercises,
    supersetGroup = supersetGroup,
    getSupersetGroup = { it.routineExercise.supersetGroup },
    updateItem = { item, pos, group ->
        val current = item.routineExercise
        if (current.position != pos || current.supersetGroup != group) {
            item.copy(
                routineExercise =
                    current.copy(
                        position = pos,
                        supersetGroup = group,
                        updatedAt = if (updatedAt > 0L) updatedAt else current.updatedAt
                    )
            )
        } else {
            item
        }
    }
)

/**
 * Calcula el número de serie normal (1, 2, 3…) para cada posición de una lista de tipos de serie.
 * Las series especiales (`WARMUP`, `FAILURE`, `DROP`) no consumen número de serie normal.
 */
fun computeNormalSetNumbers(setTypes: List<SetType>): List<Int> {
    var normalCounter = 0
    return setTypes.map { type ->
        if (type == SetType.NORMAL) {
            normalCounter += 1
            normalCounter
        } else {
            (normalCounter + 1).coerceAtLeast(1)
        }
    }
}

/**
 * Convierte una [RoutineTemplate] en un [RoutineWithExercises] listo para guardarse en base de datos.
 */
fun RoutineTemplate.toRoutineWithExercises(
    folderId: String?,
    position: Int,
    isSpanish: Boolean,
    now: Long,
    idGenerator: () -> String = { UUID.randomUUID().toString() }
): RoutineWithExercises {
    val routineId = idGenerator()
    val routine =
        Routine(
            id = routineId,
            folderId = folderId,
            name = localizedName(isSpanish),
            notes = localizedNotes(isSpanish),
            position = position,
            createdAt = now,
            updatedAt = now
        )
    val rawExercises =
        exercises.mapIndexed { exerciseIndex, templateExercise ->
            val routineExerciseId = idGenerator()
            val sets =
                templateExercise.sets.mapIndexed { setIndex, templateSet ->
                    RoutineSet(
                        id = idGenerator(),
                        routineExerciseId = routineExerciseId,
                        position = setIndex,
                        setType = templateSet.setType,
                        targetWeightKg = templateSet.targetWeightKg,
                        targetRepsMin = templateSet.targetRepsMin,
                        targetRepsMax = templateSet.targetRepsMax,
                        targetDurationS = templateSet.targetDurationS,
                        targetDistanceM = templateSet.targetDistanceM,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            RoutineExerciseWithSets(
                routineExercise =
                    RoutineExercise(
                        id = routineExerciseId,
                        routineId = routineId,
                        exerciseId = templateExercise.exerciseId,
                        position = exerciseIndex,
                        supersetGroup = templateExercise.supersetGroup,
                        restSeconds = templateExercise.restSeconds,
                        notes = templateExercise.localizedNotes(isSpanish),
                        createdAt = now,
                        updatedAt = now
                    ),
                sets = sets
            )
        }
    val normalizedExercises =
        normalizeSupersetGroups(
            items = rawExercises,
            getSupersetGroup = { it.routineExercise.supersetGroup },
            updateItem = { item, pos, group ->
                item.copy(
                    routineExercise =
                        item.routineExercise.copy(
                            position = pos,
                            supersetGroup = group
                        )
                )
            }
        )
    return RoutineWithExercises(
        routine = routine,
        exercises = normalizedExercises
    )
}
