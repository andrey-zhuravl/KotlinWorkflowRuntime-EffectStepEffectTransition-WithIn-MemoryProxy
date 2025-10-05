package com.example.workflow.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory store keeping the latest workflow state per instance id.
 */
class InMemoryStateStore<S>(private val initialStateProvider: () -> S) {
    private val states = ConcurrentHashMap<String, S>()

    fun getState(id: String): S = states.computeIfAbsent(id) { initialStateProvider() }

    fun updateState(id: String, state: S) {
        states[id] = state
    }
}

/**
 * Thread-safe in-memory append-only store for workflow events.
 */
class InMemoryEventStore<E> {
    private val events = ConcurrentHashMap<String, MutableList<E>>()

    fun append(id: String, newEvents: List<E>) {
        if (newEvents.isEmpty()) return
        events.compute(id) { _, existing ->
            (existing ?: mutableListOf<E>()).apply { addAll(newEvents) }
        }
    }

    fun events(id: String): List<E> = events[id]?.toList() ?: emptyList()
}
