package com.example.workflow.saga

/**
 * Collects saga steps using a fluent DSL.
 */
class SagaDsl<State> internal constructor() {
    private val steps = mutableListOf<SagaStep<State>>()

    fun step(name: String, block: StepBuilder<State>.() -> Unit) {
        val builder = StepBuilder<State>(name).apply(block)
        steps += builder.build()
    }

    fun build(): SagaDefinition<State> = SagaDefinition(steps.toList())

    class StepBuilder<State>(private val name: String) {
        private lateinit var participant: Participant<State, *, *>
        private var compensation: Compensation<State>? = null

        fun <Command, Reply> invoke(participant: Participant<State, Command, Reply>, command: Command) {
            this.participant = participant.withCommand(command)
        }

        fun compensate(block: suspend (State) -> Any) {
            this.compensation = Compensation(name, block)
        }

        fun build(): SagaStep<State> = SagaStep(name, participant, compensation)
    }
}

data class SagaDefinition<State>(val steps: List<SagaStep<State>>)

data class SagaStep<State>(
    val name: String,
    val participant: Participant<State, *, *>,
    val compensation: Compensation<State>?
)

data class Compensation<State>(val name: String, val block: suspend (State) -> Any)
