package com.example.sample.order

import com.example.platform.admin.AdminService
import com.example.platform.core.CommandMetadata
import com.example.platform.human.InMemoryHumanTaskManager
import com.example.platform.persistence.jdbc.InMemoryWorkflowPersistence
import com.example.platform.projections.InMemoryProjectionEngine
import com.example.platform.projections.ProjectionHandler
import com.example.platform.runtime.CommandEnvelope
import com.example.platform.runtime.WorkflowRuntime
import com.example.platform.security.AllowAllPolicyEngine
import com.example.platform.timers.InMemoryTimerScheduler
import com.example.platform.transport.kafka.InMemoryOutboxDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OrderWorkflowEndToEndTest {
    private val persistence = InMemoryWorkflowPersistence()
    private val timers = InMemoryTimerScheduler()
    private val outbox = InMemoryOutboxDispatcher()
    private val humanTasks = InMemoryHumanTaskManager()
    private val projections = InMemoryProjectionEngine()
    private val policy = AllowAllPolicyEngine()
    private val runtime = WorkflowRuntime(
        persistence = persistence,
        timerScheduler = timers,
        outboxDispatcher = outbox,
        humanTaskManager = humanTasks,
        projectionEngine = projections,
        policyEngine = policy,
    )

    init {
        runtime.register(OrderWorkflow())
        projections.register("order-view", ProjectionHandler { event, tx ->
            when (val domainEvent = event.event) {
                is OrderEvent.OrderCreated -> {
                    val view = OrderView(
                        id = event.workflowId,
                        status = "Created",
                        customerId = domainEvent.customerId,
                        amount = domainEvent.amount,
                        approvedBy = null,
                        approvedAt = null,
                        shippedAt = null,
                    )
                    tx.put(event.workflowId, view)
                }
                is OrderEvent.OrderApproved -> {
                    val current = tx.get(event.workflowId) as? OrderView
                    val updated = (current ?: OrderView(
                        id = event.workflowId,
                        status = "Created",
                        customerId = null,
                        amount = 0,
                        approvedBy = null,
                        approvedAt = null,
                        shippedAt = null,
                    )).copy(
                        status = "Approved",
                        approvedBy = domainEvent.approvedBy,
                        approvedAt = domainEvent.approvedAt,
                    )
                    tx.put(event.workflowId, updated)
                }
                is OrderEvent.OrderShipped -> {
                    val current = tx.get(event.workflowId) as? OrderView
                    val updated = (current ?: OrderView(
                        id = event.workflowId,
                        status = "Created",
                        customerId = null,
                        amount = 0,
                        approvedBy = null,
                        approvedAt = null,
                        shippedAt = null,
                    )).copy(
                        status = "Shipped",
                        shippedAt = domainEvent.shippedAt,
                    )
                    tx.put(event.workflowId, updated)
                }
            }
        })
    }

    @Test
    fun `order workflow happy path`() = runTest {
        val workflowId = "order-1"
        val createReply = runtime.dispatch(
            CommandEnvelope(
                workflowType = "order",
                workflowId = workflowId,
                command = OrderCommand.Create(workflowId, "customer-1", 1_000L),
                metadata = CommandMetadata(commandId = "create-1", correlationId = "corr-1"),
            ),
        )
        assertIs<OrderReply.Accepted>(createReply)

        val timersAfterCreate = timers.pendingTimers()
        assertEquals(1, timersAfterCreate.size)
        assertEquals("approval-deadline", timersAfterCreate.first().key)

        val approveReply = runtime.dispatch(
            CommandEnvelope(
                workflowType = "order",
                workflowId = workflowId,
                command = OrderCommand.Approve(workflowId, "manager"),
                metadata = CommandMetadata(commandId = "approve-1", correlationId = "corr-1"),
            ),
        )
        assertIs<OrderReply.Accepted>(approveReply)

        val shipReply = runtime.dispatch(
            CommandEnvelope(
                workflowType = "order",
                workflowId = workflowId,
                command = OrderCommand.Ship(workflowId),
                metadata = CommandMetadata(commandId = "ship-1", correlationId = "corr-1"),
            ),
        )
        assertIs<OrderReply.Accepted>(shipReply)

        val outboxMessages = outbox.peek()
        assertEquals(1, outboxMessages.size)
        assertEquals("orders.shipped", outboxMessages.first().channel)

        val admin = AdminService(persistence)
        val stateView = admin.readState("order", workflowId)
        val state = stateView.state as OrderState
        assertEquals(OrderState.Status.Shipped, state.status)

        val eventsView = admin.readEvents("order", workflowId)
        assertEquals(3, eventsView.events.size)

        val projectionView = projections.snapshot("order-view")
        val projected = projectionView[workflowId] as OrderView
        assertEquals("Shipped", projected.status)
        assertTrue(projected.shippedAt != null)

        timers.advanceTo(timersAfterCreate.first().fireAt)
        assertEquals(0, timers.pendingTimers().size)
    }
}
