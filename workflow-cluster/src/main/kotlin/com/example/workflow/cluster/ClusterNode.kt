package com.example.workflow.cluster

import com.example.workflow.transport.kafka.CommandBus
import com.example.workflow.transport.kafka.ReplyBus
import com.example.workflow.transport.kafka.SignalBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Entry point for a distributed workflow runtime node.
 */
class ClusterNode(
    private val scope: CoroutineScope,
    private val commandBus: CommandBus,
    private val replyBus: ReplyBus,
    private val signalBus: SignalBus,
    private val partitionLoopFactory: PartitionLoopFactory
) {
    private val jobs = mutableListOf<Job>()

    fun start(partitions: Int) {
        repeat(partitions) { partitionId ->
            val loop = partitionLoopFactory.create(partitionId)
            jobs += scope.launch { loop.run() }
        }
    }

    suspend fun stop() {
        jobs.forEach { it.cancel() }
        signalBus.publish(com.example.workflow.transport.kafka.Signal(com.example.workflow.transport.kafka.SignalType.SHUTDOWN))
    }

    fun interface PartitionLoopFactory {
        fun create(partitionId: Int): PartitionLoop
    }
}
