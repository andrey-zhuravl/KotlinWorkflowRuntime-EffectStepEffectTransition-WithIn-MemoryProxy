package com.example.platform.timers

import java.time.Instant

interface TimerScheduler {
    fun registerConsumer(consumer: TimerConsumer)
    suspend fun schedule(request: TimerRequest)
    suspend fun cancel(workflowType: String, workflowId: String, key: String)
    suspend fun advanceTo(instant: Instant)
    suspend fun pendingTimers(): List<ScheduledTimer>
}
