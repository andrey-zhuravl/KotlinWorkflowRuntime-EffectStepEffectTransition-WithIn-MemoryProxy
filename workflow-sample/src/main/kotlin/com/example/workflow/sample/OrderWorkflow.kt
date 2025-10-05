package com.example.workflow.sample

import com.example.workflow.core.Effect
import com.example.workflow.core.Workflow
import com.example.workflow.core.WorkflowContext

/** Represents all possible statuses for an order. */
enum class OrderStatus { NEW, CREATED, APPROVED, SHIPPED, CANCELLED }

/** Commands that can be issued to the [OrderWorkflow]. */
sealed interface OrderCommand {
    data class CreateOrder(val customerId: String) : OrderCommand
    data class AddItem(val item: String) : OrderCommand
    object Approve : OrderCommand
    object Ship : OrderCommand
    data class Cancel(val reason: String? = null) : OrderCommand
}

/** Events persisted by the [OrderWorkflow]. */
sealed interface OrderEvent {
    data class OrderCreated(val orderId: String, val customerId: String) : OrderEvent
    data class ItemAdded(val item: String) : OrderEvent
    object OrderApproved : OrderEvent
    object OrderShipped : OrderEvent
    data class OrderCancelled(val reason: String?) : OrderEvent
}

/** Replies returned to callers of the [OrderWorkflow]. */
sealed class OrderReply(open val state: OrderState) {
    data class Accepted(val message: String, override val state: OrderState) : OrderReply(state)
    data class Rejected(val reason: String, override val state: OrderState) : OrderReply(state)
}

/** State maintained by the [OrderWorkflow]. */
data class OrderState(
    val orderId: String,
    val customerId: String?,
    val items: List<String>,
    val status: OrderStatus,
    val cancelReason: String?,
    val version: Int
)

/** Workflow implementing a simple order life-cycle. */
class OrderWorkflow : Workflow<OrderState, OrderCommand, OrderEvent, OrderReply> {
    override val name: String = "order-workflow"

    override fun initialState(): OrderState =
        OrderState(orderId = "", customerId = null, items = emptyList(), status = OrderStatus.NEW, cancelReason = null, version = 0)

    override fun applyEvent(state: OrderState, event: OrderEvent): OrderState = when (event) {
        is OrderEvent.OrderCreated -> state.copy(
            orderId = event.orderId,
            customerId = event.customerId,
            status = OrderStatus.CREATED,
            cancelReason = null
        )
        is OrderEvent.ItemAdded -> state.copy(items = state.items + event.item)
        OrderEvent.OrderApproved -> state.copy(status = OrderStatus.APPROVED)
        OrderEvent.OrderShipped -> state.copy(status = OrderStatus.SHIPPED)
        is OrderEvent.OrderCancelled -> state.copy(status = OrderStatus.CANCELLED, cancelReason = event.reason)
    }

    override suspend fun onCommand(
        state: OrderState,
        command: OrderCommand,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> = when (command) {
        is OrderCommand.CreateOrder -> handleCreate(state, command, ctx)
        is OrderCommand.AddItem -> handleAddItem(state, command, ctx)
        OrderCommand.Approve -> handleApprove(state, ctx)
        OrderCommand.Ship -> handleShip(state, ctx)
        is OrderCommand.Cancel -> handleCancel(state, command, ctx)
    }

    private fun handleCreate(
        state: OrderState,
        command: OrderCommand.CreateOrder,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.status != OrderStatus.NEW) {
            return rejection(ctx, state, "Order already created")
        }
        return ctx.effects.persist(
            OrderEvent.OrderCreated(orderId = ctx.id, customerId = command.customerId)
        ).thenTransition { updated ->
            updated.copy(version = updated.version + 1)
        }.thenRun { updated ->
            println("Order ${'$'}{updated.orderId} created for customer ${'$'}{updated.customerId}")
        }.thenReply { updated ->
            OrderReply.Accepted("Order created", updated)
        }
    }

    private fun handleAddItem(
        state: OrderState,
        command: OrderCommand.AddItem,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.status !in setOf(OrderStatus.CREATED, OrderStatus.APPROVED)) {
            return rejection(ctx, state, "Cannot add items when order is ${'$'}{state.status}")
        }
        return ctx.effects.persist(
            OrderEvent.ItemAdded(command.item)
        ).thenTransition { updated ->
            updated.copy(version = updated.version + 1)
        }.thenReply { updated ->
            OrderReply.Accepted("Item added", updated)
        }
    }

    private fun handleApprove(
        state: OrderState,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.status != OrderStatus.CREATED) {
            return rejection(ctx, state, "Order must be created before approval")
        }
        return ctx.effects.persist(
            OrderEvent.OrderApproved
        ).thenTransition { updated ->
            updated.copy(status = OrderStatus.APPROVED, version = updated.version + 1)
        }.thenRun { updated ->
            println("Order ${'$'}{updated.orderId} approved")
        }.thenReply { updated ->
            OrderReply.Accepted("Order approved", updated)
        }
    }

    private fun handleShip(
        state: OrderState,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.status != OrderStatus.APPROVED) {
            return rejection(ctx, state, "Order must be approved before shipping")
        }
        return ctx.effects.persist(
            OrderEvent.OrderShipped
        ).thenTransition { updated ->
            updated.copy(status = OrderStatus.SHIPPED, version = updated.version + 1)
        }.thenRun { updated ->
            println("Shipping order ${'$'}{updated.orderId} containing ${'$'}{updated.items.size} items")
        }.thenReply { updated ->
            OrderReply.Accepted("Order shipped", updated)
        }
    }

    private fun handleCancel(
        state: OrderState,
        command: OrderCommand.Cancel,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> {
        if (state.status !in setOf(OrderStatus.CREATED, OrderStatus.APPROVED)) {
            return rejection(ctx, state, "Cannot cancel order when status is ${'$'}{state.status}")
        }
        return ctx.effects.persist(
            OrderEvent.OrderCancelled(command.reason)
        ).thenTransition { updated ->
            updated.copy(status = OrderStatus.CANCELLED, version = updated.version + 1)
        }.thenReply { updated ->
            OrderReply.Accepted("Order cancelled", updated)
        }
    }

    private fun rejection(
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>,
        state: OrderState,
        reason: String
    ): Effect<OrderState, OrderEvent, OrderReply> =
        ctx.effects.none().thenReply {
            OrderReply.Rejected(reason, it)
        }
}
