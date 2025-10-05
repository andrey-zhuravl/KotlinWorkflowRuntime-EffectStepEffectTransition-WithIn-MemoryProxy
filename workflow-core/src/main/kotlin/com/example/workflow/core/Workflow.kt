package com.example.workflow.core

/**
 * Base contract for all workflows executed by the runtime.
 */
interface Workflow<S, C, E, R> {
    /** Human-readable workflow name used for diagnostics. */
    val name: String

    /** Creates a brand-new state for a workflow instance. */
    fun initialState(): S

    /** Applies the given [event] to the supplied [state], producing a new state snapshot. */
    fun applyEvent(state: S, event: E): S

    /**
     * Handles a [command] given the current [state] and must return an [Effect] describing the outcome.
     */
    suspend fun onCommand(state: S, command: C, ctx: WorkflowContext<S, C, E, R>): Effect<S, E, R>
}
