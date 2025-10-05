package com.example.platform.admin

import com.example.platform.persistence.jdbc.JournalEntry
import com.example.platform.persistence.jdbc.WorkflowPersistence

class AdminService(private val persistence: WorkflowPersistence) {
    suspend fun readState(workflowType: String, workflowId: String): WorkflowStateView {
        val state = persistence.readState(workflowType, workflowId)
        return WorkflowStateView(workflowType, workflowId, state)
    }

    suspend fun readEvents(workflowType: String, workflowId: String): WorkflowEventsView {
        val entries = persistence.fetchJournal(workflowType, workflowId)
        return WorkflowEventsView(workflowType, workflowId, entries)
    }
}

data class WorkflowStateView(
    val workflowType: String,
    val workflowId: String,
    val state: Any?,
)

data class WorkflowEventsView(
    val workflowType: String,
    val workflowId: String,
    val events: List<JournalEntry<Any>>,
)
