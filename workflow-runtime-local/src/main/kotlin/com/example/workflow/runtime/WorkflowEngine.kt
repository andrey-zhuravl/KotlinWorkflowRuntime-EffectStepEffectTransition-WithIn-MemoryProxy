package com.example.workflow.runtime

import com.example.workflow.core.DefaultWorkflowContext
import com.example.workflow.core.Effect
import com.example.workflow.core.Workflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel

/**
 * In-memory workflow engine responsible for executing workflow logic sequentially per instance id.
 */
class WorkflowEngine<S, C, E, R>(
    private val workflow: Workflow<S, C, E, R>,
    scope: CoroutineScope? = null
) {
    private val ownsScope = scope == null
    private val scope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateStore = InMemoryStateStore(workflow::initialState)
    private val eventStore = InMemoryEventStore<E>()

    internal data class CommandEnvelope<C, R>(
        val command: C,
        val reply: CompletableDeferred<R>?,
        val expectReply: Boolean,
        val completion: CompletableDeferred<Unit>
    )

    private data class WorkflowInstance<C, R>(
        val channel: Channel<CommandEnvelope<C, R>>,
        val job: kotlinx.coroutines.Job
    )

    private val instances = java.util.concurrent.ConcurrentHashMap<String, WorkflowInstance<C, R>>()

    fun proxy(id: String): WorkflowProxy<S, C, E, R> {
        ensureInstance(id)
        return WorkflowProxy(id, this)
    }

    internal suspend fun submit(id: String, envelope: CommandEnvelope<C, R>) {
        val instance = ensureInstance(id)
        instance.channel.send(envelope)
    }

    private fun ensureInstance(id: String): WorkflowInstance<C, R> =
        instances.computeIfAbsent(id) {
            val channel = Channel<CommandEnvelope<C, R>>(Channel.UNLIMITED)
            val job = scope.launch {
                for (envelope in channel) {
                    processEnvelope(id, envelope)
                }
            }
            WorkflowInstance(channel, job)
        }

    private suspend fun processEnvelope(id: String, envelope: CommandEnvelope<C, R>) {
        val completion = envelope.completion
        val reply = envelope.reply
        var failure: Throwable? = null
        try {
            val currentState = stateStore.getState(id)
            val context = DefaultWorkflowContext<S, C, E, R>(id)
            val effect = workflow.onCommand(currentState, envelope.command, context)
            val executable = effect as? Effect.Executable<S, E, R>
                ?: error("Unsupported effect implementation: $effect")
            val newState = applyEffect(id, currentState, executable)
            when {
                envelope.expectReply && executable.replyType == Effect.ReplyType.NO_REPLY -> {
                    failure = IllegalStateException(
                        "Workflow ${workflow.name} returned no reply for command ${envelope.command}"
                    )
                    reply?.completeExceptionally(failure!!)
                }
                envelope.expectReply -> {
                    val replyFn = executable.reply ?: error("Reply function missing")
                    val result = replyFn(newState)
                    reply?.complete(result)
                }
                else -> {
                    executable.reply?.invoke(newState)
                }
            }
        } catch (ex: Throwable) {
            reply?.completeExceptionally(ex)
            completion.completeExceptionally(ex)
            return
        }
        failure?.let {
            completion.completeExceptionally(it)
            return
        }
        completion.complete(Unit)
    }

    private suspend fun applyEffect(
        id: String,
        currentState: S,
        effect: Effect.Executable<S, E, R>
    ): S {
        var newState = currentState
        if (effect.events.isNotEmpty()) {
            eventStore.append(id, effect.events)
            for (event in effect.events) {
                newState = workflow.applyEvent(newState, event)
            }
        }
        effect.transition?.let { transition ->
            newState = transition.toState(newState)
        }
        stateStore.updateState(id, newState)
        if (effect.timers.isNotEmpty()) {
            println("[workflow-runtime-local] ignoring ${effect.timers.size} timer instruction(s) for $id in local engine")
        }
        if (effect.outboxMessages.isNotEmpty()) {
            println("[workflow-runtime-local] ignoring ${effect.outboxMessages.size} outbox message(s) for $id in local engine")
        }
        for (sideEffect in effect.sideEffects) {
            sideEffect(newState)
        }
        return newState
    }

    internal fun stateSnapshot(id: String): S = stateStore.getState(id)

    internal fun eventLog(id: String): List<E> = eventStore.events(id)

    suspend fun shutdown() {
        instances.values.forEach { it.channel.close() }
        instances.values.forEach { it.job.join() }
        if (ownsScope) {
            scope.cancel()
        }
        instances.clear()
    }
}
