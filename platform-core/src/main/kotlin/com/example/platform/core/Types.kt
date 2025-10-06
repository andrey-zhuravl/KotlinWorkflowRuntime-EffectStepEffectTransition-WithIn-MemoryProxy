package com.example.platform.core

/** Alias for a suspending side-effect executed after state updates. */
public typealias SideEffect<S> = suspend (S) -> Unit

/** Alias for workflow reply functions producing [R] from state [S]. */
public typealias ReplyFunction<S, R> = (S) -> R
