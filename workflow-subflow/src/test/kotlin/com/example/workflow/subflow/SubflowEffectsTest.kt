package com.example.workflow.subflow

import kotlin.test.Test
import kotlin.test.assertEquals

class SubflowEffectsTest {
    @Test
    fun `start child encodes options`() {
        val effect = SubflowEffects.startChild<Unit, Unit, Unit>(
            childType = "Invoice",
            childId = "123",
            initialCommandJson = "{\"cmd\":\"start\"}",
            options = ChildOptions(awaitCompletion = true, correlationKey = "order-1")
        )

        assertEquals("start-child", effect.name)
        assertEquals(true, effect.attributes["awaitCompletion"])
        assertEquals("order-1", effect.attributes["correlationKey"])
    }
}
