package com.example.platform.core

import java.time.Duration
import java.time.Instant

interface Workflow<S, C, E, R> {
    val name: String

    fun initialState(id: String): S

    fun applyEvent(state: S, event: E): S

    suspend fun onCommand(
        state: S,
        command: C,
        ctx: WorkflowContext<S, C, E, R>
    ): Effect<S, E, R>
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkflowService(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Projection(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Saga(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Event

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reply

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Sensitive

interface WorkflowLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

interface WorkflowContext<S, C, E, R> {
    val workflowId: String
    val effects: Effects<S, E, R>
    val logger: WorkflowLogger

    fun now(): Instant

    val correlationId: String?
    val tenantId: String?

    fun guard(
        name: String,
        state: S,
        command: C,
        predicate: (S) -> Boolean
    ): GuardEvaluation<S, C, E, R> =
        GuardEvaluation(name, this, state, command, predicate(state))
}

class GuardEvaluation<S, C, E, R>
internal constructor(
    private val name: String,
    private val context: WorkflowContext<S, C, E, R>,
    private val state: S,
    private val command: C,
    private val passed: Boolean
) {
    fun orReject(builder: (GuardContext<S, C>) -> R): GuardContinuation<S, C, E, R> =
        GuardContinuation(name, context, state, command, passed, builder)
}

class GuardContinuation<S, C, E, R>
internal constructor(
    private val name: String,
    private val context: WorkflowContext<S, C, E, R>,
    private val state: S,
    private val command: C,
    private val passed: Boolean,
    private val rejectionBuilder: (GuardContext<S, C>) -> R
) {
    fun then(block: () -> Effect<S, E, R>): Effect<S, E, R> {
        return if (passed) {
            block()
        } else {
            val reply = rejectionBuilder(GuardContext(name, state, command))
            context.effects.reply { reply }
        }
    }
}

data class GuardContext<S, C>(
    val name: String,
    val state: S,
    val command: C
) {
    fun snapshot(): S = state
}

class Effect<S, E, R>(
    internal val steps: List<EffectStep<S, E, R>>
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

class Effects<S, E, R> {
    fun none(): Effect<S, E, R> = Effect(emptyList())

    fun persist(vararg events: E): Effect<S, E, R> =
        Effect(listOf(EffectStep.Persist(events.toList())))

    fun reply(value: R): Effect<S, E, R> = reply { value }

    fun reply(builder: (S) -> R): Effect<S, E, R> =
        Effect(emptyList()).thenReply(builder)
}

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

sealed interface TimerInstruction {
    data class At(val key: String, val fireAt: Instant, val payload: Any) : TimerInstruction
    data class After(val key: String, val delay: Duration, val payload: Any) : TimerInstruction
    data class Cron(val key: String, val expression: String, val payload: Any, val jitter: Duration?) : TimerInstruction
    data class Cancel(val key: String) : TimerInstruction
}

data class OutboxMessage(
    val id: String,
    val payload: Any,
    val correlationId: String? = null,
    val tenantId: String? = null
)

data class ChildWorkflowStart(
    val workflowType: String,
    val workflowId: String,
    val initialCommand: Any,
    val options: ChildWorkflowOptions = ChildWorkflowOptions()
)

data class ChildWorkflowOptions(
    val startDelay: Duration? = null,
    val retryStrategy: RetryStrategy? = null
)

data class RetryStrategy(
    val maxAttempts: Int,
    val initialDelay: Duration,
    val multiplier: Double = 2.0
)

data class HumanInteraction(
    val token: String,
    val ttl: Duration,
    val payload: Any,
    val notifyChannel: String? = null
)

object EffectsDSL {
    fun <S, E, R> create(): Effects<S, E, R> = Effects()
}
