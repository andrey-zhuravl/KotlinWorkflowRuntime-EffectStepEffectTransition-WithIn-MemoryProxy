package com.example.platform.security

import com.example.platform.core.CommandMetadata

data class AuthorizationContext(
    val workflowType: String,
    val workflowId: String,
    val command: Any,
    val metadata: CommandMetadata,
)
