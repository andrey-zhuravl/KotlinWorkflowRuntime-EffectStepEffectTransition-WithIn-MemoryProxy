package com.example.platform.runtime

import com.example.platform.security.PolicyDecision

fun PolicyDecision.ensureAllowed() {
    if (!allowed) {
        throw PolicyViolationException(this)
    }
}
