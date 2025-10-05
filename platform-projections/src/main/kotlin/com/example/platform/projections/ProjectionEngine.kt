package com.example.platform.projections

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

interface ProjectionTx {
    suspend fun put(key: String, value: Any)
    suspend fun get(key: String): Any?
}

data class ProjectionEvent(
    val workflowType: String,
    val workflowId: String,
    val sequence: Long,
    val event: Any,
    val tenantId: String?,
    val recordedAt: Instant,
)

fun interface ProjectionHandler {
    suspend fun on(event: ProjectionEvent, tx: ProjectionTx)
}

interface ProjectionEngine {
    fun register(name: String, handler: ProjectionHandler)
    suspend fun publish(event: ProjectionEvent)
    suspend fun snapshot(name: String): Map<String, Any>
}

class InMemoryProjectionEngine : ProjectionEngine {
    private val mutex = Mutex()
    private val handlers = mutableMapOf<String, ProjectionHandler>()
    private val stores = mutableMapOf<String, MutableMap<String, Any>>()

    override fun register(name: String, handler: ProjectionHandler) {
        handlers[name] = handler
        stores.computeIfAbsent(name) { mutableMapOf() }
    }

    override suspend fun publish(event: ProjectionEvent) {
        val snapshot = mutex.withLock { stores.toMap() }
        for ((name, handler) in handlers) {
            if (!snapshot.containsKey(name)) continue
            val tx = InMemoryProjectionTx(name)
            handler.on(event, tx)
        }
    }

    override suspend fun snapshot(name: String): Map<String, Any> = mutex.withLock {
        stores[name]?.toMap() ?: emptyMap()
    }

    private inner class InMemoryProjectionTx(
        private val name: String,
    ) : ProjectionTx {
        override suspend fun put(key: String, value: Any) {
            mutex.withLock {
                val store = stores[name] ?: mutableMapOf<String, Any>().also { stores[name] = it }
                store[key] = value
            }
        }

        override suspend fun get(key: String): Any? = mutex.withLock {
            stores[name]?.get(key)
        }
    }
}
