package com.example.platform.saga

data class RetryPolicy(
    val maxAttempts: Int,
    val delayMillis: Long,
) {
    companion object {
        fun none(): RetryPolicy = RetryPolicy(1, 0)
    }
}
