package com.example.workflow.timers

import com.example.workflow.core.StepEffect
import kotlin.time.Duration

object TimerEffects {
    fun <S, E, R> scheduleCron(
        key: String,
        cron: String,
        jitter: Duration? = null,
        tags: Set<String> = emptySet()
    ): StepEffect<S, E, R> = StepEffect(
        name = "schedule-cron",
        attributes = buildMap {
            put("key", key)
            put("cron", cron)
            jitter?.let { put("jitterMillis", it.inWholeMilliseconds) }
            if (tags.isNotEmpty()) put("tags", tags.toList())
        }
    )

    fun <S, E, R> massReschedule(selector: TimerSelector, shift: Duration): StepEffect<S, E, R> = StepEffect(
        name = "mass-reschedule",
        attributes = buildMap {
            selector.prefix?.let { put("prefix", it) }
            if (selector.tags.isNotEmpty()) put("tags", selector.tags.toList())
            put("shiftMillis", shift.inWholeMilliseconds)
        }
    )
}

data class TimerSelector(val prefix: String? = null, val tags: Set<String> = emptySet())
