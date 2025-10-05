package com.example.workflow.core

import java.time.Duration
import java.time.Instant

/**
 * Entry point for building workflow effects in a fluent DSL.
 */
object Effects {
    /**
     * Creates a [StepEffect] without persisting any events.
     */
    fun <S, E, R> none(): StepEffect<S, E, R> =
        StepEffectImpl(emptyList(), emptyList(), null, emptyList(), emptyList())

    /**
     * Creates a [StepEffect] that will persist the supplied [events] in order.
     */
    fun <S, E, R> persist(vararg events: E): StepEffect<S, E, R> =
        StepEffectImpl(events.toList(), emptyList(), null, emptyList(), emptyList())

    /**
     * Schedule a durable timer at an absolute [Instant].
     */
    fun <S, E, R> schedule(key: String, at: Instant, payload: Any): StepEffect<S, E, R> =
        StepEffectImpl(emptyList(), emptyList(), null, listOf(Effect.TimerInstruction.Schedule(key, at, payload)), emptyList())

    /**
     * Schedule a durable timer relative to the current time.
     */
    fun <S, E, R> scheduleAfter(key: String, delay: Duration, payload: Any): StepEffect<S, E, R> =
        StepEffectImpl(emptyList(), emptyList(), null, listOf(Effect.TimerInstruction.ScheduleAfter(key, delay, payload)), emptyList())

    /**
     * Cancel a durable timer by its [key].
     */
    fun <S, E, R> cancelTimer(key: String): StepEffect<S, E, R> =
        StepEffectImpl(emptyList(), emptyList(), null, listOf(Effect.TimerInstruction.Cancel(key)), emptyList())

    /**
     * Enqueue an outbox message to be published with the current transaction.
     */
    fun <S, E, R> outbox(channel: String, message: Any): StepEffect<S, E, R> =
        StepEffectImpl(emptyList(), emptyList(), null, emptyList(), listOf(Effect.OutboxMessage(channel, message)))

    /**
     * Type-safe factory bound to a workflow's state, event and reply types.
     */
    class Factory<S, E, R> internal constructor() {
        fun none(): StepEffect<S, E, R> = Effects.none()
        fun persist(vararg events: E): StepEffect<S, E, R> = Effects.persist(*events)
        fun schedule(key: String, at: Instant, payload: Any): StepEffect<S, E, R> = Effects.schedule(key, at, payload)
        fun scheduleAfter(key: String, delay: Duration, payload: Any): StepEffect<S, E, R> = Effects.scheduleAfter(key, delay, payload)
        fun cancelTimer(key: String): StepEffect<S, E, R> = Effects.cancelTimer(key)
        fun outbox(channel: String, message: Any): StepEffect<S, E, R> = Effects.outbox(channel, message)
    }

    internal fun <S, E, R> factory(): Factory<S, E, R> = Factory()
}
