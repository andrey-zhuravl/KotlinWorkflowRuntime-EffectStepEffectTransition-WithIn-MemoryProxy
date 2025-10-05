package com.example.workflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private data class StubContext(
    override val state: Int,
    var violations: MutableList<String> = mutableListOf()
) : WorkflowContext<Int, String, String, String> {
    override fun onGuardViolation(name: String) {
        violations.add(name)
    }
}

class GuardDslTest {
    @Test
    fun `guard passes when predicate true`() {
        val ctx = StubContext(state = 1)
        val effect = ctx.guard("positive", ctx.state, "command") { state, _ -> state > 0 }
            .orReject { "rejected" }

        assertEquals(Effect(type = "noop"), effect)
        assertEquals(emptyList(), ctx.violations)
    }

    @Test
    fun `guard rejects and records violation`() {
        val ctx = StubContext(state = -1)
        val effect = ctx.guard("positive", ctx.state, "command") { state, _ -> state > 0 }
            .orReject { state -> "state $state" }

        assertEquals("positive", ctx.violations.single())
        assertEquals("rejection", effect.type)
        assertEquals("state -1", effect.payload)
    }

    @Test
    fun `guard throws when configured`() {
        val ctx = StubContext(state = 0)
        assertFailsWith<GuardViolationException> {
            ctx.guard("non-zero", ctx.state, "command") { state, _ -> state != 0 }
                .orThrow("Must be non-zero")
        }
        assertEquals(listOf("non-zero"), ctx.violations)
    }
}
