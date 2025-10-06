package com.example.platform.core

data class TimerCommand<C>(
    val command: C,
    val targetWorkflowType: String? = null,
    val targetWorkflowId: String? = null,
    val metadata: CommandMetadata = CommandMetadata(),
)
