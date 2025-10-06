package com.example.platform.persistence.jdbc

internal data class StoredWorkflow(
    var state: Any,
    var sequence: Long,
    val journal: MutableList<JournalEntry<Any>>,
    val commandIds: MutableSet<String>,
)
