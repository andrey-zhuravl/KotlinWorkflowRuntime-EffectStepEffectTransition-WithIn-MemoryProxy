package com.example.platform.core

/**
 * Defines the contract for a workflow handling commands and producing events and replies.
 */
public interface Workflow<S, C, E, R> {
    /** The human readable workflow name. */
    public val name: String

    /** Returns the initial state for a given identifier. */
    public fun initialState(id: String): S

    /** Applies the given event to the provided state. */
    public fun applyEvent(state: S, event: E): S

    /**
     * Handles the provided command for the given state and context producing an [Effect].
     */
    public fun onCommand(state: S, command: C, ctx: WorkflowContext<S, C, E, R>): Effect<S, E, R>
}
