package com.example.workflow.core

/**
 * Represents the executable intent returned by a workflow command handler.
 */
sealed class Effect<S, E, R> {
    internal data class Executable<S, E, R>(
        val events: List<E>,
        val transition: Transition<S>?,
        val sideEffects: List<SideEffect<S>>,
        val reply: ((S) -> R)?,
        val replyType: ReplyType
    ) : Effect<S, E, R>()

    internal enum class ReplyType { WITH_REPLY, NO_REPLY }
}

/**
 * Builder returned by DSL methods while composing workflow effects.
 */
interface StepEffect<S, E, R> {
    fun thenRun(block: SideEffect<S>): StepEffect<S, E, R>
    fun thenTransition(transition: (S) -> S): StepEffect<S, E, R>
    fun thenReply(reply: (S) -> R): Effect<S, E, R>
    fun thenNoReply(): Effect<S, E, R>
}

internal class StepEffectImpl<S, E, R>(
    private val events: List<E>,
    private val sideEffects: List<SideEffect<S>>,
    private val transition: Transition<S>?
) : StepEffect<S, E, R> {

    override fun thenRun(block: SideEffect<S>): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects + block, transition)

    override fun thenTransition(transition: (S) -> S): StepEffect<S, E, R> {
        val next = Transition(transition)
        val composed = this.transition?.let { existing ->
            Transition<S> { state -> next.toState(existing.toState(state)) }
        } ?: next
        return StepEffectImpl(events, sideEffects, composed)
    }

    override fun thenReply(reply: (S) -> R): Effect<S, E, R> =
        Effect.Executable(events, transition, sideEffects, reply, Effect.ReplyType.WITH_REPLY)

    override fun thenNoReply(): Effect<S, E, R> =
        Effect.Executable(events, transition, sideEffects, null, Effect.ReplyType.NO_REPLY)
}
