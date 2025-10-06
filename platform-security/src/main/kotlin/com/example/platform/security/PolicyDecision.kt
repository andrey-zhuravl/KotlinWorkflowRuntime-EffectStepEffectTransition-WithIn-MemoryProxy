package com.example.platform.security

data class PolicyDecision(
    val allowed: Boolean,
    val reason: String? = null,
) {
    companion object {
        fun allow(): PolicyDecision = PolicyDecision(true)
        fun deny(reason: String): PolicyDecision = PolicyDecision(false, reason)
    }
}
