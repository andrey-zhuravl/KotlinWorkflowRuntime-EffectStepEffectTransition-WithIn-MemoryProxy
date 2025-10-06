package com.example.sample.order

import com.example.platform.core.Effect
import com.example.platform.core.Effects
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext
import com.example.platform.runtime.local.WorkflowEngine
import com.example.testing.Test
import com.example.testing.assertEquals
import com.example.testing.assertIs
import kotlinx.coroutines.runBlocking

class GuardDslTest {
    @Test
    fun guardPassesAndExecutesPlan() = runBlocking {
        val sideEffects = mutableListOf<String>()
        val workflow = GuardWorkflow(initialReady = true, sideEffects)
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("guard-1")

        val reply = proxy.ask(GuardCommand.Perform)
        assertIs<GuardReply.Ok>(reply)
        assertEquals("done", reply.state.status)
        assertEquals(
            listOf(
                GuardEvent.Recorded("first"),
                GuardEvent.Recorded("second")
            ),
            proxy.events()
        )
        assertEquals("done", proxy.state().status)
        assertEquals(listOf("done"), sideEffects)
    }

    @Test
    fun guardRejectsWithoutSideEffects() = runBlocking {
        val sideEffects = mutableListOf<String>()
        val workflow = GuardWorkflow(initialReady = false, sideEffects)
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("guard-2")

        val reply = proxy.ask(GuardCommand.Perform)
        assertIs<GuardReply.Rejected>(reply)
        val rejection = reply as GuardReply.Rejected
        assertEquals("Not ready", rejection.message)
        assertEquals(emptyList<GuardEvent>(), proxy.events())
        assertEquals("initial", proxy.state().status)
        assertEquals(emptyList<String>(), sideEffects)
    }

    @Test
    fun guardThrowsWhenConfigured() = runBlocking {
        val workflow = GuardWorkflow(initialReady = false, sideEffects = mutableListOf())
        val engine = WorkflowEngine(workflow)
        val proxy = engine.proxyFor("guard-3")

        try {
            proxy.ask(GuardCommand.Throw)
            error("Expected failure")
        } catch (t: IllegalStateException) {
            assertEquals("Cannot execute (guard=must-be-ready)", t.message)
        }
        assertEquals(emptyList<GuardEvent>(), proxy.events())
        assertEquals("initial", proxy.state().status)
    }
}

private data class GuardState(
    val ready: Boolean,
    val status: String,
    val history: List<String>
)

private sealed interface GuardCommand {
    data object Perform : GuardCommand
    data object Throw : GuardCommand
}

private sealed interface GuardEvent {
    data class Recorded(val marker: String) : GuardEvent
    data class ReadyChanged(val ready: Boolean) : GuardEvent
}

private sealed interface GuardReply {
    val state: GuardState

    data class Ok(override val state: GuardState) : GuardReply
    data class Rejected(val message: String, override val state: GuardState) : GuardReply
}

private class GuardWorkflow(
    private val initialReady: Boolean,
    private val sideEffects: MutableList<String>
) : Workflow<GuardState, GuardCommand, GuardEvent, GuardReply> {
    override val name: String = "GuardWorkflow"

    override fun initialState(id: String): GuardState =
        GuardState(initialReady, status = "initial", history = emptyList())

    override fun applyEvent(state: GuardState, event: GuardEvent): GuardState = when (event) {
        is GuardEvent.Recorded -> state.copy(history = state.history + event.marker)
        is GuardEvent.ReadyChanged -> state.copy(ready = event.ready)
    }

    override fun onCommand(
        state: GuardState,
        command: GuardCommand,
        ctx: WorkflowContext<GuardState, GuardCommand, GuardEvent, GuardReply>
    ): Effect<GuardState, GuardEvent, GuardReply> = when (command) {
        GuardCommand.Perform -> ctx.guard("ready-check", state, command) { s, _ -> s.ready }
            .orReject { GuardReply.Rejected("Not ready", it) }
            .then {
                Effects
                    .persist<GuardState, GuardEvent, GuardReply>(
                        GuardEvent.Recorded("first"),
                        GuardEvent.Recorded("second")
                    )
                    .thenTransition { it.copy(status = "done") }
                    .thenRun { sideEffects += it.status }
            }
            .thenReply { GuardReply.Ok(it) }

        GuardCommand.Throw -> {
            ctx.guard("must-be-ready", state, command) { s, _ -> s.ready }
                .orThrow("Cannot execute")
            Effects
                .none<GuardState, GuardEvent, GuardReply>()
                .thenReply { GuardReply.Ok(it) }
        }
    }
}
