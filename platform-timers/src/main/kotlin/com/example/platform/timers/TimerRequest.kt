package com.example.platform.timers

import java.time.Duration
import java.time.Instant

data class TimerRequest(
    val workflowType: String,
    val workflowId: String,
    val key: String,
    val fireAt: Instant,
    val payload: Any,
    val metadata: TimerMetadata,
    val cronExpression: String? = null,
    val jitter: Duration? = null,
)
