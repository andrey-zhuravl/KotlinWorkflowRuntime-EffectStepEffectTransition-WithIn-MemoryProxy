package com.example.workflow.subflow

import com.example.workflow.core.StepEffect

data class ChildOptions(val awaitCompletion: Boolean = false, val correlationKey: String? = null)

object SubflowEffects {
    fun <S, E, R> startChild(
        childType: String,
        childId: String,
        initialCommandJson: String,
        options: ChildOptions = ChildOptions()
    ): StepEffect<S, E, R> = StepEffect(
        name = "start-child",
        attributes = buildMap {
            put("childType", childType)
            put("childId", childId)
            put("initialCommandJson", initialCommandJson)
            put("awaitCompletion", options.awaitCompletion)
            options.correlationKey?.let { put("correlationKey", it) }
        }
    )
}

data class ChildWorkflowStatus(
    val parentType: String,
    val parentId: String,
    val childType: String,
    val childId: String,
    val status: Status,
    val resultJson: String?,
    val createdAtEpochMilli: Long
) {
    enum class Status { STARTED, COMPLETED, FAILED, CANCELLED }
}
