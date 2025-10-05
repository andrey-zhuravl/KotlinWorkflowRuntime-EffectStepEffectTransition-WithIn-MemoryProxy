package com.example.workflow.cluster

import com.example.workflow.transport.kafka.CommandBus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

/**
 * Processes commands for a Kafka partition sequentially.
 */
class PartitionLoop(
    private val partitionId: Int,
    private val commandBus: CommandBus,
    private val runnerFactory: InstanceRunnerFactory,
    private val backpressure: Backpressure
) {
    suspend fun run() {
        commandBus.subscribe()
            .onEach { envelope ->
                val runner = runnerFactory.runnerFor(envelope.workflowId)
                backpressure.awaitSlot()
                runner.offer(envelope)
            }
            .collect()
    }

    fun interface InstanceRunnerFactory {
        fun runnerFor(workflowId: String): InstanceRunner
    }
}
