package com.example.workflow.projections

import com.example.workflow.core.StepEffect
import kotlinx.coroutines.flow.Flow

interface Projection {
    val name: String
    suspend fun start(from: Offset?)
    suspend fun stop()
}

data class Offset(val journalSeq: Long)

data class JournalEvent(
    val type: String,
    val workflowId: String,
    val sequence: Long,
    val payloadJson: String,
    val tenantId: String
)

interface ProjectionHandler {
    suspend fun on(event: JournalEvent, tx: ProjectionTx)
}

interface ProjectionTx {
    suspend fun upsert(sql: String, params: List<Any?>)
}

interface ProjectionSupervisor {
    val events: Flow<StepEffect<*, *, *>>
    suspend fun ensureRunning(projection: Projection)
    suspend fun shutdown(name: String)
}
