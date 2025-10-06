package com.example.platform.persistence.jdbc

import com.example.platform.core.CommandMetadata

interface WorkflowPersistence {
    suspend fun <S : Any> loadWorkflow(
        workflowType: String,
        workflowId: String,
        initialState: () -> S,
    ): LoadedWorkflow<S>

    suspend fun isCommandProcessed(
        workflowType: String,
        workflowId: String,
        commandId: String,
    ): Boolean

    suspend fun <S : Any, E : Any> commitWorkflow(
        workflowType: String,
        workflowId: String,
        expectedSequence: Long,
        newState: S,
        newEvents: List<E>,
        metadata: CommandMetadata,
    )

    suspend fun fetchJournal(
        workflowType: String,
        workflowId: String,
    ): List<JournalEntry<Any>>

    suspend fun readState(workflowType: String, workflowId: String): Any?
}
