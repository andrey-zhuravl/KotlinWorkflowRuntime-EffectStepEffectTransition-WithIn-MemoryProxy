package com.example.sample.order

import com.example.platform.runtime.local.WorkflowEngine
import com.example.sample.order.generated.OrderWorkflowGenerated
import com.example.testing.Test
import com.example.testing.assertEquals
import com.example.testing.assertIs
import kotlinx.coroutines.runBlocking

class OrderWorkflowGeneratedTest {
    @Test
    fun generatedProxyHandlesHappyPath() = runBlocking {
        val workflow = OrderWorkflow()
        val engine = WorkflowEngine(workflow)
        OrderWorkflowGenerated.register(engine, workflow)
        val proxy = OrderWorkflowGenerated.proxy(engine, "order-generated")

        val create = proxy.ask(CreateOrder("Dana"))
        assertIs<OrderReply.Ok>(create)
        proxy.ask(AddItem("Item-A"))
        proxy.ask(AddItem("Item-B"))
        proxy.ask(Approve)
        proxy.ask(Ship)

        val state = proxy.state()
        assertEquals(OrderStatus.Shipped, state.status)
        assertEquals(listOf("Item-A", "Item-B"), state.items)
        assertEquals(
            listOf(
                OrderCreated("Dana"),
                ItemAdded("Item-A"),
                ItemAdded("Item-B"),
                OrderApproved,
                OrderShipped
            ),
            proxy.events()
        )
    }
}
