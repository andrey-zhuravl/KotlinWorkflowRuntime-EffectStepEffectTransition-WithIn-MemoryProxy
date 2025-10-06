package com.example.platform.core

data class ChildWorkflowStart(
    val workflowType: String,
    val workflowId: String,
    val initialCommand: Any,
    val options: ChildWorkflowOptions = ChildWorkflowOptions(),
)
