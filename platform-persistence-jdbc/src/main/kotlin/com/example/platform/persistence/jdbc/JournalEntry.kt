package com.example.platform.persistence.jdbc

import com.example.platform.core.CommandMetadata
import java.time.Instant

data class JournalEntry<E : Any>(
    val sequence: Long,
    val event: E,
    val metadata: CommandMetadata,
    val recordedAt: Instant,
)
