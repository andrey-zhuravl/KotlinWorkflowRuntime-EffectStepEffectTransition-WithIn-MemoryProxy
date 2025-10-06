package com.example.platform.admin

import com.example.platform.persistence.jdbc.JournalEntry

data class WorkflowEventsView(
    val workflowType: String,
    val workflowId: String,
    val events: List<JournalEntry<Any>>,
)
