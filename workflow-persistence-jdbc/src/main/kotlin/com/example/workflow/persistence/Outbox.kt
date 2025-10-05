package com.example.workflow.persistence

import java.time.Instant
import java.util.UUID

interface OutboxDispatcher {
    suspend fun publishBatch(max: Int): Int
}

data class OutboxRecord(
    val id: UUID,
    val channel: String,
    val key: String,
    val headers: Map<String, String> = emptyMap(),
    val payloadJson: String,
    val createdAt: Instant,
    val publishedAt: Instant? = null,
    val attempts: Int = 0,
    val lastError: String? = null
)

interface Archiver {
    suspend fun archive(before: Instant): Int
}

interface Replayer {
    suspend fun replay(range: SequenceRange): ReplayReport
}

data class SequenceRange(val fromInclusive: Long, val toInclusive: Long)

data class ReplayReport(val replayed: Long, val failed: Long)

interface SchemaRegistry {
    fun current(type: String): Int
    fun get(type: String, version: Int): String
}

interface EventUpcaster {
    fun canUpcast(type: String, from: Int): Boolean
    fun upcast(json: String, from: Int, to: Int): String
}
