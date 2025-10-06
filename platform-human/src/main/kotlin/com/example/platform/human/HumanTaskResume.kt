package com.example.platform.human

import com.example.platform.core.CommandMetadata

data class HumanTaskResume(
    val workflowType: String,
    val workflowId: String,
    val token: String,
    val payload: Any?,
    val metadata: CommandMetadata,
)
