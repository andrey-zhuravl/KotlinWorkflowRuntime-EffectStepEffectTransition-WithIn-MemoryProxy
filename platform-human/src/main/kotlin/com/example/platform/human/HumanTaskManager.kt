package com.example.platform.human

import com.example.platform.core.CommandMetadata

interface HumanTaskManager {
    fun registerListener(listener: HumanTaskListener)
    suspend fun register(request: HumanTaskRequest)
    suspend fun resume(token: String, payload: Any?, metadata: CommandMetadata = CommandMetadata())
    suspend fun pending(): List<HumanTaskRequest>
}
