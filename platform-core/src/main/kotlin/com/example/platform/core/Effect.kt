package com.example.platform.core

/**
 * Represents the frozen execution plan produced by a workflow command handler.
 *
 * An [Effect] records the full sequence of work that the runtime must execute in
 * the following strict order:
 *
 * 1. Persist all [Result.events] to durable storage.
 * 2. Apply each event to the current state via [Workflow.applyEvent] in order.
 * 3. Apply the optional [Result.transition] to produce the final state.
 * 4. Execute all queued side-effects with the latest state.
 * 5. Produce the optional reply by invoking [Result.reply] with the final state.
 */
public sealed class Effect<S, E, R> {
    /**
     * Terminal effect produced after composing events, an optional transition,
     * side-effects and an optional reply function.
     */
    public data class Result<S, E, R>(
        val events: List<E>,
        val transition: Transition<S>?,
        val sideEffects: List<suspend (S) -> Unit>,
        val reply: ((S) -> R)?
    ) : Effect<S, E, R>()
}

/**
 * Mutable-style builder used by workflow handlers to compose an [Effect].
 *
 * Implementations accumulate emitted events, a single state [Transition],
 * zero or more side-effects and finally a terminal reply decision. Once a
 * terminal method is invoked the builder yields an immutable [Effect.Result].
 */
public interface StepEffect<S, E, R> {
    /**
     * Queues a suspending side-effect that will run after the state is finalized.
     */
    public fun thenRun(action: suspend (S) -> Unit): StepEffect<S, E, R>

    /**
     * Schedules the single transition to run after events have been applied.
     *
     * @throws IllegalStateException if a transition was already provided.
     */
    public fun thenTransition(transform: (S) -> S): StepEffect<S, E, R>

    /**
     * Finishes the effect replying with a value derived from the latest state.
     */
    public fun thenReply(responder: (S) -> R): Effect<S, E, R>

    /**
     * Finishes the effect without providing a reply.
     */
    public fun thenNoReply(): Effect<S, E, R>
}

internal class NormalStepEffectImpl<S, E, R> (
    private val events: List<E>,
    private val transition: Transition<S>?,
    private val sideEffects: List<suspend (S) -> Unit>
) : StepEffect<S, E, R> {
    override fun thenRun(action: suspend (S) -> Unit): StepEffect<S, E, R> =
        NormalStepEffectImpl(events, transition, sideEffects + action)

    override fun thenTransition(transform: (S) -> S): StepEffect<S, E, R> {
        if (transition != null) {
            throw IllegalStateException("Transition already set")
        }
        return NormalStepEffectImpl(events, Transition(transform), sideEffects)
    }

    override fun thenReply(responder: (S) -> R): Effect<S, E, R> =
        freeze(responder)

    override fun thenNoReply(): Effect<S, E, R> =
        freeze(null)

    private fun freeze(reply: ((S) -> R)?): Effect<S, E, R> =
        Effect.Result(events, transition, sideEffects, reply)
}

internal class RejectedStepEffectImpl<S, E, R>(
    private val rejectionReply: (S) -> R
) : StepEffect<S, E, R> {
    private val frozen: Effect<S, E, R> =
        Effect.Result(emptyList(), null, emptyList(), rejectionReply)

    override fun thenRun(action: suspend (S) -> Unit): StepEffect<S, E, R> = this

    override fun thenTransition(transform: (S) -> S): StepEffect<S, E, R> = this

    override fun thenReply(responder: (S) -> R): Effect<S, E, R> = frozen

    override fun thenNoReply(): Effect<S, E, R> = frozen
}
