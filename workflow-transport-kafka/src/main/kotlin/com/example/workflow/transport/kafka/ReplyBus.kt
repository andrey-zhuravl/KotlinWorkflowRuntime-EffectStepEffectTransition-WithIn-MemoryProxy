package com.example.workflow.transport.kafka

import kotlinx.coroutines.flow.Flow

/**
 * Request/Reply response transport abstraction.
 */
interface ReplyBus {
    suspend fun publish(reply: ReplyEnvelope)
    fun subscribe(): Flow<ReplyEnvelope>
}

data class ReplyEnvelope(
    val correlationId: String,
    val replyType: String,
    val replyJson: String,
    val status: ReplyStatus
)

enum class ReplyStatus { OK, ERROR }
