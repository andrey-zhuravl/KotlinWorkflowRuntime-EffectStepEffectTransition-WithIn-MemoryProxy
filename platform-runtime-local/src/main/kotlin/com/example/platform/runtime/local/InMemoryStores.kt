package com.example.platform.runtime.local

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple in-memory storage for workflow state and events.
 */
public class InMemoryEventStore<S, E>(private val initialState: (String) -> S) {
    private val states = ConcurrentHashMap<String, S>()
    private val events = ConcurrentHashMap<String, MutableList<E>>()

    /** Returns the latest state for the given identifier, initializing it if necessary. */
    public fun state(id: String): S = states.computeIfAbsent(id, initialState)

    /** Updates the stored state for the given identifier. */
    public fun updateState(id: String, newState: S) {
        states[id] = newState
    }

    /** Appends the provided events for the given identifier. */
    public fun appendEvents(id: String, newEvents: List<E>) {
        if (newEvents.isEmpty()) return
        events.compute(id) { _, existing ->
            val list = existing ?: CopyOnWriteArrayList()
            list.addAll(newEvents)
            list
        }
    }

    /** Returns a snapshot of persisted events for the given identifier. */
    public fun events(id: String): List<E> = events[id]?.toList() ?: emptyList()
}
