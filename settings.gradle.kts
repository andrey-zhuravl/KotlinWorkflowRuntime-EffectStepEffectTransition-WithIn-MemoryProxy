pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kotlin-workflow-runtime"

include(
    "workflow-core",
    "workflow-runtime-local",
    "workflow-persistence-jdbc",
    "workflow-transport-kafka",
    "workflow-cluster",
    "workflow-timers",
    "workflow-saga",
    "workflow-gateway",
    "workflow-sample",
    "workflow-integration-tests"
)
