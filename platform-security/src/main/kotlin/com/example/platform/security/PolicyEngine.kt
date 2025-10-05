package com.example.platform.security

import com.example.platform.core.CommandMetadata

data class AuthorizationContext(
    val workflowType: String,
    val workflowId: String,
    val command: Any,
    val metadata: CommandMetadata,
)

data class PolicyDecision(
    val allowed: Boolean,
    val reason: String? = null,
) {
    companion object {
        fun allow(): PolicyDecision = PolicyDecision(true)
        fun deny(reason: String): PolicyDecision = PolicyDecision(false, reason)
    }
}

fun interface PolicyEngine {
    suspend fun authorize(context: AuthorizationContext): PolicyDecision
}

class AllowAllPolicyEngine : PolicyEngine {
    override suspend fun authorize(context: AuthorizationContext): PolicyDecision = PolicyDecision.allow()
}
