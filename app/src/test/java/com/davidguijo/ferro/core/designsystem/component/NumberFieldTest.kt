package com.davidguijo.ferro.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFieldTest {
    @Test
    fun `sanitizeNumericInput normalizes decimal separator to locale`() {
        assertEquals("102,5", sanitizeNumericInput("102.5", allowDecimal = true, ','))
        assertEquals("102.5", sanitizeNumericInput("102,5", allowDecimal = true, '.'))
        assertEquals("102,55", sanitizeNumericInput("102,5.5", allowDecimal = true, ','))
        assertEquals("1025", sanitizeNumericInput("102,5", allowDecimal = false, ','))
        assertEquals("80", sanitizeNumericInput("80abc", allowDecimal = true, ','))
    }
}
