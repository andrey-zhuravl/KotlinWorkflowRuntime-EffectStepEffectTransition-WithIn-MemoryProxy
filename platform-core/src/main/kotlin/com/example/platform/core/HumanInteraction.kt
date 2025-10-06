package com.example.platform.core

import java.time.Duration

data class HumanInteraction(
    val token: String,
    val ttl: Duration,
    val payload: Any,
    val notifyChannel: String? = null,
)
