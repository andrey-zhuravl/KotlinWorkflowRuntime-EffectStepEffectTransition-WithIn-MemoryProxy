package com.example.platform.transport.kafka

import com.example.platform.core.OutboxMessage

data class OutboxMessageEnvelope(
    val workflowType: String,
    val workflowId: String,
    val message: OutboxMessage,
    val channel: String,
    val headers: Map<String, String>,
    val tenantId: String?,
    val correlationId: String?,
)
