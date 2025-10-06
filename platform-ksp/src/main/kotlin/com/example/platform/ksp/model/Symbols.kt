package com.example.platform.ksp.model

import java.io.File

internal object Symbols {
    const val WORKFLOW_INTERFACE = "Workflow"
    const val WORKFLOW_SERVICE = "WorkflowService"
}

internal data class WorkflowBinding(
    val packageName: String,
    val className: String,
    val serviceName: String,
    val stateType: String,
    val commandType: String,
    val eventType: String,
    val replyType: String,
    val stateFqn: String,
    val commandFqn: String,
    val eventFqn: String,
    val replyFqn: String,
    val sourceFile: File
)
