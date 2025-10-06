package com.example.platform.core

interface Workflow<S, C, E, R> {
    val name: String

    fun initialState(id: String): S

    fun applyEvent(state: S, event: E): S

    suspend fun onCommand(
        state: S,
        command: C,
        ctx: WorkflowContext<S, C, E, R>,
    ): Effect<S, E, R>
}
