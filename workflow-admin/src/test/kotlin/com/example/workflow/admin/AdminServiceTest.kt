package com.example.workflow.admin

import com.example.workflow.core.StepEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class InMemoryAdminService : AdminService {
    override suspend fun getWorkflow(type: String, id: String): WorkflowStateResponse? = null
    override suspend fun listEvents(type: String, id: String, fromSequence: Long?): EventsResponse = EventsResponse(emptyList())
    override suspend fun replayWorkflow(type: String, id: String, request: ReplayRequest): StepEffect<*, *, *>? = null
    override suspend fun moveToDlq(topic: String, offset: Long): StepEffect<*, *, *>? = null
    override suspend fun listTimers(dueBeforeEpochMilli: Long): List<TimerDescriptor> = emptyList()
    override suspend fun rescheduleTimers(selector: TimerSelectorRequest): Int = 0
    override suspend fun getProjectionOffset(name: String): ProjectionOffset? = null
}

class AdminServiceTest {
    @Test
    fun `in memory service returns null`() {
        val service = InMemoryAdminService()
        assertNull(service.getWorkflow("Order", "1"))
        assertEquals(0, service.rescheduleTimers(TimerSelectorRequest()))
    }
}
