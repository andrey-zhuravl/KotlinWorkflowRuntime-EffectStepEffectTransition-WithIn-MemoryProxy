package com.example.workflow.devtools

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class WorkflowDevtoolsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val initTask = target.tasks.register("workflowInit", ScaffoldTask::class.java) {
            description = "Scaffold a new workflow type"
        }
        target.tasks.register("workflowGen", GenerateCodeTask::class.java) {
            description = "Generate workflow handlers from a schema"
            dependsOn(initTask)
        }
        target.tasks.register("workflowScenario", ScenarioTask::class.java) {
            description = "Run workflow scenario from YAML definition"
        }
    }
}

abstract class ScaffoldTask : org.gradle.api.DefaultTask() {
    init {
        group = "workflow"
    }

    @org.gradle.api.tasks.TaskAction
    fun scaffold() {
        project.logger.lifecycle("Scaffolding workflow named ${'$'}{project.findProperty("workflowName") ?: "Sample"}")
    }
}

abstract class GenerateCodeTask : org.gradle.api.DefaultTask() {
    init {
        group = "workflow"
    }

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        project.logger.lifecycle("Generating workflow code for type ${'$'}{project.findProperty("workflowType") ?: "Unknown"}")
    }
}

abstract class ScenarioTask : org.gradle.api.DefaultTask() {
    init {
        group = "verification"
    }

    @org.gradle.api.tasks.TaskAction
    fun runScenario() {
        project.logger.lifecycle("Running workflow scenario ${'$'}{project.findProperty("workflowScenarioFile") ?: "scenario.yaml"}")
    }
}
