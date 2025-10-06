package com.example.sample.order

import com.example.platform.core.Effect
import com.example.platform.core.Effects
import com.example.testing.Test
import com.example.testing.assertEquals
import com.example.testing.assertNotNull
import com.example.testing.assertNull

class EffectDslTest {
    @Test
    fun chainingSupportsSideEffectsAndReply() {
        val effect = Effects
            .none<Int, String, String>()
            .thenRun { }
            .thenReply { "state-$it" }

        val result = effect as Effect.Result<Int, String, String>
        assertEquals(emptyList<String>(), result.events)
        assertEquals(1, result.sideEffects.size)
        assertEquals("state-0", result.reply?.invoke(0))
    }

    @Test
    fun persistAndTransition() {
        val effect = Effects
            .persist<Int, String, String>("evt1", "evt2")
            .thenTransition { it + 1 }
            .thenReply { "value-$it" }

        val result = effect as Effect.Result<Int, String, String>
        assertEquals(listOf("evt1", "evt2"), result.events)
        assertNotNull(result.transition)
        assertEquals("value-5", result.reply?.invoke(5))
    }

    @Test
    fun transitionAppliesAfterEvents() {
        val effect = Effects
            .persist<Int, Int, Unit>(1, 2, 3)
            .thenTransition { it + 10 }
            .thenNoReply()

        val result = effect as Effect.Result<Int, Int, Unit>
        var state = 0
        for (event in result.events) {
            state += event
        }
        val transitioned = result.transition?.toState?.invoke(state) ?: state
        assertEquals(16, transitioned)
        assertNull(result.reply)
    }
}
