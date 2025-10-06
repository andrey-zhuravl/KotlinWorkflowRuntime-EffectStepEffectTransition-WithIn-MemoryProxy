package com.example.platform.saga

data class SagaDefinition<I, O>(
    val name: String,
    val steps: List<SagaStep<I, O>>,
)
