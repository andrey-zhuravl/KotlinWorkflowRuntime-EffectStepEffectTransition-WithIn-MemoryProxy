package com.example.workflow.saga

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Retry policy description for saga steps.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds
)
