package com.example.workflow.core

/**
 * Represents a side-effect executed after the workflow state has been updated for a command.
 */
typealias SideEffect<S> = suspend (S) -> Unit

/**
 * Provides contextual information to a workflow while handling commands.
 */
interface WorkflowContext<S, C, E, R> {
    /** Identifier for the workflow instance currently being processed. */
    val id: String

    /** Factory that produces [Effect] builders bound to the workflow types. */
    val effects: Effects.Factory<S, E, R>
}

internal class DefaultWorkflowContext<S, C, E, R>(
    override val id: String
) : WorkflowContext<S, C, E, R> {
    override val effects: Effects.Factory<S, E, R> = Effects.factory()
}
