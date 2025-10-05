package com.example.workflow.persistence.jdbc

import java.time.Instant

/**
 * Contract for loading and appending workflow events in a durable store.
 */
interface EventStore {
    suspend fun <E> append(request: AppendRequest<E>): AppendResult
    suspend fun <E> load(workflowType: String, workflowId: String, fromSequence: Long = 0): EventStream<E>

    data class AppendRequest<E>(
        val workflowType: String,
        val workflowId: String,
        val expectedSequence: Long,
        val events: List<SerializedEvent<E>>,
        val metadata: EventMetadata,
        val snapshot: SnapshotUpdate? = null,
        val outbox: List<OutboxEntry> = emptyList(),
        val timers: List<TimerMutation> = emptyList()
    )

    data class SerializedEvent<E>(
        val payload: E,
        val type: String,
        val json: String,
        val occurredAt: Instant = Instant.now(),
        val commandId: String?
    )

    data class EventMetadata(
        val correlationId: String,
        val causationId: String?,
        val userId: String? = null
    )

    data class SnapshotUpdate(
        val stateJson: String,
        val lastSequence: Long,
        val takenAt: Instant = Instant.now()
    )

    data class OutboxEntry(
        val channel: String,
        val key: String?,
        val payloadJson: String,
        val type: String
    )

    sealed class TimerMutation {
        data class Schedule(
            val timerKey: String,
            val payloadJson: String,
            val fireAt: Instant,
            val commandType: String
        ) : TimerMutation()

        data class Cancel(val timerKey: String) : TimerMutation()
    }

    data class AppendResult(
        val lastSequence: Long,
        val persistedEvents: Int
    )

    data class EventStream<E>(
        val events: List<PersistedEvent<E>>,
        val snapshot: Snapshot?
    )

    data class PersistedEvent<E>(
        val sequence: Long,
        val type: String,
        val payload: E,
        val json: String,
        val occurredAt: Instant,
        val commandId: String?
    )

    data class Snapshot(
        val stateJson: String,
        val lastSequence: Long,
        val takenAt: Instant
    )
}
