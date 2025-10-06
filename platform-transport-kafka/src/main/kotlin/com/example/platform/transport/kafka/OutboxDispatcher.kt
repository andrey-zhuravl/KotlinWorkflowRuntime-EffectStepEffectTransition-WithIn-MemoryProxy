package com.example.platform.transport.kafka

fun interface OutboxDispatcher {
    suspend fun publish(envelope: OutboxMessageEnvelope)
}
