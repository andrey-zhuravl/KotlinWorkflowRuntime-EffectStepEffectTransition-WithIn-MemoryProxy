package com.example.sample.order

import java.time.Instant

data class ShippingNotification(
    val orderId: String,
    val shippedAt: Instant,
)
