package com.example.platform.timers

import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface TimerConsumer {
    suspend fun onTimer(timer: ScheduledTimer)
}

data class TimerMetadata(
    val tenantId: String?,
    val correlationId: String?,
)

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

data class ScheduledTimer(
    val workflowType: String,
    val workflowId: String,
    val key: String,
    val fireAt: Instant,
    val payload: Any,
    val metadata: TimerMetadata,
    val cronExpression: String? = null,
    val jitter: Duration? = null,
)

interface TimerScheduler {
    fun registerConsumer(consumer: TimerConsumer)
    suspend fun schedule(request: TimerRequest)
    suspend fun cancel(workflowType: String, workflowId: String, key: String)
    suspend fun advanceTo(instant: Instant)
    suspend fun pendingTimers(): List<ScheduledTimer>
}

class InMemoryTimerScheduler : TimerScheduler {
    private val mutex = Mutex()
    private val timers = mutableListOf<ScheduledTimer>()
    private var consumer: TimerConsumer? = null

    override fun registerConsumer(consumer: TimerConsumer) {
        this.consumer = consumer
    }

    override suspend fun schedule(request: TimerRequest) {
        mutex.withLock {
            val jittered = request.jitter?.let { jitter ->
                val millis = Random.Default.nextLong(0, jitter.toMillis() + 1)
                request.fireAt.plusMillis(millis)
            } ?: request.fireAt
            val timer = ScheduledTimer(
                workflowType = request.workflowType,
                workflowId = request.workflowId,
                key = request.key,
                fireAt = jittered,
                payload = request.payload,
                metadata = request.metadata,
                cronExpression = request.cronExpression,
                jitter = request.jitter,
            )
            timers.removeIf { existing ->
                existing.workflowType == timer.workflowType &&
                    existing.workflowId == timer.workflowId &&
                    existing.key == timer.key &&
                    existing.cronExpression == null
            }
            timers += timer
            timers.sortBy { it.fireAt }
        }
    }

    override suspend fun cancel(workflowType: String, workflowId: String, key: String) {
        mutex.withLock {
            timers.removeIf { it.workflowType == workflowType && it.workflowId == workflowId && it.key == key }
        }
    }

    override suspend fun advanceTo(instant: Instant) {
        val due = mutableListOf<ScheduledTimer>()
        mutex.withLock {
            val iterator = timers.iterator()
            while (iterator.hasNext()) {
                val timer = iterator.next()
                if (!timer.fireAt.isAfter(instant)) {
                    due += timer
                    iterator.remove()
                }
            }
        }
        val consumer = consumer ?: return
        for (timer in due.sortedBy { it.fireAt }) {
            consumer.onTimer(timer)
            if (timer.cronExpression != null) {
                val next = computeNext(timer)
                if (next != null) {
                    schedule(next)
                }
            }
        }
    }

    override suspend fun pendingTimers(): List<ScheduledTimer> = mutex.withLock { timers.toList() }

    private fun computeNext(timer: ScheduledTimer): TimerRequest? {
        val interval = parseInterval(timer.cronExpression!!) ?: return null
        return TimerRequest(
            workflowType = timer.workflowType,
            workflowId = timer.workflowId,
            key = timer.key,
            fireAt = timer.fireAt.plus(interval),
            payload = timer.payload,
            metadata = timer.metadata,
            cronExpression = timer.cronExpression,
            jitter = timer.jitter,
        )
    }

    private fun parseInterval(expression: String): Duration? {
        val trimmed = expression.trim()
        if (trimmed.startsWith("@every")) {
            val amount = trimmed.removePrefix("@every").trim()
            return parseDuration(amount)
        }
        return null
    }

    private fun parseDuration(value: String): Duration? {
        return when {
            value.endsWith("ms") -> Duration.ofMillis(value.removeSuffix("ms").toLong())
            value.endsWith("s") -> Duration.ofSeconds(value.removeSuffix("s").toLong())
            value.endsWith("m") -> Duration.ofMinutes(value.removeSuffix("m").toLong())
            value.endsWith("h") -> Duration.ofHours(value.removeSuffix("h").toLong())
            else -> null
        }
    }
}
