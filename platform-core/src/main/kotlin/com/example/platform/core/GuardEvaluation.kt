package com.example.platform.core

class GuardEvaluation<S, C, E, R>
internal constructor(
    private val name: String,
    private val context: WorkflowContext<S, C, E, R>,
    private val state: S,
    private val command: C,
    private val passed: Boolean,
) {
    fun orReject(builder: (GuardContext<S, C>) -> R): GuardContinuation<S, C, E, R> =
        GuardContinuation(name, context, state, command, passed, builder)
}
