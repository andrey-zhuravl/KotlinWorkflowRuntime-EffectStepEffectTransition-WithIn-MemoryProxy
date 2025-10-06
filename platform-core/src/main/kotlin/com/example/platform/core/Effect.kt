package com.example.platform.core

import java.time.Duration
import java.time.Instant

class Effect<S, E, R>(
    internal val steps: List<EffectStep<S, E, R>>,
) {
    fun thenTransition(transition: suspend (S) -> S): Effect<S, E, R> =
        withStep(EffectStep.Transition(transition))

    fun thenRun(block: suspend (S) -> Unit): Effect<S, E, R> =
        withStep(EffectStep.Run(block))

    fun thenReply(builder: (S) -> R): Effect<S, E, R> =
        withStep(EffectStep.Reply(builder))

    fun thenNoReply(): Effect<S, E, R> = withStep(EffectStep.NoReply())

    fun schedule(key: String, at: Instant, payload: Any): Effect<S, E, R> =
        withStep(EffectStep.Schedule(TimerInstruction.At(key, at, payload)))

    fun scheduleAfter(key: String, delay: Duration, payload: Any): Effect<S, E, R> =
        withStep(EffectStep.Schedule(TimerInstruction.After(key, delay, payload)))

    fun scheduleCron(key: String, expression: String, payload: Any, jitter: Duration? = null): Effect<S, E, R> =
        withStep(EffectStep.Schedule(TimerInstruction.Cron(key, expression, payload, jitter)))

    fun cancelTimer(key: String): Effect<S, E, R> =
        withStep(EffectStep.Schedule(TimerInstruction.Cancel(key)))

    fun outbox(message: OutboxMessage, channel: String, headers: Map<String, String> = emptyMap()): Effect<S, E, R> =
        withStep(EffectStep.Outbox(message, channel, headers))

    fun startChild(child: ChildWorkflowStart): Effect<S, E, R> =
        withStep(EffectStep.ChildWorkflow(child))

    fun awaitHuman(interaction: HumanInteraction): Effect<S, E, R> =
        withStep(EffectStep.Human(interaction))

    private fun withStep(step: EffectStep<S, E, R>) = Effect(steps + step)
}
