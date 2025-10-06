package com.example.platform.core

data class OutboxMessage(
    val id: String,
    val payload: Any,
    val correlationId: String? = null,
    val tenantId: String? = null,
)
