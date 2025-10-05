package com.example.platform.human

import com.example.platform.core.HumanInteraction
import com.example.platform.core.CommandMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface HumanTaskListener {
    suspend fun onResume(resume: HumanTaskResume)
}

data class HumanTaskRequest(
    val workflowType: String,
    val workflowId: String,
    val interaction: HumanInteraction,
    val metadata: CommandMetadata,
)

data class HumanTaskResume(
    val workflowType: String,
    val workflowId: String,
    val token: String,
    val payload: Any?,
    val metadata: CommandMetadata,
)

interface HumanTaskManager {
    fun registerListener(listener: HumanTaskListener)
    suspend fun register(request: HumanTaskRequest)
    suspend fun resume(token: String, payload: Any?, metadata: CommandMetadata = CommandMetadata())
    suspend fun pending(): List<HumanTaskRequest>
}

class InMemoryHumanTaskManager : HumanTaskManager {
    private val mutex = Mutex()
    private val tasks = mutableMapOf<String, HumanTaskRequest>()
    private var listener: HumanTaskListener? = null

    override fun registerListener(listener: HumanTaskListener) {
        this.listener = listener
    }

    override suspend fun register(request: HumanTaskRequest) {
        mutex.withLock {
            tasks[request.interaction.token] = request
        }
    }

    override suspend fun resume(token: String, payload: Any?, metadata: CommandMetadata) {
        val task = mutex.withLock { tasks.remove(token) } ?: return
        listener?.onResume(
            HumanTaskResume(
                workflowType = task.workflowType,
                workflowId = task.workflowId,
                token = token,
                payload = payload,
                metadata = metadata,
            ),
        )
    }

    override suspend fun pending(): List<HumanTaskRequest> = mutex.withLock { tasks.values.toList() }
}
