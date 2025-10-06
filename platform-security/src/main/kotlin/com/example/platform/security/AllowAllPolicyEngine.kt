package com.example.platform.security

class AllowAllPolicyEngine : PolicyEngine {
    override suspend fun authorize(context: AuthorizationContext): PolicyDecision = PolicyDecision.allow()
}
