package com.example.workflow.security

import kotlin.test.Test
import kotlin.test.assertEquals

class PolicyEngineTest {
    @Test
    fun `policy engine can be implemented with lambda`() {
        val engine = PolicyEngine { subject, action, _ ->
            if ("admin" in subject.roles && action.verb == "write") Decision.ALLOW else Decision.DENY
        }

        val decision = engine.authorize(
            Subject("user-1", setOf("admin")),
            Action("write", "workflow"),
            Resource("workflow", "order-123")
        )

        assertEquals(Decision.ALLOW, decision)
    }
}
