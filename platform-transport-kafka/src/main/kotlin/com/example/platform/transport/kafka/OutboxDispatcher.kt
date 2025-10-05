package com.example.platform.transport.kafka

import com.example.platform.core.OutboxMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class OutboxMessageEnvelope(
    val workflowType: String,
    val workflowId: String,
    val message: OutboxMessage,
    val channel: String,
    val headers: Map<String, String>,
    val tenantId: String?,
    val correlationId: String?,
)

fun interface OutboxDispatcher {
    suspend fun publish(envelope: OutboxMessageEnvelope)
}

class InMemoryOutboxDispatcher : OutboxDispatcher {
    private val mutex = Mutex()
    private val messages = mutableListOf<OutboxMessageEnvelope>()

    override suspend fun publish(envelope: OutboxMessageEnvelope) {
        mutex.withLock {
            messages += envelope
        }
    }

    suspend fun drain(): List<OutboxMessageEnvelope> = mutex.withLock {
        val copy = messages.toList()
        messages.clear()
        copy
    }

    suspend fun peek(): List<OutboxMessageEnvelope> = mutex.withLock { messages.toList() }
}
