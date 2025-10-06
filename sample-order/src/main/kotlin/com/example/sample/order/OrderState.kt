package com.example.sample.order

import java.time.Instant

data class OrderState(
    val id: String,
    val status: Status = Status.New,
    val customerId: String? = null,
    val amount: Long = 0,
    val approvedBy: String? = null,
    val approvedAt: Instant? = null,
    val shippedAt: Instant? = null,
) {
    enum class Status { New, Created, Approved, Shipped }

    val exists: Boolean get() = customerId != null
    val isApproved: Boolean get() = status == Status.Approved || status == Status.Shipped

    fun created(customerId: String, amount: Long): OrderState = copy(
        status = Status.Created,
        customerId = customerId,
        amount = amount,
    )

    fun approved(user: String, at: Instant): OrderState = copy(
        status = Status.Approved,
        approvedBy = user,
        approvedAt = at,
    )

    fun shipped(at: Instant): OrderState = copy(
        status = Status.Shipped,
        shippedAt = at,
    )

    fun snapshot(): OrderView = OrderView(
        id = id,
        status = status.name,
        customerId = customerId,
        amount = amount,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        shippedAt = shippedAt,
    )
}
