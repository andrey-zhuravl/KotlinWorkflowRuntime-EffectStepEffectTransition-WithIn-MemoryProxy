package com.example.platform.runtime

import com.example.platform.core.CommandMetadata

data class CommandEnvelope<C : Any>(
    val workflowType: String,
    val workflowId: String,
    val command: C,
    val metadata: CommandMetadata = CommandMetadata(),
)
