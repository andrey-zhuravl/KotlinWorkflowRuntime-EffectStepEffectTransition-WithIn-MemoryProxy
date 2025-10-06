package com.example.platform.persistence.jdbc

import com.example.platform.core.CommandMetadata
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryWorkflowPersistence(
    private val clock: Clock = Clock.systemUTC(),
) : WorkflowPersistence {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, StoredWorkflow>()

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
        store[key]?.commandIds?.contains(commandId) ?: false
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
                    state = newState as Any,
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
            existing.state = newState as Any
        }
    }

    override suspend fun fetchJournal(
        workflowType: String,
        workflowId: String,
    ): List<JournalEntry<Any>> = mutex.withLock {
        val key = key(workflowType, workflowId)
        store[key]?.journal?.toList() ?: emptyList()
    }

    override suspend fun readState(workflowType: String, workflowId: String): Any? = mutex.withLock {
        val key = key(workflowType, workflowId)
        store[key]?.state
    }

    private fun key(workflowType: String, workflowId: String): String = "${'$'}workflowType:${'$'}workflowId"
}
