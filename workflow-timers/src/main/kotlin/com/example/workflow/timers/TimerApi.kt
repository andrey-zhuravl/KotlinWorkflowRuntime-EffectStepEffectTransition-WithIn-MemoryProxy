package com.example.workflow.timers

import java.time.Duration
import java.time.Instant

/**
 * API exposed to workflow effects for scheduling durable timers.
 */
interface TimerApi {
    suspend fun schedule(workflowType: String, workflowId: String, key: String, fireAt: Instant, payloadJson: String)
    suspend fun scheduleAfter(workflowType: String, workflowId: String, key: String, delay: Duration, payloadJson: String) {
        schedule(workflowType, workflowId, key, Instant.now().plus(delay), payloadJson)
    }
    suspend fun cancel(workflowType: String, workflowId: String, key: String)
}
