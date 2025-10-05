package com.example.workflow.timers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class TimerEffectsTest {
    @Test
    fun `schedule cron carries attributes`() {
        val effect = TimerEffects.scheduleCron<Unit, Unit, Unit>("billing", "*/5 * * * *", jitter = 1.minutes, tags = setOf("invoice"))
        assertEquals("schedule-cron", effect.name)
        assertEquals("billing", effect.attributes["key"])
        assertEquals(listOf("invoice"), effect.attributes["tags"])
    }

    @Test
    fun `mass reschedule encodes selector`() {
        val selector = TimerSelector(prefix = "order", tags = setOf("sla"))
        val effect = TimerEffects.massReschedule<Unit, Unit, Unit>(selector, 5.minutes)
        assertEquals("mass-reschedule", effect.name)
        assertEquals("order", effect.attributes["prefix"])
        assertEquals(5.minutes.inWholeMilliseconds, effect.attributes["shiftMillis"])
    }
}
