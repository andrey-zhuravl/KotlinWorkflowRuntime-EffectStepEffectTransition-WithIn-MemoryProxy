package com.example.platform.runtime

import com.example.platform.core.WorkflowLogger

internal class StdoutWorkflowLogger(
    private val workflowType: String,
    private val workflowId: String,
) : WorkflowLogger {
    override fun debug(message: String) {
        println("[${'$'}workflowType:${'$'}workflowId] DEBUG ${'$'}message")
    }

    override fun info(message: String) {
        println("[${'$'}workflowType:${'$'}workflowId] INFO ${'$'}message")
    }

    override fun warn(message: String, throwable: Throwable?) {
        println("[${'$'}workflowType:${'$'}workflowId] WARN ${'$'}message")
        throwable?.printStackTrace()
    }

    override fun error(message: String, throwable: Throwable?) {
        System.err.println("[${'$'}workflowType:${'$'}workflowId] ERROR ${'$'}message")
        throwable?.printStackTrace()
    }
}
