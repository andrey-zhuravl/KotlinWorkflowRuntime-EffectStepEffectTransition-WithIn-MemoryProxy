package com.example.sample.order

import com.example.platform.runtime.local.WorkflowEngine
import com.example.testing.Test
import com.example.testing.assertEquals
import com.example.testing.assertIs
import kotlinx.coroutines.runBlocking

class OrderWorkflowTest {
    @Test
    fun happyPath() = runBlocking {
        val workflow = OrderWorkflow()
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("order-test")

        val create = proxy.ask(CreateOrder("Bob"))
        assertIs<OrderReply.Ok>(create)
        val add1 = proxy.ask(AddItem("Item-1"))
        assertIs<OrderReply.Ok>(add1)
        val approve = proxy.ask(Approve)
        assertIs<OrderReply.Ok>(approve)
        val ship = proxy.ask(Ship)
        assertIs<OrderReply.Ok>(ship)

        val finalState = proxy.state()
        assertEquals(OrderStatus.Shipped, finalState.status)
        assertEquals(listOf("Item-1"), finalState.items)
        assertEquals(
            listOf(
                OrderCreated("Bob"),
                ItemAdded("Item-1"),
                OrderApproved,
                OrderShipped
            ),
            proxy.events()
        )
    }
}
