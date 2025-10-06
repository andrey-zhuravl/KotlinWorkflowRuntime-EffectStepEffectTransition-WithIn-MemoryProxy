package com.example.platform.runtime

data class ExecutionResult<S, E, R>(
    val state: S,
    val events: List<E>,
    val reply: R?,
)
