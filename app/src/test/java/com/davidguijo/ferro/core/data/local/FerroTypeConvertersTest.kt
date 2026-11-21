package com.davidguijo.ferro.core.data.local

import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FerroTypeConvertersTest {
    @Test
    fun allEnumsRoundTripCorrectly() {
        SetType.entries.forEach {
            assertEquals(it, FerroTypeConverters.toSetType(FerroTypeConverters.fromSetType(it)))
        }
        TrackingType.entries.forEach {
            assertEquals(it, FerroTypeConverters.toTrackingType(FerroTypeConverters.fromTrackingType(it)))
        }
        Muscle.entries.forEach {
            assertEquals(it, FerroTypeConverters.toMuscle(FerroTypeConverters.fromMuscle(it)))
        }
        Equipment.entries.forEach {
            assertEquals(it, FerroTypeConverters.toEquipment(FerroTypeConverters.fromEquipment(it)))
        }
        RecordType.entries.forEach {
            assertEquals(it, FerroTypeConverters.toRecordType(FerroTypeConverters.fromRecordType(it)))
        }
        WorkoutStatus.entries.forEach {
            assertEquals(it, FerroTypeConverters.toWorkoutStatus(FerroTypeConverters.fromWorkoutStatus(it)))
        }
    }

    @Test
    fun muscleListRoundTripsEmptySingleAndMultiple() {
        assertEquals(
            emptyList<Muscle>(),
            FerroTypeConverters.toMuscleList(FerroTypeConverters.fromMuscleList(emptyList()))
        )
        val single = listOf(Muscle.TRICEPS)
        assertEquals(single, FerroTypeConverters.toMuscleList(FerroTypeConverters.fromMuscleList(single)))
        val multiple = listOf(Muscle.TRICEPS, Muscle.SHOULDERS, Muscle.UPPER_BACK)
        assertEquals(multiple, FerroTypeConverters.toMuscleList(FerroTypeConverters.fromMuscleList(multiple)))
    }
}
