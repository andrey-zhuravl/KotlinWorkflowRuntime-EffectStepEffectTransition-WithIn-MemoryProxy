package com.example.sample.order

import com.example.platform.runtime.local.WorkflowEngine
import kotlinx.coroutines.runBlocking

/** Entry point demonstrating the order workflow runtime. */
public fun main() = runBlocking {
    val workflow = OrderWorkflow()
    val engine = WorkflowEngine(workflow)
    val proxy = engine.proxyFor("order-1")

    println("== Starting order workflow demo ==")
    println(proxy.ask(CreateOrder("Alice")))
    println(proxy.ask(AddItem("Apple")))
    println(proxy.ask(AddItem("Banana")))
    println(proxy.ask(Approve))
    println(proxy.ask(Ship))

    println("Final state: ${proxy.state()}")
    println("Events: ${proxy.events()}")
}
