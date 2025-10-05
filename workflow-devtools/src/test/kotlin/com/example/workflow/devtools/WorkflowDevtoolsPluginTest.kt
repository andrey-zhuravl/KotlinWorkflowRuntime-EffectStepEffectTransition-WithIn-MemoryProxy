package com.example.workflow.devtools

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class WorkflowDevtoolsPluginTest {
    @Test
    fun `plugin registers tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.example.workflow.devtools")

        assertNotNull(project.tasks.findByName("workflowInit"))
        assertNotNull(project.tasks.findByName("workflowGen"))
        assertNotNull(project.tasks.findByName("workflowScenario"))
    }
}
