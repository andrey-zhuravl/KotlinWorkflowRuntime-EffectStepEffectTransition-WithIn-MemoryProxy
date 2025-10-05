plugins {
    application
}

apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    implementation(project(":workflow-runtime-local"))
    implementation(project(":workflow-saga"))
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("com.example.workflow.sample.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
