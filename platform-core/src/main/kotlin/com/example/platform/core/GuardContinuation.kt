package com.example.platform.core

class GuardContinuation<S, C, E, R>
internal constructor(
    private val name: String,
    private val context: WorkflowContext<S, C, E, R>,
    private val state: S,
    private val command: C,
    private val passed: Boolean,
    private val rejectionBuilder: (GuardContext<S, C>) -> R,
) {
    fun then(block: () -> Effect<S, E, R>): Effect<S, E, R> {
        return if (passed) {
            block()
        } else {
            val reply = rejectionBuilder(GuardContext(name, state, command))
            context.effects.reply { reply }
        }
    }
}
