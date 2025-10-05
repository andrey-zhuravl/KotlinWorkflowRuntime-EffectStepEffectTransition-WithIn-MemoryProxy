package com.example.workflow.cluster

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Simple semaphore-based backpressure helper.
 */
class Backpressure(maxConcurrentInstances: Int) {
    private val semaphore = Semaphore(maxConcurrentInstances)

    suspend fun <T> withPermit(block: suspend () -> T): T = semaphore.withPermit { block() }

    suspend fun awaitSlot() {
        semaphore.acquire()
        semaphore.release()
    }
}
