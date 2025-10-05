package com.example.workflow.core

/**
 * Represents a functional transition that transforms workflow state after events are applied.
 */
data class Transition<S>(val toState: (S) -> S)
