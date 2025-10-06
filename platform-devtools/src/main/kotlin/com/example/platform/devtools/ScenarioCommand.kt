package com.example.platform.devtools

import com.example.platform.core.CommandMetadata

data class ScenarioCommand(
    val workflowType: String,
    val workflowId: String,
    val command: Any,
    val metadata: CommandMetadata = CommandMetadata(),
)
