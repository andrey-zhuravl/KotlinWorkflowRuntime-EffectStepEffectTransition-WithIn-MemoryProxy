package com.example.sample.order

import java.time.Instant

data class OrderView(
    val id: String,
    val status: String,
    val customerId: String?,
    val amount: Long,
    val approvedBy: String?,
    val approvedAt: Instant?,
    val shippedAt: Instant?,
)
