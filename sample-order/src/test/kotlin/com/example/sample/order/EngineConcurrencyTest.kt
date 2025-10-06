package com.example.sample.order

import com.example.platform.runtime.local.WorkflowEngine
import com.example.testing.Test
import com.example.testing.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class EngineConcurrencyTest {
    @Test
    fun addItemsConcurrentlySerializesPerInstance() = runBlocking {
        val workflow = OrderWorkflow()
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("order-serial")

        proxy.ask(CreateOrder("Charlie"))

        val tasks = (0 until 100).map { index ->
            async { proxy.ask(AddItem("item-$index")) }
        }
        tasks.awaitAll()

        val state = proxy.state()
        assertEquals(100, state.items.size)
        assertEquals((0 until 100).map { "item-$it" }.toSet(), state.items.toSet())
        assertEquals(101, proxy.events().size)
    }
}
