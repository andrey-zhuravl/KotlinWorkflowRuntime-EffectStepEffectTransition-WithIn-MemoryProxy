package com.example.platform.saga

class SagaBuilder<I, O>(private val name: String) {
    private val steps = mutableListOf<SagaStep<I, O>>()

    fun action(
        name: String,
        retry: RetryPolicy = RetryPolicy.none(),
        execute: suspend (I) -> O,
        compensate: suspend (I, O) -> Unit = { _, _ -> },
    ) {
        steps += SagaStep.Action(name, execute, compensate, retry)
    }

    fun build(): SagaDefinition<I, O> = SagaDefinition(name, steps.toList())
}
