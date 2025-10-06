package com.example.platform.ksp

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
