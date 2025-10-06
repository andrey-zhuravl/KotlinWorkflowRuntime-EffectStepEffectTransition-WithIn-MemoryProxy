package com.example.platform.admin

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
