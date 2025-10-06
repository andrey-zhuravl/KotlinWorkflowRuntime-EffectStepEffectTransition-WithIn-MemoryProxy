package com.example.platform.core

import java.time.Duration
import java.time.Instant

sealed interface TimerInstruction {
    data class At(val key: String, val fireAt: Instant, val payload: Any) : TimerInstruction
    data class After(val key: String, val delay: Duration, val payload: Any) : TimerInstruction
    data class Cron(val key: String, val expression: String, val payload: Any, val jitter: Duration?) : TimerInstruction
    data class Cancel(val key: String) : TimerInstruction
}
