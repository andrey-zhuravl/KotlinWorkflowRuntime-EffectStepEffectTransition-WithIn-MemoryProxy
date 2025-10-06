package com.example.platform.ksp

import kotlin.reflect.KClass

data class WorkflowDescriptor(
    val type: String,
    val workflowClass: KClass<*>,
    val stateClass: KClass<*>,
    val commandClass: KClass<*>,
    val eventClass: KClass<*>,
    val replyClass: KClass<*>,
) : Descriptor
