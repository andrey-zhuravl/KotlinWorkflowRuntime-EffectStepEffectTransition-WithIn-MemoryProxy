package com.example.workflow.saga

import java.time.Instant

/**
 * Persistence abstraction for saga state.
 */
interface SagaStore<State> {
    suspend fun load(sagaId: String): SagaState<State>?
    suspend fun save(state: SagaState<State>)

    data class SagaState<State>(
        val sagaId: String,
        val sagaName: String,
        val stateJson: String,
        val status: SagaStatus,
        val updatedAt: Instant
    )
}

enum class SagaStatus { RUNNING, COMPLETED, COMPENSATING, COMPENSATED, FAILED }
