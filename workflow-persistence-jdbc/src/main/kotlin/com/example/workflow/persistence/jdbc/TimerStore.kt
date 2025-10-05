package com.example.workflow.persistence.jdbc

import java.time.Instant

/**
 * Durable timer queue contract.
 */
interface TimerStore {
    suspend fun schedule(timer: TimerRequest)
    suspend fun cancel(workflowType: String, workflowId: String, timerKey: String)
    suspend fun claimDue(limit: Int = 100): List<TimerRequest>

    data class TimerRequest(
        val workflowType: String,
        val workflowId: String,
        val timerKey: String,
        val payloadJson: String,
        val fireAt: Instant,
        val commandType: String
    )
}
