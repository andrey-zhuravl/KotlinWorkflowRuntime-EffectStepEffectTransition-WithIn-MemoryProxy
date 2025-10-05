package com.example.workflow.saga

/**
 * Represents a durable saga definition.
 */
interface Saga<State> {
    val name: String
    fun initialState(): State
    fun build(builder: SagaDsl<State>.() -> Unit): SagaDefinition<State>
}
