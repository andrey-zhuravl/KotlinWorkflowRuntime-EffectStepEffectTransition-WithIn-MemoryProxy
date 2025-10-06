package com.example.platform.projections

interface ProjectionEngine {
    fun register(name: String, handler: ProjectionHandler)
    suspend fun publish(event: ProjectionEvent)
    suspend fun snapshot(name: String): Map<String, Any>
}
