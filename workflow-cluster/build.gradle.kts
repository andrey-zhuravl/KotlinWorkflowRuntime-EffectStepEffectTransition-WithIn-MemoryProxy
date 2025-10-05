apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    api(project(":workflow-core"))
    implementation(project(":workflow-runtime-local"))
    implementation(project(":workflow-transport-kafka"))
    implementation(project(":workflow-persistence-jdbc"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
