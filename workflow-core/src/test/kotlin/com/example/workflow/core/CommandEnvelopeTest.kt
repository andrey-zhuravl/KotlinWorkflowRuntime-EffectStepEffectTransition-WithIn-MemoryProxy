package com.example.workflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandEnvelopeTest {
    @Test
    fun `defaults to normal priority`() {
        val envelope = CommandEnvelope(workflowType = "Order", workflowId = "123", command = "Create")
        assertEquals(Priority.NORMAL, envelope.priority)
        assertNull(envelope.tenantId)
    }

    @Test
    fun `uses provided metadata`() {
        val envelope = CommandEnvelope(
            workflowType = "Order",
            workflowId = "123",
            command = "Create",
            priority = Priority.HIGH,
            tenantId = "acme",
            metadata = mapOf("source" to "api")
        )

        assertEquals("api", envelope.metadata["source"])
        assertEquals(Priority.HIGH, envelope.priority)
        assertEquals("acme", envelope.tenantId)
    }
}
