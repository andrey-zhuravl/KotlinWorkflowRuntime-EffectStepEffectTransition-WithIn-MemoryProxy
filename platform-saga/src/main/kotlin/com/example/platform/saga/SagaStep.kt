package com.example.platform.saga

sealed interface SagaStep<I, O> {
    val name: String

    data class Action<I, O>(
        override val name: String,
        val execute: suspend (I) -> O,
        val compensate: suspend (I, O) -> Unit = { _, _ -> },
        val retry: RetryPolicy = RetryPolicy.none(),
    ) : SagaStep<I, O>
}
