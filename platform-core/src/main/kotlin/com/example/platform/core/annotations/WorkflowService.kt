package com.example.platform.core.annotations

/** Marks a workflow implementation for service generation. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class WorkflowService(val name: String)
