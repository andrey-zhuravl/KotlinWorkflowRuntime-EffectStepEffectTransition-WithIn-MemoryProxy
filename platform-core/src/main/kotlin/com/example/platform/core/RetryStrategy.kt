package com.example.platform.core

import java.time.Duration

data class RetryStrategy(
    val maxAttempts: Int,
    val initialDelay: Duration,
    val multiplier: Double = 2.0,
)
