package com.example.workflow.admin

import com.example.workflow.core.StepEffect

data class WorkflowStateResponse(
    val workflowType: String,
    val workflowId: String,
    val stateJson: String,
    val lastSequence: Long
)

data class EventsResponse(val eventsJson: List<String>)

data class TimerDescriptor(
    val key: String,
    val dueAtEpochMilli: Long,
    val cronExpr: String?,
    val jitterMs: Int?,
    val tags: Set<String>
)

data class ProjectionOffset(val name: String, val journalSeq: Long)

data class DlqMoveRequest(val topic: String, val offset: Long)

data class ReplayRequest(val fromSeq: Long?, val toSeq: Long?)

interface AdminService {
    suspend fun getWorkflow(type: String, id: String): WorkflowStateResponse?
    suspend fun listEvents(type: String, id: String, fromSequence: Long?): EventsResponse
    suspend fun replayWorkflow(type: String, id: String, request: ReplayRequest): StepEffect<*, *, *>?
    suspend fun moveToDlq(topic: String, offset: Long): StepEffect<*, *, *>?
    suspend fun listTimers(dueBeforeEpochMilli: Long): List<TimerDescriptor>
    suspend fun rescheduleTimers(selector: TimerSelectorRequest): Int
    suspend fun getProjectionOffset(name: String): ProjectionOffset?
}

data class TimerSelectorRequest(
    val prefix: String? = null,
    val tags: Set<String> = emptySet(),
    val shiftMillis: Long = 0
)
