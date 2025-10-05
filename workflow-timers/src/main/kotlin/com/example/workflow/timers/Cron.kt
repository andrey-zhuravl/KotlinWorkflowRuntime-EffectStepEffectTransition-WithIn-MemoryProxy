package com.example.workflow.timers

import java.time.Instant

/**
 * Minimal cron expression helper used by timer scheduling.
 */
class Cron(private val expression: String) {
    init {
        require(expression.trim().split(" ").size in 5..6) { "Unsupported cron expression: $expression" }
    }

    fun next(after: Instant): Instant {
        // Placeholder implementation; production code will parse cron expression properly.
        return after.plusSeconds(60)
    }
}
