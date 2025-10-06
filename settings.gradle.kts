pluginManagement {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "workflow-platform"
include("platform-core", "platform-ksp", "platform-runtime-local", "sample-order")
