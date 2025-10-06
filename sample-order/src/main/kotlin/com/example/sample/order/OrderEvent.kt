package com.example.sample.order

import com.example.platform.core.Event
import java.time.Instant

@Event
sealed interface OrderEvent {
    data class OrderCreated(val customerId: String, val amount: Long) : OrderEvent
    data class OrderApproved(val approvedBy: String, val approvedAt: Instant) : OrderEvent
    data class OrderShipped(val shippedAt: Instant) : OrderEvent
}
