package com.example.sample.order

import com.example.platform.core.Effect
import com.example.platform.core.Effects
import com.example.platform.core.StepEffect
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext
import com.example.platform.runtime.local.WorkflowEngine
import com.example.testing.Test
import com.example.testing.assertEquals
import com.example.testing.assertNotNull
import com.example.testing.assertNull
import kotlinx.coroutines.runBlocking

class EffectDslTest {
    @Test
    fun chainingSupportsSideEffectsAndReply() {
        val effect = Effects
            .none<Int, String, String>()
            .thenRun { }
            .thenReply { "state-$it" }

        val result = effect as Effect.Result<Int, String, String>
        assertEquals(emptyList<String>(), result.events)
        assertEquals(1, result.sideEffects.size)
        assertEquals("state-0", result.reply?.invoke(0))
    }

    @Test
    fun persistAndTransition() {
        val effect = Effects
            .persist<Int, String, String>("evt1", "evt2")
            .thenTransition { it + 1 }
            .thenReply { "value-$it" }

        val result = effect as Effect.Result<Int, String, String>
        assertEquals(listOf("evt1", "evt2"), result.events)
        assertNotNull(result.transition)
        assertEquals("value-5", result.reply?.invoke(5))
    }

    @Test
    fun transitionAppliesAfterEvents() {
        val effect = Effects
            .persist<Int, Int, Unit>(1, 2, 3)
            .thenTransition { it + 10 }
            .thenNoReply()

        val result = effect as Effect.Result<Int, Int, Unit>
        var state = 0
        for (event in result.events) {
            state += event
        }
        val transitioned = result.transition?.toState?.invoke(state) ?: state
        assertEquals(16, transitioned)
        assertNull(result.reply)
    }

    @Test
    fun eventsAreAppliedInOrderBeforeTransition() = runBlocking {
        val applied = mutableListOf<Int>()
        val workflow = RecordingWorkflow(applied, sideEffects = mutableListOf())
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("dsl-order-1")

        val reply = proxy.ask(TestCommand.Execute)
        assertEquals(16, reply)
        assertEquals(listOf(1, 2, 3), applied)
        assertEquals(16, proxy.state())
        assertEquals(listOf(1, 2, 3), proxy.events())
    }

    @Test
    fun transitionRunsAfterEvents() = runBlocking {
        val workflow = RecordingWorkflow(mutableListOf(), sideEffects = mutableListOf())
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("dsl-order-2")

        proxy.ask(TestCommand.Execute)
        assertEquals(16, proxy.state())
    }

    @Test
    fun sideEffectsSeeFinalState() = runBlocking {
        val seen = mutableListOf<String>()
        val workflow = RecordingWorkflow(mutableListOf(), sideEffects = seen)
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("dsl-order-3")

        proxy.ask(TestCommand.Execute)
        assertEquals(listOf("16"), seen)
    }

    @Test
    fun sideEffectFailureDoesNotRollback() = runBlocking {
        val workflow = object : Workflow<Int, TestCommand, Int, Int> {
            override val name: String = "FailingSideEffect"
            override fun initialState(id: String): Int = 0
            override fun applyEvent(state: Int, event: Int): Int = state + event
            override fun onCommand(
                state: Int,
                command: TestCommand,
                ctx: WorkflowContext<Int, TestCommand, Int, Int>
            ): Effect<Int, Int, Int> {
                return Effects
                    .persist<Int, Int, Int>(1, 2, 3)
                    .thenTransition { it + 10 }
                    .thenRun { throw IllegalStateException("boom") }
                    .thenReply { it }
            }
        }
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("dsl-order-4")

        try {
            proxy.ask(TestCommand.Execute)
            error("Expected exception")
        } catch (t: IllegalStateException) {
            assertEquals("boom", t.message)
        }
        assertEquals(16, proxy.state())
        assertEquals(listOf(1, 2, 3), proxy.events())
    }

    @Test
    fun onlySingleTransitionAllowed() {
        val step: StepEffect<Int, Int, Int> = Effects.persist(1)
        try {
            step
                .thenTransition { it + 1 }
                .thenTransition { it + 2 }
            error("Expected IllegalStateException")
        } catch (t: IllegalStateException) {
            assertEquals("Transition already set", t.message)
        }
    }
}

private enum class TestCommand { Execute }

private class RecordingWorkflow(
    private val applied: MutableList<Int>,
    private val sideEffects: MutableList<String>
) : Workflow<Int, TestCommand, Int, Int> {
    override val name: String = "RecordingWorkflow"

    override fun initialState(id: String): Int = 0

    override fun applyEvent(state: Int, event: Int): Int {
        applied += event
        return state + event
    }

    override fun onCommand(
        state: Int,
        command: TestCommand,
        ctx: WorkflowContext<Int, TestCommand, Int, Int>
    ): Effect<Int, Int, Int> {
        return Effects
            .persist<Int, Int, Int>(1, 2, 3)
            .thenTransition { it + 10 }
            .thenRun { sideEffects += it.toString() }
            .thenReply { it }
    }
}
