package com.example.platform.projections

import java.time.Instant

data class ProjectionEvent(
    val workflowType: String,
    val workflowId: String,
    val sequence: Long,
    val event: Any,
    val tenantId: String?,
    val recordedAt: Instant,
)
