# Kotlin Workflow Runtime — Effect/StepEffect/Transition Engine

This repository hosts a Kotlin workflow runtime that models Akka-style Effect/StepEffect/Transition semantics and includes an in-memory proxy/engine for executing workflows with event-sourced state evolution.

## Specifications

- [v2.1 Supplement — Reliability, DSL+, Performance, Ops, Security, DX](docs/spec/v2.1-supplement.md)

The v2.1 supplement layers on top of the existing v2 design to introduce exactly-once guarantees, multi-region failover, PITR/archival, schema upcasting, crypto audit chains, CQRS projections, human-in-the-loop workflows, sub-workflows, enhanced timers, guard DSLs, performance optimizations, operations tooling, security controls, and developer experience improvements.
