package com.example.platform.projections

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryProjectionEngine : ProjectionEngine {
    private val mutex = Mutex()
    private val handlers = mutableMapOf<String, ProjectionHandler>()
    private val stores = mutableMapOf<String, MutableMap<String, Any>>()

    override fun register(name: String, handler: ProjectionHandler) {
        handlers[name] = handler
        stores.computeIfAbsent(name) { mutableMapOf() }
    }

    override suspend fun publish(event: ProjectionEvent) {
        val registered = handlers.toMap()
        for ((name, handler) in registered) {
            if (!stores.containsKey(name)) continue
            val tx = InMemoryProjectionTx(name, stores, mutex)
            handler.on(event, tx)
        }
    }

    override suspend fun snapshot(name: String): Map<String, Any> = mutex.withLock {
        stores[name]?.toMap() ?: emptyMap()
    }
}
