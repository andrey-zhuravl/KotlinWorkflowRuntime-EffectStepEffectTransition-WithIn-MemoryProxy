package com.example.platform.ksp

import kotlin.reflect.KClass

data class SagaDescriptor(
    val name: String,
    val handlerClass: KClass<*>,
) : Descriptor
