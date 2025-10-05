package com.example.workflow.transport.kafka

import com.example.workflow.core.Workflow
import kotlinx.coroutines.flow.Flow

/**
 * Command transport facade for distributed workflow execution.
 */
interface CommandBus {
    suspend fun publish(command: CommandEnvelope)
    fun subscribe(): Flow<CommandEnvelope>
}

/**
 * Envelope definition for commands targeting workflow instances.
 */
data class CommandEnvelope(
    val workflowType: String,
    val workflowId: String,
    val commandType: String,
    val commandJson: String,
    val commandId: String,
    val correlationId: String,
    val replyTo: String?
) {
    val key: String get() = "$workflowType:$workflowId"
}

/**
 * Convenience to derive a workflow type name when not explicitly provided.
 */
fun Workflow<*, *, *, *>.commandTopic(): String = name
