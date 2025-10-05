package com.example.workflow.persistence.jdbc

import java.time.Instant

/**
 * Snapshot access contract.
 */
interface SnapshotStore {
    suspend fun load(workflowType: String, workflowId: String): Snapshot?
    suspend fun write(snapshot: Snapshot)

    data class Snapshot(
        val workflowType: String,
        val workflowId: String,
        val lastSequence: Long,
        val stateJson: String,
        val takenAt: Instant = Instant.now()
    )
}
