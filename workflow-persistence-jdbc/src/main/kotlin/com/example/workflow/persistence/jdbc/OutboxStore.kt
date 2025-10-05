package com.example.workflow.persistence.jdbc

import java.time.Instant

/**
 * Durable outbox table accessor.
 */
interface OutboxStore {
    suspend fun enqueue(entries: List<OutboxMessage>)
    suspend fun markPublished(id: Long, publishedAt: Instant = Instant.now())
    suspend fun pollBatch(limit: Int = 100): List<OutboxMessage>

    data class OutboxMessage(
        val id: Long,
        val channel: String,
        val key: String?,
        val payloadJson: String,
        val type: String,
        val createdAt: Instant,
        val publishedAt: Instant?
    )
}
