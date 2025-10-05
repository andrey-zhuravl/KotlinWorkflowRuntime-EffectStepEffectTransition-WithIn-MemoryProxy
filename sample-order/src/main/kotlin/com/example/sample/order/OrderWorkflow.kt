package com.example.sample.order

import com.example.platform.core.Command
import com.example.platform.core.CommandMetadata
import com.example.platform.core.Effect
import com.example.platform.core.Event
import com.example.platform.core.OutboxMessage
import com.example.platform.core.Reply
import com.example.platform.core.TimerCommand
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext
import com.example.platform.core.WorkflowService
import java.time.Duration
import java.time.Instant
import java.util.UUID

@WorkflowService("order")
class OrderWorkflow : Workflow<OrderState, OrderCommand, OrderEvent, OrderReply> {
    override val name: String = "order"

    override fun initialState(id: String): OrderState = OrderState(id = id)

    override fun applyEvent(state: OrderState, event: OrderEvent): OrderState = when (event) {
        is OrderEvent.OrderCreated -> state.created(event.customerId, event.amount)
        is OrderEvent.OrderApproved -> state.approved(event.approvedBy, event.approvedAt)
        is OrderEvent.OrderShipped -> state.shipped(event.shippedAt)
    }

    override suspend fun onCommand(
        state: OrderState,
        command: OrderCommand,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> = when (command) {
        is OrderCommand.Create -> handleCreate(state, command, ctx)
        is OrderCommand.Approve -> handleApprove(state, command, ctx)
        is OrderCommand.Ship -> handleShip(state, command, ctx)
    }

    private fun handleCreate(
        state: OrderState,
        command: OrderCommand.Create,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.exists) {
            return ctx.effects.reply { OrderReply.AlreadyExists(state.snapshot()) }
        }

        return ctx.effects
            .persist(OrderEvent.OrderCreated(command.customerId, command.amount))
            .thenTransition { it.created(command.customerId, command.amount) }
            .scheduleAfter(
                key = "approval-deadline",
                delay = Duration.ofHours(24),
                payload = TimerCommand(
                    command = OrderCommand.Ship(command.id),
                    metadata = CommandMetadata(correlationId = ctx.correlationId, tenantId = ctx.tenantId),
                ),
            )
            .thenRun { ctx.logger.info("order ${'$'}{it.id} created") }
            .thenReply { OrderReply.Accepted(it.snapshot()) }
    }

    private fun handleApprove(
        state: OrderState,
        command: OrderCommand.Approve,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (!state.exists) {
            return ctx.effects.reply { OrderReply.NotFound(command.id) }
        }
        if (state.isApproved) {
            return ctx.effects.reply { OrderReply.Accepted(state.snapshot()) }
        }

        return ctx.effects
            .persist(
                OrderEvent.OrderApproved(
                    approvedBy = command.user,
                    approvedAt = ctx.now()
                )
            )
            .thenTransition { it.approved(command.user, ctx.now()) }
            .thenReply { OrderReply.Accepted(it.snapshot()) }
    }

    private fun handleShip(
        state: OrderState,
        command: OrderCommand.Ship,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (!state.exists) {
            return ctx.effects.reply { OrderReply.NotFound(command.id) }
        }
        if (state.status == OrderState.Status.Shipped) {
            return ctx.effects.reply { OrderReply.Accepted(state.snapshot()) }
        }

        return ctx
            .guard("onlyApproved", state, command) { it.isApproved }
            .orReject { OrderReply.NotAllowed("approve first", it.snapshot()) }
            .then {
                val shippedAt = ctx.now()
                ctx.effects
                    .persist(OrderEvent.OrderShipped(shippedAt))
                    .thenTransition { it.shipped(shippedAt) }
                    .cancelTimer("approval-deadline")
                    .outbox(
                        message = OutboxMessage(
                            id = UUID.randomUUID().toString(),
                            payload = ShippingNotification(it.id, shippedAt),
                            correlationId = ctx.correlationId,
                            tenantId = ctx.tenantId,
                        ),
                        channel = "orders.shipped",
                    )
                    .thenReply { OrderReply.Accepted(it.snapshot()) }
            }
    }
}

@Command
sealed interface OrderCommand {
    val id: String

    data class Create(
        override val id: String,
        val customerId: String,
        val amount: Long
    ) : OrderCommand

    data class Approve(
        override val id: String,
        val user: String
    ) : OrderCommand

    data class Ship(
        override val id: String
    ) : OrderCommand
}

@Event
sealed interface OrderEvent {
    data class OrderCreated(val customerId: String, val amount: Long) : OrderEvent
    data class OrderApproved(val approvedBy: String, val approvedAt: Instant) : OrderEvent
    data class OrderShipped(val shippedAt: Instant) : OrderEvent
}

@Reply
sealed interface OrderReply {
    data class Accepted(val order: OrderView) : OrderReply
    data class AlreadyExists(val order: OrderView) : OrderReply
    data class NotAllowed(val reason: String, val order: OrderView) : OrderReply
    data class NotFound(val id: String) : OrderReply
}

data class OrderState(
    val id: String,
    val status: Status = Status.New,
    val customerId: String? = null,
    val amount: Long = 0,
    val approvedBy: String? = null,
    val approvedAt: Instant? = null,
    val shippedAt: Instant? = null
) {
    enum class Status { New, Created, Approved, Shipped }

    val exists: Boolean get() = customerId != null
    val isApproved: Boolean get() = status == Status.Approved || status == Status.Shipped

    fun created(customerId: String, amount: Long): OrderState = copy(
        status = Status.Created,
        customerId = customerId,
        amount = amount
    )

    fun approved(user: String, at: Instant): OrderState = copy(
        status = Status.Approved,
        approvedBy = user,
        approvedAt = at
    )

    fun shipped(at: Instant): OrderState = copy(
        status = Status.Shipped,
        shippedAt = at
    )

    fun snapshot(): OrderView = OrderView(
        id = id,
        status = status.name,
        customerId = customerId,
        amount = amount,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        shippedAt = shippedAt
    )
}

data class OrderView(
    val id: String,
    val status: String,
    val customerId: String?,
    val amount: Long,
    val approvedBy: String?,
    val approvedAt: Instant?,
    val shippedAt: Instant?
)

data class ShippingNotification(
    val orderId: String,
    val shippedAt: Instant,
)
