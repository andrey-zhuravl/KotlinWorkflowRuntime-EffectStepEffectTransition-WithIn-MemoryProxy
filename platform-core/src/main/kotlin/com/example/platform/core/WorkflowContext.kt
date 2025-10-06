package com.example.platform.core

import com.example.platform.core.guard.GuardCheck
import com.example.platform.core.guard.GuardCheckImpl
import java.time.Instant

/**
 * Context provided to workflow command handlers.
 */
public interface WorkflowContext<S, C, E, R> {
    /** Identifier of the workflow instance. */
    public val id: String

    /** Entry point for building workflow effects. */
    public val effects: Effects

    /** Returns the current instant. */
    public fun now(): Instant

    /**
     * Evaluates the [predicate] named [name] and returns a guard builder that
     * can short-circuit workflow execution when the predicate fails.
     */
    public fun guard(
        name: String,
        state: S,
        command: C,
        predicate: (S, C) -> Boolean
    ): GuardCheck<S, C, E, R> = GuardCheckImpl(name, state, command, predicate)
}
