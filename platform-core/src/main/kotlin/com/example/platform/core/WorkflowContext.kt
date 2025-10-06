package com.example.platform.core

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
}
