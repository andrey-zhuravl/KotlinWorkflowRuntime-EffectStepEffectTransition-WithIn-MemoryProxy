package com.example.platform.security

fun interface PolicyEngine {
    suspend fun authorize(context: AuthorizationContext): PolicyDecision
}
