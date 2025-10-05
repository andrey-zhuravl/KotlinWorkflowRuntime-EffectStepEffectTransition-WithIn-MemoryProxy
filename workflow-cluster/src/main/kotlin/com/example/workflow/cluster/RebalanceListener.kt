package com.example.workflow.cluster

/**
 * Hook used to coordinate Kafka rebalance events with the workflow runtime.
 */
fun interface RebalanceListener {
    suspend fun onPartitionsRevoked(partitionIds: Set<Int>)
}
