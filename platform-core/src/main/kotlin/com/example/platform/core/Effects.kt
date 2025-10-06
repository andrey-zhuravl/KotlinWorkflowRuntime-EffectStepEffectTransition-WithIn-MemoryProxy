package com.example.platform.core

class Effects<S, E, R> {
    fun none(): Effect<S, E, R> = Effect(emptyList())

    fun persist(vararg events: E): Effect<S, E, R> =
        Effect(listOf(EffectStep.Persist(events.toList())))

    fun reply(value: R): Effect<S, E, R> = reply { value }

    fun reply(builder: (S) -> R): Effect<S, E, R> =
        Effect(emptyList()).thenReply(builder)
}
