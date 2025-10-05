package com.example.platform.ksp

import kotlin.reflect.KClass

sealed interface Descriptor

data class WorkflowDescriptor(
    val type: String,
    val workflowClass: KClass<*>,
    val stateClass: KClass<*>,
    val commandClass: KClass<*>,
    val eventClass: KClass<*>,
    val replyClass: KClass<*>,
) : Descriptor

data class ProjectionDescriptor(
    val name: String,
    val handlerClass: KClass<*>,
) : Descriptor

data class SagaDescriptor(
    val name: String,
    val handlerClass: KClass<*>,
) : Descriptor

object ServiceCatalog {
    private val workflows = mutableListOf<WorkflowDescriptor>()
    private val projections = mutableListOf<ProjectionDescriptor>()
    private val sagas = mutableListOf<SagaDescriptor>()

    fun registerWorkflow(descriptor: WorkflowDescriptor) {
        workflows += descriptor
    }

    fun registerProjection(descriptor: ProjectionDescriptor) {
        projections += descriptor
    }

    fun registerSaga(descriptor: SagaDescriptor) {
        sagas += descriptor
    }

    fun workflows(): List<WorkflowDescriptor> = workflows.toList()
    fun projections(): List<ProjectionDescriptor> = projections.toList()
    fun sagas(): List<SagaDescriptor> = sagas.toList()
}
