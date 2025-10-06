package com.example.platform.human

import com.example.platform.core.CommandMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
