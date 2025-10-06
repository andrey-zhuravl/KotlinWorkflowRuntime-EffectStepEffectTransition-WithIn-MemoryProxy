package com.example.platform.runtime

import com.example.platform.core.WorkflowLogger

fun interface WorkflowLoggerFactory {
    fun create(workflowType: String, workflowId: String): WorkflowLogger

    companion object {
        fun stdout(): WorkflowLoggerFactory = WorkflowLoggerFactory { type, id ->
            StdoutWorkflowLogger(type, id)
        }
    }
}
