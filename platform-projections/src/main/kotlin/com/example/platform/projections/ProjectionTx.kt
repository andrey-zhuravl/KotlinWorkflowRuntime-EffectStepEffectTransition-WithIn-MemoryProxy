package com.example.platform.projections

interface ProjectionTx {
    suspend fun put(key: String, value: Any)
    suspend fun get(key: String): Any?
}
