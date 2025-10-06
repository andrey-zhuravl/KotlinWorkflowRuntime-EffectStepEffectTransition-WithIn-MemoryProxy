package com.example.platform.core

import java.time.Duration

data class ChildWorkflowOptions(
    val startDelay: Duration? = null,
    val retryStrategy: RetryStrategy? = null,
)
