package com.example.workflow.saga

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Coordinates the execution of saga steps.
 */
class SagaOrchestrator<State>(
    private val scope: CoroutineScope,
    private val store: SagaStore<State>
) {
    fun start(sagaId: String, saga: Saga<State>, builder: SagaDsl<State>.() -> Unit) {
        scope.launch {
            val definition = SagaDsl<State>().apply(builder).build()
            store.save(
                SagaStore.SagaState(
                    sagaId = sagaId,
                    sagaName = saga.name,
                    stateJson = "{}",
                    status = SagaStatus.RUNNING,
                    updatedAt = java.time.Instant.now()
                )
            )
            // Full orchestrator logic to be implemented in production version.
        }
    }
}
