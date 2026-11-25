package com.davidguijo.ferro.core.domain

import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Medidas corporales disponibles para resumen y gráfica. */
enum class BodyMetric {
    WEIGHT,
    BODY_FAT,
    NECK,
    CHEST,
    WAIST,
    HIPS,
    ARM_L,
    ARM_R,
    THIGH_L,
    THIGH_R,
    CALF
}

/** Pose normalizada de una foto de progreso. */
enum class PhotoPose(val key: String) {
    FRONT("front"),
    SIDE("side"),
    BACK("back");

    companion object {
        fun fromKey(key: String): PhotoPose = entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FRONT
    }
}

/** Último valor conocido de una medida junto con su diferencia frente al registro anterior no nulo. */
data class BodyMetricSummary(
    val metric: BodyMetric,
    val latestValue: Double?,
    val latestMeasuredAt: Long?,
    val previousValue: Double?,
    val delta: Double?
)

/** Punto cronológico para la gráfica de una medida corporal (en unidad canónica: kg, % o cm). */
data class BodyChartPoint(val measurementId: String, val timestampMillis: Long, val value: Double)

/** Grupo de fotos de progreso tomadas en la misma fecha local. */
data class PhotoDateGroup(val date: LocalDate, val photos: List<ProgressPhoto>)

/** Cálculos puros para medidas corporales, diferencias, gráficas y agrupación de fotos. */
object BodyMeasurementCalculations {
    /** Extrae el valor canónico de [metric] en [measurement] (`kg`, `%` o `cm`). */
    fun valueFor(measurement: BodyMeasurement, metric: BodyMetric): Double? = when (metric) {
        BodyMetric.WEIGHT -> measurement.weightKg
        BodyMetric.BODY_FAT -> measurement.bodyFatPct
        BodyMetric.NECK -> measurement.neckCm
        BodyMetric.CHEST -> measurement.chestCm
        BodyMetric.WAIST -> measurement.waistCm
        BodyMetric.HIPS -> measurement.hipsCm
        BodyMetric.ARM_L -> measurement.armLCm
        BodyMetric.ARM_R -> measurement.armRCm
        BodyMetric.THIGH_L -> measurement.thighLCm
        BodyMetric.THIGH_R -> measurement.thighRCm
        BodyMetric.CALF -> measurement.calfCm
    }

    /**
     * Calcula para cada [BodyMetric] el último valor registrado y su diferencia respecto al
     * registro anterior que también tenga valor no nulo para esa medida.
     */
    fun computeMetricSummaries(measurements: List<BodyMeasurement>): List<BodyMetricSummary> {
        val sortedDesc =
            measurements.sortedWith(
                compareByDescending<BodyMeasurement> { it.measuredAt }
                    .thenByDescending { it.createdAt }
            )
        return BodyMetric.entries.map { metric ->
            val nonNullValues =
                sortedDesc.mapNotNull { entry ->
                    val value = valueFor(entry, metric)
                    if (value != null && value.isFinite()) {
                        entry.measuredAt to value
                    } else {
                        null
                    }
                }
            val latest = nonNullValues.getOrNull(0)
            val previous = nonNullValues.getOrNull(1)
            val delta =
                if (latest != null && previous != null) {
                    latest.second - previous.second
                } else {
                    null
                }
            BodyMetricSummary(
                metric = metric,
                latestValue = latest?.second,
                latestMeasuredAt = latest?.first,
                previousValue = previous?.second,
                delta = delta
            )
        }
    }

    /**
     * Construye la serie temporal ordenada cronológicamente para [metric] desde [sinceMillis]
     * (o todo el historial si es `null`).
     */
    fun buildChartSeries(
        measurements: List<BodyMeasurement>,
        metric: BodyMetric,
        sinceMillis: Long?
    ): List<BodyChartPoint> = measurements
        .asSequence()
        .filter { sinceMillis == null || it.measuredAt >= sinceMillis }
        .mapNotNull { measurement ->
            val value = valueFor(measurement, metric)
            if (value == null || !value.isFinite()) {
                null
            } else {
                BodyChartPoint(
                    measurementId = measurement.id,
                    timestampMillis = measurement.measuredAt,
                    value = value
                )
            }
        }.sortedBy { it.timestampMillis }
        .toList()

    /** Agrupa [photos] por día local ordenadas de más reciente a más antigua. */
    fun groupPhotosByDate(photos: List<ProgressPhoto>, zoneId: ZoneId = ZoneId.systemDefault()): List<PhotoDateGroup> {
        if (photos.isEmpty()) return emptyList()
        val sortedDesc =
            photos.sortedWith(
                compareByDescending<ProgressPhoto> { it.takenAt }
                    .thenByDescending { it.createdAt }
            )
        val grouped = linkedMapOf<LocalDate, MutableList<ProgressPhoto>>()
        for (photo in sortedDesc) {
            val date = Instant.ofEpochMilli(photo.takenAt).atZone(zoneId).toLocalDate()
            grouped.getOrPut(date) { mutableListOf() }.add(photo)
        }
        return grouped.entries.map { (date, items) ->
            PhotoDateGroup(date = date, photos = items)
        }
    }
}
