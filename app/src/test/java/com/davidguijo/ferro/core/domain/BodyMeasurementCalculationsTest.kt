package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMeasurementCalculationsTest {
    @Test
    fun `computeMetricSummaries calcula ultimo valor y diferencia respecto al anterior no nulo`() {
        val m1 =
            measurement(
                id = "m-1",
                measuredAt = 1_000L,
                weightKg = 82.0,
                waistCm = 86.0,
                chestCm = 100.0
            )
        val m2 =
            measurement(
                id = "m-2",
                measuredAt = 2_000L,
                weightKg = 80.5,
                waistCm = null,
                chestCm = 101.5
            )
        val m3 =
            measurement(
                id = "m-3",
                measuredAt = 3_000L,
                weightKg = 79.8,
                waistCm = 84.0,
                chestCm = null
            )

        val summaries = BodyMeasurementCalculations.computeMetricSummaries(listOf(m1, m2, m3)).associateBy { it.metric }

        val weight = summaries.getValue(BodyMetric.WEIGHT)
        assertEquals(79.8, weight.latestValue!!, 1e-9)
        assertEquals(80.5, weight.previousValue!!, 1e-9)
        assertEquals(-0.7, weight.delta!!, 1e-9)

        // Waist fue null en m2, así que compara m3 (84.0) con m1 (86.0)
        val waist = summaries.getValue(BodyMetric.WAIST)
        assertEquals(84.0, waist.latestValue!!, 1e-9)
        assertEquals(86.0, waist.previousValue!!, 1e-9)
        assertEquals(-2.0, waist.delta!!, 1e-9)

        // Chest fue null en m3, así que el último conocido es m2 (101.5) frente a m1 (100.0)
        val chest = summaries.getValue(BodyMetric.CHEST)
        assertEquals(101.5, chest.latestValue!!, 1e-9)
        assertEquals(100.0, chest.previousValue!!, 1e-9)
        assertEquals(1.5, chest.delta!!, 1e-9)

        val neck = summaries.getValue(BodyMetric.NECK)
        assertNull(neck.latestValue)
        assertNull(neck.delta)
    }

    @Test
    fun `computeMetricSummaries con un unico registro deja delta en null`() {
        val single = measurement(id = "m-1", measuredAt = 1_000L, weightKg = 75.0)
        val summaries = BodyMeasurementCalculations.computeMetricSummaries(listOf(single)).associateBy { it.metric }
        val weight = summaries.getValue(BodyMetric.WEIGHT)
        assertEquals(75.0, weight.latestValue!!, 1e-9)
        assertNull(weight.previousValue)
        assertNull(weight.delta)
    }

    @Test
    fun `buildChartSeries filtra por rango y ordena cronologicamente`() {
        val m1 = measurement(id = "m-1", measuredAt = 1_000L, weightKg = 82.0)
        val m2 = measurement(id = "m-2", measuredAt = 3_000L, weightKg = 80.0)
        val m3 = measurement(id = "m-3", measuredAt = 2_000L, weightKg = null)
        val m4 = measurement(id = "m-4", measuredAt = 2_500L, weightKg = 81.0)

        val allPoints = BodyMeasurementCalculations.buildChartSeries(listOf(m2, m1, m3, m4), BodyMetric.WEIGHT, null)
        assertEquals(listOf("m-1", "m-4", "m-2"), allPoints.map { it.measurementId })

        val sincePoints =
            BodyMeasurementCalculations.buildChartSeries(listOf(m2, m1, m3, m4), BodyMetric.WEIGHT, 2_000L)
        assertEquals(listOf("m-4", "m-2"), sincePoints.map { it.measurementId })
    }

    @Test
    fun `groupPhotosByDate agrupa por fecha local descendente`() {
        val day1 = LocalDate.of(2026, 11, 20).atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val day1Later = LocalDate.of(2026, 11, 20).atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val day2 = LocalDate.of(2026, 11, 25).atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        val p1 = ProgressPhoto("p1", day1, "a.jpg", "front", null, day1, day1)
        val p2 = ProgressPhoto("p2", day1Later, "b.jpg", "side", null, day1Later, day1Later)
        val p3 = ProgressPhoto("p3", day2, "c.jpg", "back", null, day2, day2)

        val groups = BodyMeasurementCalculations.groupPhotosByDate(listOf(p1, p2, p3), ZoneOffset.UTC)
        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 11, 25), groups[0].date)
        assertEquals(listOf("p3"), groups[0].photos.map { it.id })
        assertEquals(LocalDate.of(2026, 11, 20), groups[1].date)
        assertEquals(listOf("p2", "p1"), groups[1].photos.map { it.id })
    }

    private fun measurement(
        id: String,
        measuredAt: Long,
        weightKg: Double? = null,
        bodyFatPct: Double? = null,
        neckCm: Double? = null,
        chestCm: Double? = null,
        waistCm: Double? = null
    ) = BodyMeasurement(
        id = id,
        measuredAt = measuredAt,
        weightKg = weightKg,
        bodyFatPct = bodyFatPct,
        neckCm = neckCm,
        chestCm = chestCm,
        waistCm = waistCm,
        hipsCm = null,
        armLCm = null,
        armRCm = null,
        thighLCm = null,
        thighRCm = null,
        calfCm = null,
        notes = "",
        createdAt = measuredAt,
        updatedAt = measuredAt
    )
}
