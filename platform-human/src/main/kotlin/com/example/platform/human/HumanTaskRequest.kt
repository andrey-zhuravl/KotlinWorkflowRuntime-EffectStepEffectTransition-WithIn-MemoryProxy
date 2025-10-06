package com.example.platform.human

import com.example.platform.core.CommandMetadata
import com.example.platform.core.HumanInteraction

data class HumanTaskRequest(
    val workflowType: String,
    val workflowId: String,
    val interaction: HumanInteraction,
    val metadata: CommandMetadata,
)
