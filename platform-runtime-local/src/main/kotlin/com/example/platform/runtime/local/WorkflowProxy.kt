package com.example.platform.runtime.local

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

/**
 * Client-facing proxy for interacting with a workflow instance.
 */
public class WorkflowProxy<S, C, E, R> internal constructor(
    private val id: String,
    private val mailbox: Channel<CommandEnvelope<C, R>>,
    private val store: InMemoryEventStore<S, E>
) {
    /** Sends the command expecting a reply. */
    public suspend fun ask(command: C): R {
        val deferred = CompletableDeferred<R?>()
        mailbox.send(CommandEnvelope(command, deferred))
        val result = deferred.await()
        return result ?: error("Workflow $id returned no reply for command $command")
    }

    /** Sends the command without awaiting a reply value. */
    public suspend fun tell(command: C) {
        val deferred = CompletableDeferred<R?>()
        mailbox.send(CommandEnvelope(command, deferred))
        deferred.await()
    }

    /** Returns the latest known state. */
    public fun state(): S = store.state(id)

    /** Returns all persisted events for this instance. */
    public fun events(): List<E> = store.events(id)
}
