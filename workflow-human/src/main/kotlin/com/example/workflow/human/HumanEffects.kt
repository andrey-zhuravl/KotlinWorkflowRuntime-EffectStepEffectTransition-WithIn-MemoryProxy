package com.example.workflow.human

import com.example.workflow.core.StepEffect
import kotlin.time.Duration

object HumanEffects {
    fun <S, E, R> awaitHuman(
        token: String,
        ttl: Duration,
        payload: String,
        notifyChannel: String? = null
    ): StepEffect<S, E, R> = StepEffect(
        name = "await-human",
        attributes = buildMap {
            put("token", token)
            put("ttlMillis", ttl.inWholeMilliseconds)
            put("payload", payload)
            notifyChannel?.let { put("notifyChannel", it) }
        }
    )
}

data class PendingHumanTask(
    val token: String,
    val workflowType: String,
    val workflowId: String,
    val payload: String,
    val status: HumanTaskStatus,
    val expiresAtEpochMilli: Long,
    val createdAtEpochMilli: Long
)

enum class HumanTaskStatus { PENDING, RESUMED, EXPIRED, CANCELLED }

fun interface HumanTaskGateway {
    suspend fun resume(token: String, decision: HumanDecision)
}

data class HumanDecision(val decision: Decision, val payloadJson: String) {
    enum class Decision { APPROVE, REJECT }
}
