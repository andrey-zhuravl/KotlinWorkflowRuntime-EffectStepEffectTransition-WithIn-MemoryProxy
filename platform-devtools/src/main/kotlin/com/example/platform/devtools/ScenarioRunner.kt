package com.example.platform.devtools

import com.example.platform.core.CommandMetadata
import com.example.platform.runtime.CommandEnvelope
import com.example.platform.runtime.WorkflowRuntime
import kotlinx.coroutines.runBlocking

data class ScenarioCommand(
    val workflowType: String,
    val workflowId: String,
    val command: Any,
    val metadata: CommandMetadata = CommandMetadata(),
)

data class ScenarioResult(val replies: List<Any?>)

class ScenarioRunner(private val runtime: WorkflowRuntime) {
    fun run(commands: List<ScenarioCommand>): ScenarioResult = runBlocking {
        val replies = mutableListOf<Any?>()
        for (command in commands) {
            val reply = runtime.dispatch(
                CommandEnvelope(
                    workflowType = command.workflowType,
                    workflowId = command.workflowId,
                    command = command.command,
                    metadata = command.metadata,
                ),
            )
            replies += reply
        }
        ScenarioResult(replies)
    }
}
