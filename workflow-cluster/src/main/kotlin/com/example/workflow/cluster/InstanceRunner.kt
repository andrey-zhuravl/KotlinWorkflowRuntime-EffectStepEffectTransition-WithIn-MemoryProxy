package com.example.workflow.cluster

import com.example.workflow.transport.kafka.CommandEnvelope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

/**
 * Executes commands for a single workflow instance using an internal mailbox.
 */
class InstanceRunner(
    private val workflowId: String,
    private val handler: suspend (CommandEnvelope) -> Unit
) {
    private val mailbox = Channel<CommandEnvelope>(Channel.UNLIMITED)

    suspend fun start() {
        mailbox.consumeEach { handler(it) }
    }

    suspend fun offer(command: CommandEnvelope) {
        mailbox.send(command)
    }
}
