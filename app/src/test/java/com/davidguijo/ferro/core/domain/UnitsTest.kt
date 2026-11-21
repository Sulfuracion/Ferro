package com.davidguijo.ferro.core.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    private val localeEs = Locale.forLanguageTag("es-ES")
    private val localeEn = Locale.US

    @Test
    fun `una libra son 0,45359237 kg`() {
        assertEquals(0.45359237, Units.lbToKg(1.0), 1e-12)
    }

    @Test
    fun `100 kg son unas 220,462 lb`() {
        assertEquals(220.46226218487757, Units.kgToLb(100.0), 1e-9)
    }

    @Test
    fun `cero kg y cero lb son cero`() {
        assertEquals(0.0, Units.kgToLb(0.0), 0.0)
        assertEquals(0.0, Units.lbToKg(0.0), 0.0)
    }

    @Test
    fun `ida y vuelta kg-lb-kg conserva la precision exacta`() {
        val weightsKg = listOf(0.0, 0.5, 1.25, 20.0, 45.359237, 60.0, 100.0, 102.5, 142.75, 500.0)
        for (kg in weightsKg) {
            val lb = Units.kgToLb(kg)
            val roundTripKg = Units.lbToKg(lb)
            assertEquals("Round-trip failed for $kg kg", kg, roundTripKg, 1e-12)
        }
    }

    @Test
    fun `ida y vuelta lb-kg-lb conserva la precision exacta`() {
        val weightsLb = listOf(0.0, 1.0, 2.5, 45.0, 135.0, 225.0, 315.5, 405.0, 1000.0)
        for (lb in weightsLb) {
            val kg = Units.lbToKg(lb)
            val roundTripLb = Units.kgToLb(kg)
            assertEquals("Round-trip failed for $lb lb", lb, roundTripLb, 1e-12)
        }
    }

    @Test
    fun `en kg fromKg y toKg no aplican conversion`() {
        assertEquals(80.0, Units.fromKg(80.0, WeightUnit.KG), 0.0)
        assertEquals(80.0, Units.toKg(80.0, WeightUnit.KG), 0.0)
    }

    @Test
    fun `formatWeight redondea a 0,5 kg por defecto y usa coma o punto segun idioma`() {
        assertEquals("102,5", Units.formatWeight(102.5, WeightUnit.KG, localeEs))
        assertEquals("102.5", Units.formatWeight(102.5, WeightUnit.KG, localeEn))

        assertEquals("102,5", Units.formatWeight(102.3, WeightUnit.KG, localeEs))
        assertEquals("102", Units.formatWeight(102.2, WeightUnit.KG, localeEs))
        assertEquals("100", Units.formatWeight(100.0, WeightUnit.KG, localeEn))
        assertEquals("0", Units.formatWeight(-0.0, WeightUnit.KG, localeEs))
    }

    @Test
    fun `formatWeight redondea a 1 lb por defecto`() {
        // 100 kg = 220.462 lb -> 220 lb
        assertEquals("220", Units.formatWeight(100.0, WeightUnit.LB, localeEn))
        // 45.359237 kg = 100.0 lb -> 100 lb
        assertEquals("100", Units.formatWeight(45.359237, WeightUnit.LB, localeEs))
        // 102.5 kg = 225.973 lb -> 226 lb
        assertEquals("226", Units.formatWeight(102.5, WeightUnit.LB, localeEn))
    }

    @Test
    fun `formatWeight permite paso de redondeo configurable o sin redondeo de paso`() {
        assertEquals("10,25", Units.formatWeight(10.24, WeightUnit.KG, localeEs, roundingStep = 0.25))
        assertEquals("10.25", Units.formatWeight(10.24, WeightUnit.KG, localeEn, roundingStep = 0.25))
        assertEquals("220,5", Units.formatWeight(100.0, WeightUnit.LB, localeEs, roundingStep = 0.5))
        assertEquals("220.46", Units.formatWeight(100.0, WeightUnit.LB, localeEn, roundingStep = null))
    }

    @Test
    fun `formatWeight con unidad incluye el sufijo correcto y sin separador de miles`() {
        assertEquals("102,5 kg", Units.formatWeight(102.5, WeightUnit.KG, localeEs, includeUnit = true))
        assertEquals("220 lb", Units.formatWeight(100.0, WeightUnit.LB, localeEn, includeUnit = true))
        assertEquals("1000", Units.formatWeight(1000.0, WeightUnit.KG, localeEs))
    }

    @Test
    fun `parseWeightInput acepta coma y punto y recorta espacios`() {
        assertEquals(102.5, Units.parseWeightInput("102,5")!!, 1e-12)
        assertEquals(102.5, Units.parseWeightInput("102.5")!!, 1e-12)
        assertEquals(80.0, Units.parseWeightInput("  80  ")!!, 1e-12)
        assertEquals(0.5, Units.parseWeightInput(",5")!!, 1e-12)
        assertEquals(0.5, Units.parseWeightInput(".5")!!, 1e-12)
        assertEquals(80.0, Units.parseWeightInput("80,")!!, 1e-12)
        assertEquals(0.0, Units.parseWeightInput("0")!!, 0.0)
    }

    @Test
    fun `parseWeightInput rechaza entradas invalidas o negativas`() {
        assertNull(Units.parseWeightInput(""))
        assertNull(Units.parseWeightInput("   "))
        assertNull(Units.parseWeightInput("abc"))
        assertNull(Units.parseWeightInput("100 kg"))
        assertNull(Units.parseWeightInput("10.2.5"))
        assertNull(Units.parseWeightInput("10,2.5"))
        assertNull(Units.parseWeightInput("-5"))
        assertNull(Units.parseWeightInput("NaN"))
        assertNull(Units.parseWeightInput("Infinity"))
    }

    @Test
    fun `parseWeightToKg convierte la entrada segun la unidad`() {
        assertEquals(102.5, Units.parseWeightToKg("102,5", WeightUnit.KG)!!, 1e-12)
        assertEquals(45.359237, Units.parseWeightToKg("100", WeightUnit.LB)!!, 1e-9)
        assertNull(Units.parseWeightToKg("", WeightUnit.LB))
    }
}
