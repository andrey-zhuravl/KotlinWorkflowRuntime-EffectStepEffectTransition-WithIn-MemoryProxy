package com.example.workflow

import com.example.workflow.core.Effect
import com.example.workflow.core.Effects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectDslTest {
    @Test
    fun `persist with transition and side effects`() {
        val effect = Effects.persist<Int, String, String>("a", "b")
            .thenTransition { state -> state + 1 }
            .thenRun { }
            .thenReply { state -> "state=${'$'}state" }

        val exec = effect as Effect.Executable<Int, String, String>
        assertEquals(listOf("a", "b"), exec.events)
        assertNotNull(exec.transition)
        assertEquals("state=3", exec.reply?.invoke(3))
        assertEquals(Effect.ReplyType.WITH_REPLY, exec.replyType)
        assertEquals(1, exec.sideEffects.size)
    }

    @Test
    fun `transition composition preserves order`() {
        val effect = Effects.none<Int, String, String>()
            .thenTransition { it + 1 }
            .thenTransition { it * 2 }
            .thenReply { it.toString() }

        val exec = effect as Effect.Executable<Int, String, String>
        val transition = exec.transition
        assertNotNull(transition)
        assertEquals(8, transition.toState(3))
    }

    @Test
    fun `no reply effect`() {
        val effect = Effects.none<String, Int, String>()
            .thenRun { }
            .thenNoReply()

        val exec = effect as Effect.Executable<String, Int, String>
        assertTrue(exec.events.isEmpty())
        assertEquals(Effect.ReplyType.NO_REPLY, exec.replyType)
        assertNull(exec.reply)
    }
}
