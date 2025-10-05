package com.example.workflow.human

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class HumanEffectsTest {
    @Test
    fun `await human encodes attributes`() {
        val effect = HumanEffects.awaitHuman<Unit, Unit, Unit>(
            token = "token-1",
            ttl = 5.minutes,
            payload = "{}",
            notifyChannel = "slack"
        )

        assertEquals("await-human", effect.name)
        assertEquals("token-1", effect.attributes["token"])
        assertEquals(5.minutes.inWholeMilliseconds, effect.attributes["ttlMillis"])
        assertEquals("slack", effect.attributes["notifyChannel"])
    }
}
