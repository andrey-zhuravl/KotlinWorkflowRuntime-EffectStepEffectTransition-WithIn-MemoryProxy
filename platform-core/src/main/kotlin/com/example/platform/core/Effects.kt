package com.example.platform.core

/**
 * Factory for building [Effect] instances via the fluent DSL.
 */
public object Effects {
    /** Returns an effect that performs no persistence by default. */
    public fun <S, E, R> none(): StepEffect<S, E, R> =
        NormalStepEffectImpl(emptyList(), null, emptyList())

    /** Persists the supplied events before any additional steps. */
    public fun <S, E, R> persist(vararg events: E): StepEffect<S, E, R> =
        NormalStepEffectImpl(events.toList(), null, emptyList())
}
