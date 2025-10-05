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
    "workflow-integration-tests",
    "workflow-projections",
    "workflow-human",
    "workflow-subflow",
    "workflow-security",
    "workflow-admin",
    "workflow-devtools"
)
