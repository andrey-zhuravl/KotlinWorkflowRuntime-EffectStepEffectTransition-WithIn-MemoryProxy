rootProject.name = "kotlin-workflow-platform"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    "platform-core",
    "platform-ksp",
    "platform-runtime",
    "platform-persistence-jdbc",
    "platform-transport-kafka",
    "platform-timers",
    "platform-saga",
    "platform-projections",
    "platform-human",
    "platform-security",
    "platform-admin",
    "platform-devtools",
    "sample-order"
)
