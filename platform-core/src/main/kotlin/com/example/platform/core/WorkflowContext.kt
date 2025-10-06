package com.example.platform.core

import java.time.Instant

interface WorkflowContext<S, C, E, R> {
    val workflowId: String
    val effects: Effects<S, E, R>
    val logger: WorkflowLogger

    fun now(): Instant

    val correlationId: String?
    val tenantId: String?

    fun guard(
        name: String,
        state: S,
        command: C,
        predicate: (S) -> Boolean,
    ): GuardEvaluation<S, C, E, R> =
        GuardEvaluation(name, this, state, command, predicate(state))
}
