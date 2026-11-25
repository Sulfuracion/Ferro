package com.davidguijo.ferro.core.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

/** Unidad de peso que el usuario ve. En la base de datos el peso siempre está en kg. */
enum class WeightUnit { KG, LB }

/** Unidad de longitud para perímetros corporales. En la base de datos se guardan siempre en cm. */
enum class LengthUnit { CM, IN }

/** Conversión, formateo y parseo de unidades de peso y longitud. Funciones puras: kg y cm son las unidades canónicas. */
object Units {
    /** Kilogramos por libra internacional (definición exacta). */
    const val KG_PER_LB: Double = 0.45359237

    /** Centímetros por pulgada internacional (definición exacta). */
    const val CM_PER_INCH: Double = 2.54

    const val DEFAULT_STEP_KG: Double = 0.5
    const val DEFAULT_STEP_LB: Double = 1.0

    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB

    fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH

    fun cmToInches(cm: Double): Double = cm / CM_PER_INCH

    /** Unidad de longitud asociada a la unidad de peso elegida en ajustes (kg -> cm, lb -> in). */
    fun lengthUnitFor(weightUnit: WeightUnit): LengthUnit = when (weightUnit) {
        WeightUnit.KG -> LengthUnit.CM
        WeightUnit.LB -> LengthUnit.IN
    }

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

    /** Convierte una longitud guardada (cm) a la unidad de visualización. */
    fun fromCm(cm: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.CM -> cm
        LengthUnit.IN -> cmToInches(cm)
    }

    /** Convierte una longitud introducida en [unit] a cm para guardarla. */
    fun toCm(value: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.CM -> value
        LengthUnit.IN -> inchesToCm(value)
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

    /**
     * Formatea una longitud guardada en cm para mostrarla en [unit] con separador decimal según [locale].
     */
    fun formatLength(
        lengthCm: Double,
        unit: LengthUnit,
        locale: Locale = Locale.getDefault(),
        includeUnit: Boolean = false
    ): String {
        val converted = fromCm(lengthCm, unit)
        val normalized = if (converted == 0.0) 0.0 else converted
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val formatter =
            DecimalFormat("0.##", symbols).apply {
                isGroupingUsed = false
            }
        val numberText = formatter.format(normalized)
        return if (includeUnit) {
            val suffix =
                when (unit) {
                    LengthUnit.CM -> "cm"
                    LengthUnit.IN -> "in"
                }
            "$numberText $suffix"
        } else {
            numberText
        }
    }

    /** Parsea [input] en la unidad [unit] y lo convierte a cm para guardarlo. */
    fun parseLengthToCm(input: String, unit: LengthUnit): Double? = parseWeightInput(input)?.let { toCm(it, unit) }

    /** Formatea un porcentaje de grasa corporal con hasta 1 decimal según [locale]. */
    fun formatBodyFat(bodyFatPct: Double, locale: Locale = Locale.getDefault(), includeUnit: Boolean = false): String {
        val normalized = if (bodyFatPct == 0.0) 0.0 else bodyFatPct
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val formatter =
            DecimalFormat("0.#", symbols).apply {
                isGroupingUsed = false
            }
        val numberText = formatter.format(normalized)
        return if (includeUnit) "$numberText %" else numberText
    }
}
