package com.example.workflow.core

/**
 * Represents a logical side effect emitted by a workflow step.
 *
 * The runtime interprets the [name] and [attributes] to trigger the correct
 * transport, timer or persistence integration.
 */
data class StepEffect<S, E, R>(
    val name: String,
    val attributes: Map<String, Any?> = emptyMap()
)

/**
 * Lightweight description of a reply or command effect produced by the DSL.
 */
data class Effect<S, E, R>(
    val type: String,
    val payload: R? = null,
    val reason: String? = null
)

/**
 * Context available to workflow handlers during execution.
 */
interface WorkflowContext<S, C, E, R> {
    val state: S

    /**
     * Records that a guard named [name] has been violated. Implementations can
     * increment counters or emit structured telemetry.
     */
    fun onGuardViolation(name: String)

    fun effect(type: String, payload: R? = null, reason: String? = null): Effect<S, E, R> =
        Effect(type = type, payload = payload, reason = reason)
}

/**
 * Exception thrown when guard checks are configured to throw.
 */
class GuardViolationException(message: String) : IllegalStateException(message)

interface GuardCheck<S, C, E, R> {
    fun orReject(buildReply: (S) -> R): Effect<S, E, R>
    fun orThrow(message: String = "Guard violated"): Nothing
}

fun <S, C, E, R> WorkflowContext<S, C, E, R>.guard(
    name: String,
    state: S,
    cmd: C,
    predicate: (S, C) -> Boolean
): GuardCheck<S, C, E, R> = GuardCheckImpl(this, name, state, cmd, predicate(state, cmd))

private class GuardCheckImpl<S, C, E, R>(
    private val context: WorkflowContext<S, C, E, R>,
    private val name: String,
    private val state: S,
    private val cmd: C,
    private val passed: Boolean
) : GuardCheck<S, C, E, R> {
    override fun orReject(buildReply: (S) -> R): Effect<S, E, R> {
        return if (passed) {
            context.effect(type = "noop")
        } else {
            context.onGuardViolation(name)
            context.effect(type = "rejection", payload = buildReply(state), reason = "Guard '$name' rejected command $cmd")
        }
    }

    override fun orThrow(message: String): Nothing {
        if (passed) {
            throw IllegalStateException("Guard '$name' marked as throw unexpectedly evaluated to true")
        }
        context.onGuardViolation(name)
        throw GuardViolationException(message)
    }
}
