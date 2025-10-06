package com.example.platform.runtime

import com.example.platform.core.CommandMetadata
import com.example.platform.core.Effects
import com.example.platform.core.WorkflowContext
import com.example.platform.core.WorkflowLogger
import java.time.Clock
import java.time.Instant

internal class DefaultWorkflowContext<S, C, E, R>(
    val workflowType: String,
    override val workflowId: String,
    override val effects: Effects<S, E, R>,
    override val logger: WorkflowLogger,
    private val clock: Clock,
    val metadata: CommandMetadata,
) : WorkflowContext<S, C, E, R> {
    override fun now(): Instant = clock.instant()
    override val correlationId: String? get() = metadata.correlationId
    override val tenantId: String? get() = metadata.tenantId
}
