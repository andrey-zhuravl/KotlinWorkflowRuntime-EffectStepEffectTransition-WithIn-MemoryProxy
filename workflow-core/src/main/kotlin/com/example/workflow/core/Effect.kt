package com.example.workflow.core

import java.time.Duration
import java.time.Instant

/**
 * Represents the executable intent returned by a workflow command handler.
 */
sealed class Effect<S, E, R> {
    internal data class Executable<S, E, R>(
        val events: List<E>,
        val transition: Transition<S>?,
        val sideEffects: List<SideEffect<S>>,
        val reply: ((S) -> R)?,
        val replyType: ReplyType,
        val timers: List<TimerInstruction>,
        val outboxMessages: List<OutboxMessage>
    ) : Effect<S, E, R>()

    internal enum class ReplyType { WITH_REPLY, NO_REPLY }

    sealed class TimerInstruction {
        data class Schedule(val key: String, val at: Instant, val payload: Any) : TimerInstruction()
        data class ScheduleAfter(val key: String, val delay: Duration, val payload: Any) : TimerInstruction()
        data class Cancel(val key: String) : TimerInstruction()
    }

    data class OutboxMessage(val channel: String, val payload: Any)
}

/**
 * Builder returned by DSL methods while composing workflow effects.
 */
interface StepEffect<S, E, R> {
    fun thenRun(block: SideEffect<S>): StepEffect<S, E, R>
    fun thenTransition(transition: (S) -> S): StepEffect<S, E, R>
    fun schedule(key: String, at: Instant, payload: Any): StepEffect<S, E, R>
    fun scheduleAfter(key: String, delay: Duration, payload: Any): StepEffect<S, E, R>
    fun cancelTimer(key: String): StepEffect<S, E, R>
    fun outbox(channel: String, message: Any): StepEffect<S, E, R>
    fun thenReply(reply: (S) -> R): Effect<S, E, R>
    fun thenNoReply(): Effect<S, E, R>
}

internal class StepEffectImpl<S, E, R> internal constructor(
    private val events: List<E>,
    private val sideEffects: List<SideEffect<S>>,
    private val transition: Transition<S>?,
    private val timers: List<Effect.TimerInstruction>,
    private val outbox: List<Effect.OutboxMessage>
) : StepEffect<S, E, R> {

    override fun thenRun(block: SideEffect<S>): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects + block, transition, timers, outbox)

    override fun thenTransition(transition: (S) -> S): StepEffect<S, E, R> {
        val next = Transition(transition)
        val composed = this.transition?.let { existing ->
            Transition<S> { state -> next.toState(existing.toState(state)) }
        } ?: next
        return StepEffectImpl(events, sideEffects, composed, timers, outbox)
    }

    override fun schedule(key: String, at: Instant, payload: Any): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects, transition, timers + Effect.TimerInstruction.Schedule(key, at, payload), outbox)

    override fun scheduleAfter(key: String, delay: Duration, payload: Any): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects, transition, timers + Effect.TimerInstruction.ScheduleAfter(key, delay, payload), outbox)

    override fun cancelTimer(key: String): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects, transition, timers + Effect.TimerInstruction.Cancel(key), outbox)

    override fun outbox(channel: String, message: Any): StepEffect<S, E, R> =
        StepEffectImpl(events, sideEffects, transition, timers, outbox + Effect.OutboxMessage(channel, message))

    override fun thenReply(reply: (S) -> R): Effect<S, E, R> =
        Effect.Executable(events, transition, sideEffects, reply, Effect.ReplyType.WITH_REPLY, timers, outbox)

    override fun thenNoReply(): Effect<S, E, R> =
        Effect.Executable(events, transition, sideEffects, null, Effect.ReplyType.NO_REPLY, timers, outbox)
}
