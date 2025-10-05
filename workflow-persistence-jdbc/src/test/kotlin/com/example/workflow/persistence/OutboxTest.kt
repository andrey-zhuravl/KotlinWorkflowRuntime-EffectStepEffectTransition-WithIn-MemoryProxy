package com.example.workflow.persistence

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OutboxTest {
    @Test
    fun `records default attempts`() {
        val record = OutboxRecord(
            id = UUID.randomUUID(),
            channel = "order-events",
            key = "order-1",
            payloadJson = "{}",
            createdAt = Instant.EPOCH
        )

        assertEquals(0, record.attempts)
        assertEquals(null, record.publishedAt)
    }
}
