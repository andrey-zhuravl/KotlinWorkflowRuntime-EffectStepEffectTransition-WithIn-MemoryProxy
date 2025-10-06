package com.example.platform.saga

import kotlinx.coroutines.delay

class InMemorySagaOrchestrator {
    suspend fun <I, O> run(
        input: I,
        builder: SagaBuilder<I, O>.() -> Unit,
    ): SagaResult<O> {
        val saga = SagaBuilder<I, O>("inline").apply(builder).build()
        return run(input, saga)
    }

    suspend fun <I, O> run(input: I, saga: SagaDefinition<I, O>): SagaResult<O> {
        val executed = mutableListOf<Pair<SagaStep.Action<I, O>, O>>()
        return try {
            var last: O? = null
            for (step in saga.steps) {
                when (step) {
                    is SagaStep.Action -> {
                        val result = executeWithRetry(step, input)
                        executed += step to result
                        last = result
                    }
                }
            }
            SagaResult(SagaResult.Outcome.Success, last)
        } catch (error: Throwable) {
            executed.asReversed().forEach { (step, result) ->
                try {
                    step.compensate(input, result)
                } catch (_: Throwable) {
                    // best-effort compensation
                }
            }
            SagaResult(SagaResult.Outcome.Compensated, error = error)
        }
    }

    private suspend fun <I, O> executeWithRetry(step: SagaStep.Action<I, O>, input: I): O {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < step.retry.maxAttempts) {
            try {
                return step.execute(input)
            } catch (error: Throwable) {
                lastError = error
                attempt++
                if (attempt >= step.retry.maxAttempts) break
                if (step.retry.delayMillis > 0) {
                    delay(step.retry.delayMillis)
                }
            }
        }
        throw lastError ?: IllegalStateException("Unknown saga error for step ${'$'}{step.name}")
    }
}
