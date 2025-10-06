package com.example.sample.order

import com.example.platform.core.Command

@Command
sealed interface OrderCommand {
    val id: String

    data class Create(
        override val id: String,
        val customerId: String,
        val amount: Long,
    ) : OrderCommand

    data class Approve(
        override val id: String,
        val user: String,
    ) : OrderCommand

    data class Ship(
        override val id: String,
    ) : OrderCommand
}
