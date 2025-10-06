package com.example.platform.admin

data class WorkflowStateView(
    val workflowType: String,
    val workflowId: String,
    val state: Any?,
)
