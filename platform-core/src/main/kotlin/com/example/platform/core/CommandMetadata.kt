package com.example.platform.core

data class CommandMetadata(
    val commandId: String? = null,
    val correlationId: String? = null,
    val tenantId: String? = null,
    val priority: CommandPriority = CommandPriority.NORMAL,
)
