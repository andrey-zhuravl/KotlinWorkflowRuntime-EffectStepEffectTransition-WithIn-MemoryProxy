package com.example.platform.core.annotations

/** Declares a sealed hierarchy containing workflow commands. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Command

/** Declares a sealed hierarchy containing workflow events. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Event

/** Declares a sealed hierarchy containing workflow replies. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Reply
