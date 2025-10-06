package com.example.platform.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkflowService(val value: String)
