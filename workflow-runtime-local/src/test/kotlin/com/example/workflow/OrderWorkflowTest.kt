package com.example.workflow

import com.example.workflow.runtime.WorkflowEngine
import com.example.workflow.sample.OrderCommand
import com.example.workflow.sample.OrderEvent
import com.example.workflow.sample.OrderReply
import com.example.workflow.sample.OrderState
import com.example.workflow.sample.OrderStatus
import com.example.workflow.sample.OrderWorkflow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OrderWorkflowTest {
    private val workflow = OrderWorkflow()
    private lateinit var engine: WorkflowEngine<OrderState, OrderCommand, OrderEvent, OrderReply>

    @AfterTest
    fun tearDown() = runTest {
        if (::engine.isInitialized) {
            engine.shutdown()
        }
    }

    @Test
    fun `happy path transitions`() = runTest {
        engine = WorkflowEngine(workflow, backgroundScope)
        val proxy = engine.proxy("order-1")

        val create = proxy.ask(OrderCommand.CreateOrder("customer-1"))
        assertTrue(create is OrderReply.Accepted)

        val add1 = proxy.ask(OrderCommand.AddItem("laptop"))
        val add2 = proxy.ask(OrderCommand.AddItem("mouse"))
        assertTrue(add1 is OrderReply.Accepted && add2 is OrderReply.Accepted)

        val approve = proxy.ask(OrderCommand.Approve)
        val ship = proxy.ask(OrderCommand.Ship)
        assertTrue(approve is OrderReply.Accepted && ship is OrderReply.Accepted)

        val state = proxy.state()
        assertEquals(OrderStatus.SHIPPED, state.status)
        assertEquals(2, state.items.size)
        assertEquals(5, state.version)

        val events = proxy.events()
        assertEquals(5, events.size)
        assertTrue(events[0] is OrderEvent.OrderCreated)
        assertTrue(events[1] is OrderEvent.ItemAdded)
        assertTrue(events[2] is OrderEvent.ItemAdded)
        assertTrue(events[3] is OrderEvent.OrderApproved)
        assertTrue(events[4] is OrderEvent.OrderShipped)
    }

    @Test
    fun `invalid transition produces rejection`() = runTest {
        engine = WorkflowEngine(workflow, backgroundScope)
        val proxy = engine.proxy("order-2")
        proxy.ask(OrderCommand.CreateOrder("customer-1"))

        val reply = proxy.ask(OrderCommand.Ship)
        assertTrue(reply is OrderReply.Rejected)
        assertEquals(OrderStatus.CREATED, proxy.state().status)
        assertEquals(1, proxy.events().size)
    }

    @Test
    fun `cancelled order rejects further commands`() = runTest {
        engine = WorkflowEngine(workflow, backgroundScope)
        val proxy = engine.proxy("order-3")
        proxy.ask(OrderCommand.CreateOrder("customer-2"))
        proxy.ask(OrderCommand.AddItem("keyboard"))

        val cancel = proxy.ask(OrderCommand.Cancel("customer request"))
        assertTrue(cancel is OrderReply.Accepted)
        assertEquals(OrderStatus.CANCELLED, proxy.state().status)

        val after = proxy.ask(OrderCommand.AddItem("mouse"))
        assertTrue(after is OrderReply.Rejected)
        assertEquals(3, proxy.events().size)
    }
}
