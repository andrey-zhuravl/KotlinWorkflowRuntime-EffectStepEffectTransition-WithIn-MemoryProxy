package com.example.platform.saga

data class SagaResult<O>(
    val outcome: Outcome,
    val result: O? = null,
    val error: Throwable? = null,
) {
    enum class Outcome { Success, Compensated }
}
