package com.example.workflow.runtime

import kotlinx.coroutines.CompletableDeferred

/**
 * Client-facing proxy for interacting with a workflow instance.
 */
class WorkflowProxy<S, C, E, R> internal constructor(
    private val id: String,
    private val engine: WorkflowEngine<S, C, E, R>
) {
    /** Sends a command expecting a reply. */
    suspend fun ask(command: C): R {
        val reply = CompletableDeferred<R>()
        val completion = CompletableDeferred<Unit>()
        engine.submit(
            id,
            WorkflowEngine.CommandEnvelope(command, reply, expectReply = true, completion)
        )
        return reply.await()
    }

    /** Sends a fire-and-forget command that completes once processed. */
    suspend fun tell(command: C) {
        val completion = CompletableDeferred<Unit>()
        engine.submit(
            id,
            WorkflowEngine.CommandEnvelope(command, reply = null, expectReply = false, completion)
        )
        completion.await()
    }

    /** Returns the latest state snapshot of the workflow instance. */
    fun state(): S = engine.stateSnapshot(id)

    /** Returns a copy of the persisted event log for the workflow instance. */
    fun events(): List<E> = engine.eventLog(id)
}
