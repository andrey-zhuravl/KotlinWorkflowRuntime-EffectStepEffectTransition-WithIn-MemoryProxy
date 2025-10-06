package com.example.platform.projections

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryProjectionTx(
    private val name: String,
    private val stores: MutableMap<String, MutableMap<String, Any>>,
    private val mutex: Mutex,
) : ProjectionTx {
    override suspend fun put(key: String, value: Any) {
        mutex.withLock {
            val store = stores.computeIfAbsent(name) { mutableMapOf() }
            store[key] = value
        }
    }

    override suspend fun get(key: String): Any? = mutex.withLock {
        stores[name]?.get(key)
    }
}
