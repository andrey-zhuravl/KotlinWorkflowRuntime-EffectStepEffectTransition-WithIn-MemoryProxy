# Kotlin Workflow Platform vNext (Developer Experience First)

This repository bootstraps the **Kotlin Workflow Platform vNext** initiative. The focus is a business-first
developer experience that keeps domain code clean while the runtime and tooling handle infrastructure
concerns such as transport, persistence, orchestration, security, and observability.

The initial commit establishes a multi-module Gradle build, foundational workflow DSL primitives, and a
sample `order` workflow illustrating the desired ergonomics.

## Project Layout

```
settings.gradle.kts
build.gradle.kts
platform-core/                # Domain-facing APIs & DSL primitives
platform-ksp/                 # (placeholder) KSP processors for code generation
platform-runtime/             # (placeholder) Runtime engine, clustering, sharding
platform-persistence-jdbc/    # (placeholder) Postgres persistence implementation
platform-transport-kafka/     # (placeholder) Kafka transport adapters
platform-timers/              # (placeholder) Durable timers subsystem
platform-saga/                # (placeholder) Saga orchestration DSL & engine
platform-projections/         # (placeholder) Projection runner infrastructure
platform-human/               # (placeholder) Human-in-the-loop gateways
platform-security/            # (placeholder) RBAC/OPA integration
platform-admin/               # (placeholder) Admin surfaces & diagnostics
platform-devtools/            # (placeholder) Gradle plugin & scenario tooling
sample-order/                 # Sample workflow showing business-only code
```

The placeholder modules contain lightweight `*Placeholder` objects as scaffolding. They document the
intended responsibilities and keep the build graph ready for incremental delivery.

## Core Workflow DSL

`platform-core` provides type-safe contracts for implementing business workflows:

* `Workflow<S, C, E, R>` – domain code defines state evolution and command handling.
* `WorkflowContext` – runtime supplied context exposing `effects`, clocks, logging, guard helpers, and
  multi-tenancy metadata.
* `Effect` – immutable chain describing what the runtime must execute: event persistence, transitions,
  side effects, durable timers, outbox messages, child workflows, and human interactions.
* `Effects` – factory exposed via the context; business code builds effect chains using fluent combinators.
* Guard DSL – `ctx.guard(...).orReject { ... }.then { ... }` to express invariants without infrastructure
  plumbing.
* Marker annotations (`@WorkflowService`, `@Command`, `@Event`, `@Reply`, `@Projection`, `@Saga`) that will
  drive KSP code generation in future iterations.

The DSL emphasises strong typing and business readability, mirroring the design goals outlined in the
specification.

## Sample: Order Workflow

The `sample-order` module contains an `OrderWorkflow` showcasing the ergonomics:

```kotlin
@WorkflowService("order")
class OrderWorkflow : Workflow<OrderState, OrderCommand, OrderEvent, OrderReply> {
  override fun applyEvent(state: OrderState, event: OrderEvent): OrderState = when (event) {
    is OrderEvent.OrderCreated -> state.created(event.customerId, event.amount)
    is OrderEvent.OrderApproved -> state.approved(event.approvedBy, event.approvedAt)
    is OrderEvent.OrderShipped -> state.shipped(event.shippedAt)
  }

  override suspend fun onCommand(...): Effect<OrderState, OrderEvent, OrderReply> = when (command) {
    is OrderCommand.Create -> ctx.effects
      .persist(OrderEvent.OrderCreated(command.customerId, command.amount))
      .thenTransition { it.created(command.customerId, command.amount) }
      .thenReply { OrderReply.Accepted(it.snapshot()) }
    is OrderCommand.Ship -> ctx.guard("onlyApproved", state, command) { it.isApproved }
      .orReject { OrderReply.NotAllowed("approve first", it.snapshot()) }
      .then { ctx.effects.persist(OrderEvent.OrderShipped(ctx.now()))
        .thenTransition { it.shipped(ctx.now()) }
        .thenReply { OrderReply.Accepted(it.snapshot()) } }
    // ...
  }
}
```

Business files remain free from transport, persistence, or serialization noise—the runtime and upcoming KSP
code generation will provide the infrastructure plumbing.

## Build & Usage

* Build everything: `./gradlew clean build`
* Run the sample stub: `./gradlew :sample-order:run`

Future work will flesh out each module with the durable runtime, persistence adapters, transport integration,
observability, dev tools, and admin APIs described in the vNext specification.
