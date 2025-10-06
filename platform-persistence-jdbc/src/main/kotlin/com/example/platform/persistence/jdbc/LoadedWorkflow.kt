package com.example.platform.persistence.jdbc

data class LoadedWorkflow<S>(
    val state: S,
    val history: List<JournalEntry<Any>>,
    val lastSequence: Long,
)
