pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://cache-redirector.jetbrains.com/maven-central") }
    }
}

rootProject.name = "workflow-platform"
include("platform-core", "platform-ksp", "platform-runtime-local", "sample-order")
