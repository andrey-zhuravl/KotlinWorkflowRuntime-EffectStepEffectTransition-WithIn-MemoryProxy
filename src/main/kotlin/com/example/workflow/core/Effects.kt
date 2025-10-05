package com.example.workflow.core

/**
 * Entry point for building workflow effects in a fluent DSL.
 */
object Effects {
    /**
     * Creates a [StepEffect] without persisting any events.
     */
    fun <S, E, R> none(): StepEffect<S, E, R> = StepEffectImpl(emptyList(), emptyList(), null)

    /**
     * Creates a [StepEffect] that will persist the supplied [events] in order.
     */
    fun <S, E, R> persist(vararg events: E): StepEffect<S, E, R> =
        StepEffectImpl(events.toList(), emptyList(), null)

    /**
     * Type-safe factory bound to a workflow's state, event and reply types.
     */
    class Factory<S, E, R> internal constructor() {
        fun none(): StepEffect<S, E, R> = Effects.none()
        fun persist(vararg events: E): StepEffect<S, E, R> = Effects.persist(*events)
    }

    internal fun <S, E, R> factory(): Factory<S, E, R> = Factory()
}
