package com.example.workflow.core

enum class Priority { HIGH, NORMAL, LOW }

data class CommandEnvelope<C>(
    val workflowType: String,
    val workflowId: String,
    val command: C,
    val priority: Priority = Priority.NORMAL,
    val tenantId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
