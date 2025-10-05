Here’s a production-grade follow-up spec that evolves your in-memory runtime into a distributed, durable, timer-aware, saga-capable workflow system—keeping your existing Workflow<S,C,E,R> and Effect/StepEffect/Transition DSL intact.

Title

Kotlin Workflow Runtime v2 — Distributed, Durable, Timers & Saga Orchestration

Goal

Deliver a cluster-ready Kotlin workflow runtime that:

preserves the existing Akka-inspired semantics (Effect, StepEffect, Transition, event-sourced state),

adds durable storage, distributed command processing, exactly-once state transitions (practically), durable timers, and saga orchestration,

exposes client proxies for request/reply and fire-and-forget,

includes observability, ops, and failure testing.

Scope & Deliverables

Multi-module Gradle project (Kotlin DSL, JDK 17, Kotlin 1.9+):

workflow-core: existing DSL & contracts (reused)

workflow-runtime-local: your in-memory engine (for unit tests/dev)

workflow-persistence-jdbc: JDBC Postgres impl (event store, snapshots, outbox, timer store)

workflow-transport-kafka: Kafka transport (commands, replies, signals)

workflow-cluster: node, shard assignment, recovery, backpressure

workflow-timers: durable timers (schedule, cancel, cron), dispatcher

workflow-saga: typed saga DSL + orchestrator w/ compensation

workflow-gateway: thin HTTP/gRPC gateway for external clients (optional; Kafka request/reply is default)

workflow-sample: Order + Payment sample incl. saga

workflow-integration-tests: Testcontainers (Postgres+Kafka), crash/restart/timer/saga tests

Durable state/event store (Postgres) with:

Append-only event journal, snapshots, outbox, timers, deduplication (idempotency keys)

Migrations (Flyway)

Transport (Kafka):

Topics for commands, replies, signals; key by workflowId to preserve per-instance order

Consumer groups for cluster scaling/rebalance

Clustered processing:

Node process that consumes partitions; each partition hosts many workflow instances

Per-instance single-threaded mailbox (actor loop) guaranteeing sequential command handling

Durable timers:

Schedule/cancel timers as part of Effects; timers survive restarts and fire at-least-once

Saga orchestration:

Orchestrator + DSL for multi-service transactions (Try/Confirm/Cancel or custom compensation)

Persistent saga state, retries, timeouts, compensation ordering

Observability: metrics (p50/p95), tracing (OpenTelemetry), structured logs, DLQ

Docs: KDoc + ADRs (architecture decision records)

Sample: end-to-end “Order→Payment→Shipping” happy-path + failures

CI: ./gradlew clean build test, integration tests via Testcontainers; docker-compose to run demo

Non-Goals

No Akka dependency.

No vendor-specific cloud services required (local Postgres/Kafka are enough; pluggable for cloud later).

No complex multi-tenancy isolation beyond shard/partition separation.

Technical Stack

Language: Kotlin 1.9+, Coroutines

Build/Tests: Gradle Kotlin DSL, JUnit 5, kotlin-test, Testcontainers (Postgres, Kafka)

Persistence: JDBC (HikariCP), Postgres, Flyway migrations

Transport: Apache Kafka (kotlin clients), optional HTTP/gRPC gateway (Ktor/gRPC)

Serialization: Kotlinx Serialization (JSON) for events/commands/replies (pluggable)

Observability: OpenTelemetry API (optional), plain logs by default

Project Structure (modules & key packages)
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml

workflow-core/
  src/main/kotlin/com/example/workflow/core/
    Workflow.kt
    Effect.kt
    Transition.kt
    Effects.kt         // extend with timers & outbox hooks (see below)
    Types.kt
    WorkflowContext.kt
    ReplyStrategy.kt   // ask/tell semantics

workflow-runtime-local/     // your v1 engine kept for dev/tests
  ...

workflow-persistence-jdbc/
  src/main/kotlin/com/example/workflow/persistence/jdbc/
    EventStore.kt      // SPI (interface)
    JdbcEventStore.kt  // Postgres impl: journal, snapshots, dedup
    SnapshotStore.kt
    OutboxStore.kt
    TimerStore.kt
    SqlSchemas.sql     // DDL reference; applied via Flyway

workflow-transport-kafka/
  src/main/kotlin/com/example/workflow/transport/kafka/
    KafkaConfig.kt
    CommandBus.kt      // send/consume commands
    ReplyBus.kt        // send/consume replies
    SignalBus.kt       // internal signals (rebalance, shutdown)
    Serde.kt

workflow-cluster/
  src/main/kotlin/com/example/workflow/cluster/
    ClusterNode.kt     // main node process
    PartitionLoop.kt   // 1 per Kafka partition
    InstanceRunner.kt  // per workflowId, sequential mailbox
    RebalanceListener.kt
    Backpressure.kt    // flow control

workflow-timers/
  src/main/kotlin/com/example/workflow/timers/
    TimerApi.kt        // schedule/cancel/get
    TimerDispatcher.kt // scans due timers (DB) -> emit commands
    Cron.kt            // minimal cron parser (optional)

workflow-saga/
  src/main/kotlin/com/example/workflow/saga/
    Saga.kt
    SagaDsl.kt
    SagaStore.kt
    SagaOrchestrator.kt
    Participant.kt     // TCC or custom compensate
    RetryPolicy.kt

workflow-gateway/ (optional)
  src/main/kotlin/com/example/workflow/gateway/
    HttpApi.kt         // POST /workflows/{type}/{id}/ask|tell
    GrpcApi.kt         // optional

workflow-sample/
  src/main/kotlin/com/example/workflow/sample/
    order/OrderWorkflow.kt
    payment/PaymentWorkflow.kt
    shipping/ShippingWorkflow.kt
    order/OrderSaga.kt
    Main.kt            // docker-compose up; run demo

workflow-integration-tests/
  src/test/kotlin/...  // crash/restart, timers, saga, ordering, idempotency

docker-compose.yml     // postgres + kafka + ui tools (optional)

Core Concepts (extended)
1) Workflow (unchanged API)
interface Workflow<S, C, E, R> {
  val name: String
  fun initialState(id: String): S
  fun applyEvent(state: S, event: E): S
  fun onCommand(state: S, command: C, ctx: WorkflowContext<S, C, E, R>): Effect<S, E, R>
}

2) Effect / StepEffect / Transition (extended)

Keep existing semantics:

Persist events → apply to state → apply Transition → run side-effects → reply.

Extend DSL with durable timers and outbox:

object Effects {
  fun <S,E,R> none(): StepEffect<S,E,R>
  fun <S,E,R> persist(vararg events: E): StepEffect<S,E,R>

  // New: durable timers (at-least-once)
  fun <S,E,R> schedule(key: String, at: Instant, payload: Any): StepEffect<S,E,R>
  fun <S,E,R> scheduleAfter(key: String, delay: Duration, payload: Any): StepEffect<S,E,R>
  fun <S,E,R> cancelTimer(key: String): StepEffect<S,E,R>

  // New: outbox message for external broker/topic (published atomically with events)
  fun <S,E,R> outbox(message: Any, channel: String): StepEffect<S,E,R>
}


Notes:

schedule*/cancelTimer are persisted in the same DB tx as events for atomicity.

outbox writes to Outbox table in the same tx; a dispatcher publishes to Kafka/NATS/etc.

3) WorkflowContext (extended)
interface WorkflowContext<S,C,E,R> {
  val id: String
  val effects: Effects
  fun now(): Instant
  fun correlationId(): String
  fun logger(): Logger
}

Persistence Model (JDBC/Postgres)

Tables (minimum):

event_journal(workflow_type, workflow_id, seq_nr, event_type, event_json, meta_json, timestamp, command_id UNIQUE NULLABLE)

snapshots(workflow_type, workflow_id, last_seq_nr, state_json, timestamp)

outbox(id PK, channel, key, payload_json, created_at, published_at NULL)

timers(timer_key, workflow_type, workflow_id, fire_at, payload_json, status, last_error NULL, PRIMARY KEY(workflow_type, workflow_id, timer_key))

sagas(saga_id PK, type, state_json, status, updated_at)

saga_steps(saga_id, step_id, status, attempt, last_error, metadata_json, PRIMARY KEY(saga_id, step_id))

Key guarantees & patterns:

Per-instance ordering: enforced by Kafka key = workflowId + DB seq_nr.

Idempotency: unique (workflow_type, workflow_id, command_id) prevents double-apply on retries.

Snapshots: every N events (configurable) to speed recovery; snapshot + journal tail.

Outbox: atomic with events (same tx). Background OutboxDispatcher publishes and marks published_at.

TimerStore: persisted schedule/cancel with status; TimerDispatcher periodically claims due timers (SKIP LOCKED) and emits commands.

Transport (Kafka)

Topics:

workflow.commands.v1 (key = workflowId, value = CommandEnvelope)

workflow.replies.v1 (key = correlationId, value = ReplyEnvelope)

workflow.signals.v1 (internal, optional)

Envelopes (generic; polymorphic body with type field):

data class CommandEnvelope(
  val workflowType: String,
  val workflowId: String,
  val commandType: String,
  val commandJson: String,
  val commandId: String,        // for idempotency
  val correlationId: String,
  val replyTo: String?          // reply topic, optional if HTTP/gRPC used
)
data class ReplyEnvelope(
  val correlationId: String,
  val replyType: String,
  val replyJson: String,
  val status: ReplyStatus       // OK | ERROR
)


Client proxy options:

Kafka request/reply (default): ask() publishes to commands, waits on replies by correlationId with timeout.

HTTP/gRPC gateway (optional): forward to Kafka or directly to node if collocated.

Cluster & Concurrency

ClusterNode joins a Kafka consumer group; partitions are assigned/revoked automatically.

Each PartitionLoop:

Deserializes commands, routes by workflowId to an InstanceRunner (actor loop with mailbox).

Ensures strict in-order processing per workflowId.

InstanceRunner logic:

Load current state (snapshot + journal tail) on first command or after idle eviction.

Execute onCommand to get Effect.

Start DB tx:

append events (+ metadata: commandId, correlationId),

update snapshots if threshold reached,

persist timer ops,

persist outbox messages,

commit.

Apply Transition; run thenRun side-effects after commit.

Publish replies (Kafka or HTTP/gRPC).

Backpressure: configurable max concurrent active instances per partition; LRU idle eviction with cached snapshots.

Rebalance: graceful stop of loops; in-flight commands finish (or are retried due to idempotency).

Processing semantics:

Commands: at-least-once delivery (Kafka), exactly-once state change via DB idempotency key.

Side-effects: run after commit; if they fail, state remains; reply error is returned; optional retry policies.

Timers

Effects: schedule(key, at, payload) / scheduleAfter / cancelTimer(key).

Durability: timer rows persisted atomically with events; survive crashes/restarts.

Dispatcher:

Periodically scans fire_at <= now(), claims with FOR UPDATE SKIP LOCKED, enqueues corresponding CommandEnvelope (e.g., synthetic command).

At-least-once firing; idempotency ensured by commandId (e.g., ${workflowId}:${timerKey}:${fireAt}).

Cron (optional): expand to concrete fire_at instances; store next; jitter support.

Saga Orchestration

Saga DSL:

saga("OrderPaymentShipping") {
  step("ReservePayment") {
    invoke(participant = Payment, command = Reserve(amount))
      .retry(maxAttempts = 5, backoff = exp(100.ms, max=30.s))
      .timeout(15.s)
      .onReply { state, reply -> state.copy(paymentAuthId = reply.authId) }
      .compensate { state -> CancelReservation(state.paymentAuthId) }
  }
  step("AllocateInventory") {
    invoke(participant = Inventory, command = Allocate(items))
      .compensate { state -> Deallocate(items) }
  }
  step("CreateShipment") {
    invoke(participant = Shipping, command = CreateShipment(addr))
      .onReply { s, r -> s.copy(shipmentId = r.id) }
      .compensate { s -> CancelShipment(s.shipmentId) }
  }
}


Execution:

Saga state persisted in sagas/saga_steps.

Steps can be sequential or parallel (provide parallel { ... }).

Failures trigger compensation in reverse completion order.

Steps use the Outbox to invoke participants (Kafka) or via Workflow commands.

Correlation managed by correlationId; replies mapped to step by ids.

Retries, idempotency keys, and timeouts per step.

Error Handling & Reliability

Idempotency: commandId unique per (workflowType, workflowId).

Poison pill / DLQ: messages failing deserialization or permanent logic errors routed to DLQ with context snapshot.

Crash recovery: on restart, node resumes consuming; instances recover from snapshot+journal; due timers re-fired.

Outbox publisher recovers safely (idempotent publish with producer message key).

Schema evolution: events and states are versioned; use JSON with type + version fields; add up-casters if needed.

Observability

Metrics:

Command latency p50/p95/p99, processing rate, in-flight, mailbox depth

DB tx duration, commit rate, snapshots/sec

Timer lag (now − fire_at), fired/sec

Saga step durations, retries, compensations

Outbox backlog, publish errors

Tracing: spans for command handling; attributes workflowType, workflowId, commandType, seqNr, correlationId.

Logs: structured JSON; correlationId everywhere.

Security

TLS for Kafka/HTTP/gRPC (configurable)

Secrets via env/keystore; no secrets in code

Basic RBAC at gateway (optional): who can send commands/read state

Operational Concerns

Migrations: Flyway on startup (node & gateway)

Retention: snapshot compaction; journal retention policy/archival

Rolling updates: consumer group allows zero-downtime; enable schema compatibility

Replay tools: CLI to rebuild read models or rehydrate states

Config (env + HOCON/TOML): partitions, snapshotEvery, timerScanInterval, maxActiveInstances, backoff, etc.

Backward Compatibility (from v1)

Keep workflow-runtime-local so unit tests can run without infra.

The same Workflow<S,C,E,R> and DSL run unchanged on the cluster runtime.

Add adapters: local proxy ↔ Kafka proxy (request/reply).

Acceptance Criteria

Ordering & Idempotency:

Concurrent AddItem to same order processed in order; final state correct.

Duplicate command (same commandId) does not duplicate events.

Crash/Restart:

Kill node during persist; after restart, state is consistent (no partial transitions).

Outbox/message publishing resumes; no duplicates observable by idempotent consumers.

Timers:

Schedule in T0+5s; restart node before fire; command still fires ~5s (± jitter) after T0.

Cancelled timer never fires.

Saga:

Happy path commits all steps.

Inject failure at step 3 → compensations for steps 2 and 1 executed in reverse order; final saga status = COMPENSATED.

Retries obey backoff; timeout triggers compensation.

Throughput:

Benchmark: ≥ X cmds/sec per partition (provide default target like 500 cmd/s in CI env).

p95 end-to-end latency under configurable threshold on dev hardware.

Observability:

Metrics exposed; simple Grafana dashboard JSON provided.

Traces show command→DB→outbox sequence.

Integration Tests (Testcontainers):

Idempotency, ordering, crash/restart, timers, saga compensation, outbox publish.

Demo (docker-compose up, then ./gradlew :workflow-sample:run):

End-to-end Order→Payment→Shipping with logs & metrics.

Build & Run

./gradlew clean build

./gradlew test

./gradlew :workflow-integration-tests:test (starts Testcontainers)

docker-compose up -d (postgres + kafka)

./gradlew :workflow-sample:run

Optional: :workflow-gateway:run to expose HTTP/gRPC

Exact Interfaces (guidance)

com.example.workflow.core.* (unchanged + new timer/outbox effects)

com.example.workflow.persistence.EventStore, SnapshotStore, OutboxStore, TimerStore

com.example.workflow.transport.CommandBus, ReplyBus

com.example.workflow.cluster.ClusterNode

com.example.workflow.timers.TimerDispatcher

com.example.workflow.saga.* (Saga, SagaDsl, SagaOrchestrator, Participant, RetryPolicy)

com.example.workflow.sample.* (Order, Payment, Shipping, Saga, Main)

Notes on Semantics

Effect ordering remains: persist/apply → transition → side-effects → reply.

Timers & outbox participate in the same DB transaction as events for atomicity.

At-least-once delivery externally; rely on idempotent handlers and unique keys.

Saga is opt-in; a workflow can emit saga commands or be a saga participant itself.

If you want, I can tailor this spec to the exact infra you’ll run (e.g., replace Kafka with NATS, or Postgres with MySQL/Cloud SQL) and set concrete throughput/SLO targets.
