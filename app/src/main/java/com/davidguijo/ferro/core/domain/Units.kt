package com.davidguijo.ferro.core.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

/** Unidad de peso que el usuario ve. En la base de datos el peso siempre está en kg. */
enum class WeightUnit { KG, LB }

/** Conversión, formateo y parseo de unidades de peso. Funciones puras: kg es la unidad canónica. */
object Units {
    /** Kilogramos por libra internacional (definición exacta). */
    const val KG_PER_LB: Double = 0.45359237

    const val DEFAULT_STEP_KG: Double = 0.5
    const val DEFAULT_STEP_LB: Double = 1.0

    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB

    /** Convierte un peso guardado (kg) a la unidad de visualización. */
    fun fromKg(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> kg
        WeightUnit.LB -> kgToLb(kg)
    }

    /** Convierte un peso introducido en [unit] a kg para guardarlo. */
    fun toKg(value: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> value
        WeightUnit.LB -> lbToKg(value)
    }

    /** Paso de redondeo por defecto para mostrar pesos: 0,5 kg o 1 lb. */
    fun defaultRoundingStep(unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> DEFAULT_STEP_KG
        WeightUnit.LB -> DEFAULT_STEP_LB
    }

    /** Redondea [value] al múltiplo más cercano de [step]. */
    fun roundToStep(value: Double, step: Double): Double {
        if (!value.isFinite() || step <= 0.0) return value
        val rounded = round(value / step) * step
        return if (rounded == 0.0) 0.0 else rounded
    }

    /**
     * Formatea un peso guardado en kg para mostrarlo en [unit], con redondeo configurable
     * (por defecto 0,5 kg o 1 lb) y separador decimal según [locale] (coma o punto).
     */
    fun formatWeight(
        weightKg: Double,
        unit: WeightUnit,
        locale: Locale = Locale.getDefault(),
        roundingStep: Double? = defaultRoundingStep(unit),
        includeUnit: Boolean = false
    ): String {
        val converted = fromKg(weightKg, unit)
        val rounded =
            if (roundingStep != null && roundingStep > 0.0) {
                roundToStep(converted, roundingStep)
            } else {
                converted
            }
        val normalized = if (rounded == 0.0) 0.0 else rounded
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val formatter =
            DecimalFormat("0.##", symbols).apply {
                isGroupingUsed = false
            }
        val numberText = formatter.format(normalized)
        return if (includeUnit) {
            val suffix =
                when (unit) {
                    WeightUnit.KG -> "kg"
                    WeightUnit.LB -> "lb"
                }
            "$numberText $suffix"
        } else {
            numberText
        }
    }

    /**
     * Parsea una entrada de peso del usuario aceptando tanto coma como punto decimal.
     * Devuelve null si está vacía, no es numérica, no es finita o es negativa.
     */
    fun parseWeightInput(input: String): Double? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(',', '.')
        val parsed = normalized.toDoubleOrNull() ?: return null
        if (!parsed.isFinite() || parsed < 0.0) return null
        return if (parsed == 0.0) 0.0 else parsed
    }

    /** Parsea [input] en la unidad [unit] y lo convierte a kg para guardarlo. */
    fun parseWeightToKg(input: String, unit: WeightUnit): Double? = parseWeightInput(input)?.let { toKg(it, unit) }
}
