package com.example.platform.persistence.jdbc

import com.example.platform.core.CommandMetadata
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Minimal persistence SPI used by the runtime. The JDBC module will eventually grow a true
 * implementation backed by Postgres, but for early testing we ship an in-memory implementation
 * that honours the durability semantics expected by the spec (idempotent command application,
 * monotonically increasing sequence numbers, and basic audit metadata capture).
 */
interface WorkflowPersistence {
    suspend fun <S : Any> loadWorkflow(
        workflowType: String,
        workflowId: String,
        initialState: () -> S,
    ): LoadedWorkflow<S>

    suspend fun isCommandProcessed(
        workflowType: String,
        workflowId: String,
        commandId: String,
    ): Boolean

    suspend fun <S : Any, E : Any> commitWorkflow(
        workflowType: String,
        workflowId: String,
        expectedSequence: Long,
        newState: S,
        newEvents: List<E>,
        metadata: CommandMetadata,
    )

    suspend fun fetchJournal(
        workflowType: String,
        workflowId: String,
    ): List<JournalEntry<Any>>

    suspend fun readState(workflowType: String, workflowId: String): Any?
}

data class LoadedWorkflow<S>(
    val state: S,
    val history: List<JournalEntry<Any>>,
    val lastSequence: Long,
)

data class JournalEntry<E : Any>(
    val sequence: Long,
    val event: E,
    val metadata: CommandMetadata,
    val recordedAt: Instant,
)

private data class StoredWorkflow(
    var state: Any,
    var sequence: Long,
    val journal: MutableList<JournalEntry<Any>>,
    val commandIds: MutableSet<String>,
)

class InMemoryWorkflowPersistence(
    private val clock: Clock = Clock.systemUTC(),
) : WorkflowPersistence {
    private val mutex = Mutex()
    private val store = ConcurrentHashMap<String, StoredWorkflow>()

    override suspend fun <S : Any> loadWorkflow(
        workflowType: String,
        workflowId: String,
        initialState: () -> S,
    ): LoadedWorkflow<S> = mutex.withLock {
        val key = key(workflowType, workflowId)
        val existing = store[key]
        if (existing != null) {
            @Suppress("UNCHECKED_CAST")
            return@withLock LoadedWorkflow(
                state = existing.state as S,
                history = existing.journal.toList(),
                lastSequence = existing.sequence,
            )
        }
        val initial = initialState()
        val created = StoredWorkflow(
            state = initial,
            sequence = 0L,
            journal = mutableListOf(),
            commandIds = mutableSetOf(),
        )
        store[key] = created
        return@withLock LoadedWorkflow(
            state = initial,
            history = emptyList(),
            lastSequence = 0L,
        )
    }

    override suspend fun isCommandProcessed(
        workflowType: String,
        workflowId: String,
        commandId: String,
    ): Boolean = mutex.withLock {
        val key = key(workflowType, workflowId)
        val existing = store[key]
        existing?.commandIds?.contains(commandId) ?: false
    }

    override suspend fun <S : Any, E : Any> commitWorkflow(
        workflowType: String,
        workflowId: String,
        expectedSequence: Long,
        newState: S,
        newEvents: List<E>,
        metadata: CommandMetadata,
    ) {
        mutex.withLock {
            val key = key(workflowType, workflowId)
            val existing = store[key]
                ?: StoredWorkflow(
                    state = newState,
                    sequence = 0L,
                    journal = mutableListOf(),
                    commandIds = mutableSetOf(),
                ).also { store[key] = it }

            if (existing.sequence != expectedSequence) {
                throw IllegalStateException(
                    "Sequence mismatch for ${'$'}workflowType/${'$'}workflowId. expected=${'$'}expectedSequence actual=${'$'}{existing.sequence}",
                )
            }

            metadata.commandId?.let { existing.commandIds += it }

            if (newEvents.isNotEmpty()) {
                newEvents.forEachIndexed { index, event ->
                    val sequence = existing.sequence + index + 1
                    existing.journal += JournalEntry(
                        sequence = sequence,
                        event = event as Any,
                        metadata = metadata,
                        recordedAt = clock.instant(),
                    )
                }
                existing.sequence += newEvents.size
            }
            existing.state = newState
        }
    }

    override suspend fun fetchJournal(
        workflowType: String,
        workflowId: String,
    ): List<JournalEntry<Any>> = mutex.withLock {
        val key = key(workflowType, workflowId)
        val existing = store[key]
        existing?.journal?.toList() ?: emptyList()
    }

    override suspend fun readState(workflowType: String, workflowId: String): Any? = mutex.withLock {
        val key = key(workflowType, workflowId)
        store[key]?.state
    }

    private fun key(workflowType: String, workflowId: String): String = "${'$'}workflowType:${'$'}workflowId"
}
