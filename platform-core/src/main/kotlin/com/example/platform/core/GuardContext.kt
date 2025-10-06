package com.example.platform.core

data class GuardContext<S, C>(
    val name: String,
    val state: S,
    val command: C,
) {
    fun snapshot(): S = state
}
