package com.example.platform.core

sealed interface EffectStep<S, E, R> {
    data class Persist<S, E, R>(val events: List<E>) : EffectStep<S, E, R>
    data class Transition<S, E, R>(val transition: suspend (S) -> S) : EffectStep<S, E, R>
    data class Run<S, E, R>(val block: suspend (S) -> Unit) : EffectStep<S, E, R>
    data class Reply<S, E, R>(val builder: (S) -> R) : EffectStep<S, E, R>
    class NoReply<S, E, R> internal constructor() : EffectStep<S, E, R>
    data class Schedule<S, E, R>(val instruction: TimerInstruction) : EffectStep<S, E, R>
    data class Outbox<S, E, R>(val message: OutboxMessage, val channel: String, val headers: Map<String, String>) : EffectStep<S, E, R>
    data class ChildWorkflow<S, E, R>(val start: ChildWorkflowStart) : EffectStep<S, E, R>
    data class Human<S, E, R>(val interaction: HumanInteraction) : EffectStep<S, E, R>
}
