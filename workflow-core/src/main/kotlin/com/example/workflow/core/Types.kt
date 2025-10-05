package com.example.workflow.core

import java.time.Clock
import java.time.Instant
import java.util.logging.Logger

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

    /** Current instant sourced from the runtime clock. */
    fun now(): Instant

    /** Correlation id for tracing commands through the system. */
    fun correlationId(): String

    /** Structured logger scoped to the workflow instance. */
    fun logger(): Logger
}

internal class DefaultWorkflowContext<S, C, E, R> @JvmOverloads constructor(
    override val id: String,
    private val clock: Clock = Clock.systemUTC(),
    private val correlationId: String = id,
    private val logger: Logger = Logger.getLogger("workflow.${'$'}id")
) : WorkflowContext<S, C, E, R> {
    override val effects: Effects.Factory<S, E, R> = Effects.factory()
    override fun now(): Instant = clock.instant()
    override fun correlationId(): String = correlationId
    override fun logger(): Logger = logger
}
