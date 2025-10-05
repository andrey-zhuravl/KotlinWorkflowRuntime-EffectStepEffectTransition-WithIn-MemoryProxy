package com.example.workflow

import com.example.workflow.runtime.WorkflowEngine
import com.example.workflow.sample.OrderCommand
import com.example.workflow.sample.OrderReply
import com.example.workflow.sample.OrderWorkflow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class EngineConcurrencyTest {
    private val workflow = OrderWorkflow()

    @Test
    fun `commands are serialized per instance`() = runTest {
        val engine = WorkflowEngine(workflow, backgroundScope)
        val proxy = engine.proxy("order-concurrency")
        val create = proxy.ask(OrderCommand.CreateOrder("customer"))
        assertTrue(create is OrderReply.Accepted)

        coroutineScope {
            repeat(20) { index ->
                launch {
                    val reply = proxy.ask(OrderCommand.AddItem("item-${'$'}index"))
                    assertTrue(reply is OrderReply.Accepted)
                }
            }
        }

        val state = proxy.state()
        assertEquals(21, state.version)
        assertEquals(20, state.items.size)
        assertTrue((0 until 20).all { "item-${'$'}it" in state.items })
        assertEquals(21, proxy.events().size)

        engine.shutdown()
    }
}
