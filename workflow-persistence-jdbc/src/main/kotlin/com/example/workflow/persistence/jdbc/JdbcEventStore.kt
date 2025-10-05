package com.example.workflow.persistence.jdbc

import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Postgres implementation placeholder for [EventStore].
 */
class JdbcEventStore(private val dataSource: DataSource) : EventStore {
    override suspend fun <E> append(request: EventStore.AppendRequest<E>): EventStore.AppendResult =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException("JdbcEventStore.append is not yet implemented")
        }

    override suspend fun <E> load(
        workflowType: String,
        workflowId: String,
        fromSequence: Long
    ): EventStore.EventStream<E> = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException("JdbcEventStore.load is not yet implemented")
    }
}
