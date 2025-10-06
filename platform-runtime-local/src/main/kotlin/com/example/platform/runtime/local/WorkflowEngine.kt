package com.example.platform.runtime.local

import com.example.platform.core.Effect
import com.example.platform.core.Effects
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal data class CommandEnvelope<C, R>(val command: C, val reply: CompletableDeferred<R?>)

/**
 * Executes workflow commands using an in-memory, single-threaded per-instance runtime.
 */
public class WorkflowEngine<S, C, E, R>(
    private val workflow: Workflow<S, C, E, R>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val store = InMemoryEventStore<S, E> { workflow.initialState(it) }
    private val mailboxes = ConcurrentHashMap<String, Channel<CommandEnvelope<C, R>>>()

    /** Registers the provided [workflow] ensuring it matches the engine binding. */
    public fun register(workflow: Workflow<S, C, E, R>) {
        if (workflow::class != this.workflow::class) {
            error("WorkflowEngine already bound to ${this.workflow::class.qualifiedName}")
        }
    }

    /** Creates a proxy for the given [workflow] and instance [id]. */
    public fun createProxy(workflow: Workflow<S, C, E, R>, id: String): WorkflowProxy<S, C, E, R> {
        register(workflow)
        return proxyFor(id)
    }

    /** Returns a proxy bound to the specified workflow instance identifier. */
    public fun proxyFor(id: String): WorkflowProxy<S, C, E, R> {
        val mailbox = mailboxes.computeIfAbsent(id) { createMailbox(id) }
        return WorkflowProxy(id, mailbox, store)
    }

    private fun createMailbox(id: String): Channel<CommandEnvelope<C, R>> {
        val channel = Channel<CommandEnvelope<C, R>>(Channel.UNLIMITED)
        scope.launch { process(id, channel) }
        return channel
    }

    private suspend fun process(id: String, channel: Channel<CommandEnvelope<C, R>>) {
        var currentState = store.state(id)
        val ctx = object : WorkflowContext<S, C, E, R> {
            override val id: String = id
            override val effects: Effects = Effects
            override fun now(): Instant = Instant.now()
        }
        for (envelope in channel) {
            val effect = try {
                workflow.onCommand(currentState, envelope.command, ctx)
            } catch (t: Throwable) {
                envelope.reply.completeExceptionally(t)
                continue
            }
            val result = when (effect) {
                is Effect.Result -> effect
            }
            val events = result.events
            if (events.isNotEmpty()) {
                store.appendEvents(id, events)
                for (event in events) {
                    currentState = workflow.applyEvent(currentState, event)
                }
            }
            result.transition?.let { transition ->
                currentState = transition.toState(currentState)
            }
            store.updateState(id, currentState)
            try {
                for (sideEffect in result.sideEffects) {
                    sideEffect(currentState)
                }
            } catch (t: Throwable) {
                envelope.reply.completeExceptionally(t)
                continue
            }
            try {
                val replyValue = result.reply?.invoke(currentState)
                envelope.reply.complete(replyValue)
            } catch (t: Throwable) {
                envelope.reply.completeExceptionally(t)
            }
        }
    }
}
