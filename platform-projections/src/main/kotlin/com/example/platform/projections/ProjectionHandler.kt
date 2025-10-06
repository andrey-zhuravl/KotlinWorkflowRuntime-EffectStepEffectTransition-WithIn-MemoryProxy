package com.example.platform.projections

fun interface ProjectionHandler {
    suspend fun on(event: ProjectionEvent, tx: ProjectionTx)
}
