# Kotlin Workflow Runtime — Effect/StepEffect/Transition Engine

Below is a complete, precise technical specification for OpenAI Codex to generate a production-quality, runnable Kotlin implementation that ports key workflow concepts inspired by Akka (Effect, StepEffect, Transition) and provides an in-memory proxy/engine to execute workflows. The deliverable must be self-contained, compile, run, and include tests and a sample workflow.

Title
Kotlin Workflow Runtime: Effect/StepEffect/Transition with In-Memory Proxy

Goal
Implement a minimal yet robust workflow runtime in Kotlin that models Akka-style Effect/StepEffect/Transition semantics and provides an in-memory proxy/engine to execute workflows. The runtime must be generic and type-safe, support event-sourced state evolution via events, allow state transitions via a Transition step, and execute side-effects in a controlled manner. Include a sample domain workflow and unit tests.

Scope and Deliverables
- Complete, runnable Kotlin code (no placeholders) with:
  - Gradle Kotlin DSL build (JDK 17, Kotlin 1.9+, Coroutines, JUnit 5)
  - Core workflow abstractions (Effect, StepEffect, Transition, Workflow)
  - Effects DSL (e.g., Effects.none(), Effects.persist(...), andThen/thenRun, thenTransition, thenReply)
  - In-memory workflow engine and proxy to send commands and receive typed replies
  - In-memory state/event store and command mailbox ensuring sequential processing per workflow instance
  - A sample workflow (e.g., Order workflow) demonstrating features
  - Unit tests covering essential behavior and edge cases
  - Minimal main function to run a demo
  - KDoc for public APIs and clear comments
- No external frameworks beyond kotlinx-coroutines and test libs.

Non-Goals
- No Akka dependency, no distributed clustering, no persistence beyond in-memory.
- No timers/schedulers beyond straightforward coroutines if needed.

Technical Stack
- Language: Kotlin 1.9+
- JDK: 17
- Build: Gradle Kotlin DSL
- Concurrency: kotlinx-coroutines
- Tests: JUnit 5, kotest assertions or kotlin-test (standard)
- Logging: standard println or simple logger (no SLF4J required)

Project Structure (files and packages)
- settings.gradle.kts
- build.gradle.kts
- src/main/kotlin/com/example/workflow/core/
  - Workflow.kt
  - Effect.kt
  - Transition.kt
  - Effects.kt
  - Types.kt (common aliases or small helpers)
- src/main/kotlin/com/example/workflow/runtime/
  - InMemoryStores.kt
  - WorkflowEngine.kt
  - WorkflowProxy.kt
- src/main/kotlin/com/example/workflow/sample/
  - OrderWorkflow.kt
  - Main.kt
- src/test/kotlin/com/example/workflow/
  - OrderWorkflowTest.kt
  - EngineConcurrencyTest.kt
  - EffectDslTest.kt

Core Concepts and API Contracts
1) Workflow
- A type-safe interface representing the domain logic. Parametric in:
  - S: State
  - C: Command
  - E: Event
  - R: Reply (command response type)
- Responsibilities:
  - name: String
  - initialState(): S
  - applyEvent(state: S, event: E): S
  - onCommand(state: S, command: C, ctx: WorkflowContext<S, C, E, R>): Effect<S, E, R>
- WorkflowContext exposes workflow metadata and helpers (e.g., id, clock if needed; at minimum, id and an Effects DSL entry point). Keep it small and testable.

2) Effect / StepEffect / Transition
- Model the Akka-like DSL to compose event persistence, side-effects, transitions, and replies:
  - Effect<S, E, R> is the terminal representation to be executed by the runtime.
  - StepEffect<S, E, R> is a builder returned while composing steps, allowing:
    - thenRun(suspend (S) -> Unit): StepEffect<S, E, R> — run side-effect after state is updated
    - thenTransition((S) -> S): StepEffect<S, E, R> — compute final new state after events are applied
    - thenReply((S) -> R): Effect<S, E, R> — finalize the builder with a reply
    - thenNoReply(): Effect<S, E, R> — finalize without reply
  - Transition encapsulates the intent to move to another state after events are applied:
    - data class Transition<S>(val toState: (S) -> S)
- Effects DSL:
  - Effects.none<S, E, R>(): StepEffect<S, E, R> — no events
  - Effects.persist<S, E, R>(vararg events: E): StepEffect<S, E, R>
  - Ensure builder chaining is type-safe and operational.
- The runtime will:
  - Persist events (in-memory), apply them in sequence to produce updated state
  - Apply Transition (if any) to produce the final state for this command
  - Execute side-effects (thenRun) with the latest state
  - Produce a reply if specified by thenReply; otherwise no reply

3) In-Memory Runtime and Proxy
- In-memory event store and state store:
  - For each workflow instance id (String), maintain:
    - current state S
    - a list of events [E]
  - Store implementations must be thread-safe.
- Proxy/Engine semantics:
  - A WorkflowEngine can register a Workflow and create a proxy/handle for workflow instances identified by ids (String)
  - Each instance has a mailbox (Channel) of command envelopes; process commands sequentially per instance
  - Envelope includes command C and a CompletableDeferred<R?> (reply can be nullable; or use a Result wrapper)
  - Public API on proxy:
    - suspend fun ask(command: C): R
    - suspend fun tell(command: C): Unit (fire-and-forget, no reply expected)
    - fun state(): S (returns the latest state)
    - fun events(): List<E> (returns a copy of event log)
  - The engine executes Effect returned by Workflow.onCommand:
    - Persist events -> applyEvent -> state update
    - Apply Transition (if any)
    - Run side-effects in the workflow scope
    - Complete reply if provided or complete with an error if DSL indicates reply expected but missing
  - Ensure resource cleanup (scope cancellation) and graceful shutdown methods for engine/proxies

4) Error Handling and Concurrency
- Use a per-instance Mutex or single-threaded actor loop (Channel) to serialize execution
- Side-effects exceptions:
  - If side-effect fails, the state and events must already be persisted and updated; fail the command with an error but do NOT roll back state
  - Surface errors to the caller (ask) as exceptions
- Invalid DSL usage (e.g., multiple replies) should be prevented by types; otherwise fail fast with clear exceptions

5) Sample Workflow: Order
- Domain:
  - State: OrderState with fields id, items, status (enum: New, Created, Approved, Shipped, Cancelled)
  - Commands: CreateOrder, AddItem, Approve, Ship, Cancel
  - Events: OrderCreated, ItemAdded, OrderApproved, OrderShipped, OrderCancelled
  - Reply: OrderReply (sealed class) or concrete results with success/failure message and snapshot of current state
- Behavior:
  - CreateOrder from New -> persist OrderCreated, thenTransition to Created
  - AddItem allowed in Created and Approved (if not shipped/cancelled)
  - Approve allowed in Created -> persist OrderApproved, thenTransition to Approved
  - Ship allowed in Approved -> persist OrderShipped, thenTransition to Shipped
  - Cancel allowed in Created or Approved -> persist OrderCancelled, thenTransition to Cancelled
  - Illegal commands should produce a failure reply without changing state (Effect.none().thenReply with error)
  - Demonstrate at least one thenRun side-effect (e.g., logging or invoking a fake external system)
- Provide a simple Main.kt to:
  - Start engine, create proxy for an order id
  - Run a happy-path sequence and print final state and events

6) Tests
- OrderWorkflowTest:
  - Test happy path: Create -> AddItem -> Approve -> Ship, verify state transitions and events
  - Test invalid transitions (e.g., Ship before Approve), verify no state change and error reply
  - Test cancel path and ensure no further operations allowed after cancellation
- EffectDslTest:
  - Verify chaining semantics for Effects.none and Effects.persist with thenRun/thenTransition/thenReply
  - Verify that Transition modifies state after event application
- EngineConcurrencyTest:
  - Concurrently issue multiple commands to the same instance (e.g., add items concurrently); verify serialized updates and final state correctness
- Ensure tests run with JUnit 5 and pass

Coding Standards and Documentation
- Provide KDoc for public classes/interfaces and key methods
- Small, cohesive classes; idiomatic Kotlin
- No TODOs or placeholders; code must compile and run
- Clear separation between core (DSL, interfaces) and runtime (engine/proxy) and sample domain

Build and Run Requirements
- Gradle tasks:
  - ./gradlew clean build
  - ./gradlew test
  - ./gradlew run (runs Main.kt)
- Ensure no external services required

Exact Interfaces and Class Responsibilities (guidance; final structure must be consistent and compile)
- com.example.workflow.core.Workflow<S, C, E, R>
- com.example.workflow.core.WorkflowContext<S, C, E, R> (provides id, effects builder)
- com.example.workflow.core.Effect<S, E, R> (sealed), with concrete internal implementations
- com.example.workflow.core.StepEffect<S, E, R> (builder interface)
- com.example.workflow.core.Transition<S>
- com.example.workflow.core.Effects (object with factory methods)
- com.example.workflow.runtime.InMemoryEventStore<S, E>, InMemoryStateStore<S>
- com.example.workflow.runtime.WorkflowEngine<S, C, E, R>
- com.example.workflow.runtime.WorkflowProxy<S, C, E, R>
- com.example.workflow.sample.OrderWorkflow (implements Workflow)
- com.example.workflow.sample.Main (demo)

Acceptance Criteria
- The project builds successfully with JDK 17 and Kotlin 1.9+ using Gradle Kotlin DSL
- All unit tests pass
- The Main program runs and demonstrates a working sequence with printed output
- The Effects DSL supports:
  - Effects.none().thenRun { ... }.thenReply { ... }
  - Effects.persist(event1, event2).thenTransition { ... }.thenReply { ... }
  - Effects.persist().thenNoReply()
- The in-memory proxy correctly serializes command handling and updates state/event log
- The code is fully self-contained and does not depend on Akka or other external frameworks

Instructions to Codex
- Generate the full project with all files listed above.
- Do not omit implementations; provide complete, compilable Kotlin code.
- Include imports, package declarations, Gradle build files, and test classes.
- Keep APIs cohesive and minimal; ensure generic typing across S, C, E, R where specified.
- Ensure side-effects and transition semantics execute in the correct order: persist/apply events -> transition -> run side-effects -> reply.
- Provide descriptive error messages for invalid commands or misuse.
- Add comments where semantics might be non-obvious.

Notes on Semantics (to reduce ambiguity)
- Transition is applied after events are persisted and applied; it transforms the resulting state to a new state (functional mapping).
- StepEffect represents a mid-construction step; only when thenReply or thenNoReply is called does it become an executable Effect.
- If no reply is desired for a command, use tell(), and the workflow should finalize with thenNoReply; ask() assumes a reply and should fail if none is produced.
- The engine must isolate each workflow instance by id; commands to the same id are processed in order.

Deliver the code inline, ready to copy into a project tree, with no placeholders.
