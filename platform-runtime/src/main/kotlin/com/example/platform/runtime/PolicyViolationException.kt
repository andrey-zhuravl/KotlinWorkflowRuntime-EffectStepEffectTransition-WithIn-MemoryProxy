package com.example.platform.runtime

import com.example.platform.security.PolicyDecision

data class PolicyViolationException(val decision: PolicyDecision) : RuntimeException(
    "Command rejected: ${'$'}decision",
)
