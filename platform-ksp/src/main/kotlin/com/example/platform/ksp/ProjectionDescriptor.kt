package com.example.platform.ksp

import kotlin.reflect.KClass

data class ProjectionDescriptor(
    val name: String,
    val handlerClass: KClass<*>,
) : Descriptor
