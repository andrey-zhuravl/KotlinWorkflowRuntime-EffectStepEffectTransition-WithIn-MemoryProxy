package com.example.platform.core.guard

import com.example.platform.core.RejectedStepEffectImpl
import com.example.platform.core.StepEffect

/**
 * Builder returned from [com.example.platform.core.WorkflowContext.guard] that
 * allows expressing guard failure handling strategies.
 */
public interface GuardCheck<S, C, E, R> {
    /**
     * Configures the guard to short-circuit with a rejection reply when the
     * predicate fails.
     */
    public fun orReject(buildReply: (S) -> R): GuardGate<S, E, R>

    /**
     * Throws an [IllegalStateException] with the provided [message] when the
     * predicate fails. When the predicate passes this method returns normally.
     */
    public fun orThrow(message: String = "Guard violated")
}

/**
 * Represents the continuation of a guard configuration. Calling [then] either
 * executes the provided [block] (when the guard passed) or returns a
 * short-circuit [StepEffect] that reports the configured rejection.
 */
public fun interface GuardGate<S, E, R> {
    public fun then(block: () -> StepEffect<S, E, R>): StepEffect<S, E, R>
}

internal class GuardCheckImpl<S, C, E, R>(
    private val name: String,
    private val state: S,
    private val command: C,
    private val predicate: (S, C) -> Boolean
) : GuardCheck<S, C, E, R> {
    private val passed: Boolean = predicate(state, command)

    override fun orReject(buildReply: (S) -> R): GuardGate<S, E, R> {
        return if (passed) {
            GuardGate { builder -> builder() }
        } else {
            val rejection = RejectedStepEffectImpl<S, E, R>(buildReply)
            GuardGate { rejection }
        }
    }

    override fun orThrow(message: String) {
        if (!passed) {
            throw IllegalStateException("$message (guard=$name)")
        }
    }
}
