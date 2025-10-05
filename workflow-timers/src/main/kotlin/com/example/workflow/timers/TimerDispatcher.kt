package com.example.workflow.timers

import com.example.workflow.persistence.jdbc.TimerStore
import com.example.workflow.transport.kafka.CommandBus
import com.example.workflow.transport.kafka.CommandEnvelope
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/**
 * Background component that polls the timer store for due timers and emits commands.
 */
class TimerDispatcher(
    private val clock: Clock,
    private val timerStore: TimerStore,
    private val commandBus: CommandBus,
    private val pollIntervalMillis: Long = 500L
) {
    private val running = AtomicBoolean(false)

    suspend fun run() {
        running.set(true)
        while (running.get()) {
            val due = timerStore.claimDue()
            for (timer in due) {
                val commandId = "${'$'}{timer.workflowId}:${'$'}{timer.timerKey}:${'$'}{timer.fireAt.toEpochMilli()}"
                val envelope = CommandEnvelope(
                    workflowType = timer.workflowType,
                    workflowId = timer.workflowId,
                    commandType = timer.commandType,
                    commandJson = timer.payloadJson,
                    commandId = commandId,
                    correlationId = commandId,
                    replyTo = null
                )
                commandBus.publish(envelope)
            }
            delay(pollIntervalMillis)
        }
    }

    fun stop() {
        running.set(false)
    }
}
