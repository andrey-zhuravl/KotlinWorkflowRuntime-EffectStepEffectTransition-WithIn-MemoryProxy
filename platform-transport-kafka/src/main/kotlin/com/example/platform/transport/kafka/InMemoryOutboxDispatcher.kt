package com.example.platform.transport.kafka

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
