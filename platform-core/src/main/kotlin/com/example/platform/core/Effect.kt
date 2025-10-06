package com.example.platform.core

/**
 * Represents the outcome of handling a workflow command.
 */
public sealed class Effect<S, E, R> {
    /** Terminal effect produced after composing events, transitions, side-effects and reply. */
    public data class Result<S, E, R>(
        val events: List<E>,
        val transition: Transition<S>?,
        val sideEffects: List<suspend (S) -> Unit>,
        val reply: ((S) -> R)?
    ) : Effect<S, E, R>()
}

/**
 * Builder for composing workflow side-effects before producing a terminal [Effect].
 */
public class StepEffect<S, E, R> internal constructor(
    internal val events: List<E>,
    internal val transition: Transition<S>?,
    internal val sideEffects: List<suspend (S) -> Unit>
) {
    /** Adds a suspending side-effect to execute after the state is finalized. */
    public fun thenRun(action: suspend (S) -> Unit): StepEffect<S, E, R> =
        StepEffect(events, transition, sideEffects + action)

    /** Queues a state transition to run after events have been applied. */
    public fun thenTransition(transform: (S) -> S): StepEffect<S, E, R> =
        StepEffect(events, Transition(transform), sideEffects)

    /** Finishes the effect replying with a value derived from the latest state. */
    public fun thenReply(responder: (S) -> R): Effect<S, E, R> =
        Effect.Result(events, transition, sideEffects, responder)

    /** Finishes the effect without replying. */
    public fun thenNoReply(): Effect<S, E, R> =
        Effect.Result(events, transition, sideEffects, null)
}
