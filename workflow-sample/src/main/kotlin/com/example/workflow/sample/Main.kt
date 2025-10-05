package com.example.workflow.sample

import com.example.workflow.runtime.WorkflowEngine
import kotlinx.coroutines.runBlocking

/** Simple demo showcasing the in-memory workflow runtime. */
fun main(): Unit = runBlocking {
    val workflow = OrderWorkflow()
    val engine = WorkflowEngine(workflow)
    val proxy = engine.proxy("order-1001")

    println("-- Executing order workflow demo --")
    println(proxy.ask(OrderCommand.CreateOrder(customerId = "customer-1")))
    println(proxy.ask(OrderCommand.AddItem("Laptop")))
    println(proxy.ask(OrderCommand.AddItem("Mouse")))
    println(proxy.ask(OrderCommand.Approve))
    println(proxy.ask(OrderCommand.Ship))

    println("Final state: ${'$'}{proxy.state()}")
    println("Events: ${'$'}{proxy.events()}")

    engine.shutdown()
}
