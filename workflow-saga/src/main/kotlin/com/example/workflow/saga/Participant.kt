package com.example.workflow.saga

/**
 * Saga participant capable of executing a command and producing a reply.
 */
fun interface Participant<State, Command, Reply> {
    suspend fun execute(state: State, command: Command): Reply

    @Suppress("UNCHECKED_CAST")
    fun withCommand(command: Command): Participant<State, Command, Reply> = Participant { state, _ ->
        execute(state, command)
    } as Participant<State, Command, Reply>
}
