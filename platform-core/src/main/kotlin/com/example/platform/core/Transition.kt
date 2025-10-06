package com.example.platform.core

/**
 * Describes a state transition to apply after processing events.
 */
public data class Transition<S>(val toState: (S) -> S)
