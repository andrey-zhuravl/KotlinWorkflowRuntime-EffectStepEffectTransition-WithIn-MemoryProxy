package com.example.sample.order

import com.example.platform.core.Effect
import com.example.platform.core.Effects
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext

/** Represents order lifecycle statuses. */
public enum class OrderStatus { New, Created, Approved, Shipped, Cancelled }

/** Snapshot of the order state. */
public data class OrderState(
    val id: String,
    val items: List<String>,
    val status: OrderStatus
)

/** Supported commands for the order workflow. */
public sealed interface OrderCommand
public data class CreateOrder(val customer: String) : OrderCommand
public data class AddItem(val item: String) : OrderCommand
public data object Approve : OrderCommand
public data object Ship : OrderCommand
public data object Cancel : OrderCommand

/** Events emitted by the order workflow. */
public sealed interface OrderEvent
public data class OrderCreated(val customer: String) : OrderEvent
public data class ItemAdded(val item: String) : OrderEvent
public data object OrderApproved : OrderEvent
public data object OrderShipped : OrderEvent
public data object OrderCancelled : OrderEvent

/** Reply returned to callers. */
public sealed interface OrderReply {
    public val state: OrderState

    public data class Ok(override val state: OrderState) : OrderReply
    public data class Error(val message: String, override val state: OrderState) : OrderReply
}

/** Workflow implementing the order domain logic. */
public class OrderWorkflow : Workflow<OrderState, OrderCommand, OrderEvent, OrderReply> {
    override val name: String = "OrderWorkflow"

    override fun initialState(id: String): OrderState = OrderState(id, emptyList(), OrderStatus.New)

    override fun applyEvent(state: OrderState, event: OrderEvent): OrderState = when (event) {
        is OrderCreated -> state.copy(status = OrderStatus.Created)
        is ItemAdded -> state.copy(items = state.items + event.item)
        OrderApproved -> state.copy(status = OrderStatus.Approved)
        OrderShipped -> state.copy(status = OrderStatus.Shipped)
        OrderCancelled -> state.copy(status = OrderStatus.Cancelled)
    }

    override fun onCommand(
        state: OrderState,
        command: OrderCommand,
        ctx: WorkflowContext<OrderState, OrderCommand, OrderEvent, OrderReply>
    ): Effect<OrderState, OrderEvent, OrderReply> = when (command) {
        is CreateOrder -> {
            if (state.status != OrderStatus.New) {
                ctx.effects.none<OrderState, OrderEvent, OrderReply>().thenReply {
                    OrderReply.Error("Order already created", it)
                }
            } else {
                ctx.effects
                    .persist<OrderState, OrderEvent, OrderReply>(OrderCreated(command.customer))
                    .thenTransition { it.copy(status = OrderStatus.Created) }
                    .thenReply { OrderReply.Ok(it) }
            }
        }
        is AddItem -> {
            if (state.status !in setOf(OrderStatus.Created, OrderStatus.Approved)) {
                ctx.effects.none<OrderState, OrderEvent, OrderReply>().thenReply {
                    OrderReply.Error("Cannot add items when ${state.status}", it)
                }
            } else {
                ctx.effects
                    .persist<OrderState, OrderEvent, OrderReply>(ItemAdded(command.item))
                    .thenReply { OrderReply.Ok(it) }
            }
        }
        Approve -> {
            if (state.status != OrderStatus.Created) {
                ctx.effects.none<OrderState, OrderEvent, OrderReply>().thenReply {
                    OrderReply.Error("Only created orders can be approved", it)
                }
            } else {
                ctx.effects
                    .persist<OrderState, OrderEvent, OrderReply>(OrderApproved)
                    .thenTransition { it.copy(status = OrderStatus.Approved) }
                    .thenRun { println("Order ${it.id} approved at ${ctx.now()}") }
                    .thenReply { OrderReply.Ok(it) }
            }
        }
        Ship -> {
            if (state.status != OrderStatus.Approved) {
                ctx.effects.none<OrderState, OrderEvent, OrderReply>().thenReply {
                    OrderReply.Error("Only approved orders can be shipped", it)
                }
            } else {
                ctx.effects
                    .persist<OrderState, OrderEvent, OrderReply>(OrderShipped)
                    .thenTransition { it.copy(status = OrderStatus.Shipped) }
                    .thenReply { OrderReply.Ok(it) }
            }
        }
        Cancel -> {
            if (state.status !in setOf(OrderStatus.Created, OrderStatus.Approved)) {
                ctx.effects.none<OrderState, OrderEvent, OrderReply>().thenReply {
                    OrderReply.Error("Cannot cancel when ${state.status}", it)
                }
            } else {
                ctx.effects
                    .persist<OrderState, OrderEvent, OrderReply>(OrderCancelled)
                    .thenTransition { it.copy(status = OrderStatus.Cancelled) }
                    .thenReply { OrderReply.Ok(it) }
            }
        }
    }
}
