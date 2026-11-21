package com.davidguijo.ferro.core.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {
    @Test
    fun `calcula cuenta atras, ajustes +15 -15 y expiracion a partir de timestamp con Clock falso`() {
        val clock = MutableFakeClock(initialMillis = 100_000L)
        val initial = RestTimer.start(workoutId = "w-1", exerciseId = "ex-1", durationSeconds = 90, clock = clock)
        assertNotNull(initial)
        val timer = initial!!

        assertEquals(190_000L, timer.endAtMillis)
        assertEquals(90, RestTimer.remainingSeconds(timer, clock))
        assertEquals("1:30", RestTimer.formatRemaining(RestTimer.remainingSeconds(timer, clock)))
        assertEquals(1.0f, RestTimer.progress(timer, clock), 1e-4f)
        assertFalse(RestTimer.isFinished(timer, clock))

        // Simula segundo plano / pantalla apagada avanzando el reloj 44,5 s directamente
        clock.advanceMillis(44_500L)
        assertEquals(45_500L, RestTimer.remainingMillis(timer, clock))
        assertEquals(46, RestTimer.remainingSeconds(timer, clock))
        assertEquals("0:46", RestTimer.formatRemaining(RestTimer.remainingSeconds(timer, clock)))
        assertEquals(45.5f / 90.0f, RestTimer.progress(timer, clock), 1e-4f)

        // +15 s aumenta endAtMillis sin perder exactitud
        val added = RestTimer.adjust(timer, +15, clock)!!
        assertEquals(205_000L, added.endAtMillis)
        assertEquals(61, RestTimer.remainingSeconds(added, clock))

        // -15 s vuelve al tiempo anterior
        val subtracted = RestTimer.adjust(added, -15, clock)!!
        assertEquals(190_000L, subtracted.endAtMillis)
        assertEquals(46, RestTimer.remainingSeconds(subtracted, clock))

        // Avanza hasta quedar 10 s y restar 15 s devuelve null (cancela el descanso)
        clock.advanceMillis(35_500L) // ahora t = 180_000L, quedan 10 s
        assertEquals(10, RestTimer.remainingSeconds(subtracted, clock))
        assertNull(RestTimer.adjust(subtracted, -15, clock))

        // Al alcanzar o superar endAtMillis queda finalizado
        clock.advanceMillis(10_000L)
        assertTrue(RestTimer.isFinished(subtracted, clock))
        assertEquals(0, RestTimer.remainingSeconds(subtracted, clock))
        assertEquals("0:00", RestTimer.formatRemaining(0))
        assertEquals(0f, RestTimer.progress(subtracted, clock), 1e-6f)
    }

    @Test
    fun `iniciar descanso con duracion cero o negativa devuelve null`() {
        val clock = MutableFakeClock(10_000L)
        assertNull(RestTimer.start("w-1", null, 0, clock))
        assertNull(RestTimer.start("w-1", null, -15, clock))
    }

    private class MutableFakeClock(initialMillis: Long) : Clock() {
        private var currentMillis: Long = initialMillis

        fun advanceMillis(deltaMillis: Long) {
            currentMillis += deltaMillis
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis)

        override fun millis(): Long = currentMillis
    }
}
