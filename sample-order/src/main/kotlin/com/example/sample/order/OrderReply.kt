package com.example.sample.order

import com.example.platform.core.Reply

@Reply
sealed interface OrderReply {
    data class Accepted(val order: OrderView) : OrderReply
    data class AlreadyExists(val order: OrderView) : OrderReply
    data class NotAllowed(val reason: String, val order: OrderView) : OrderReply
    data class NotFound(val id: String) : OrderReply
}
