package com.example.workflow.transport.kafka

import kotlinx.coroutines.flow.Flow

/**
 * Internal signalling channel for cluster coordination.
 */
interface SignalBus {
    suspend fun publish(signal: Signal)
    fun subscribe(): Flow<Signal>
}

data class Signal(
    val type: SignalType,
    val payload: String? = null
)

enum class SignalType {
    REBALANCE,
    SHUTDOWN
}
