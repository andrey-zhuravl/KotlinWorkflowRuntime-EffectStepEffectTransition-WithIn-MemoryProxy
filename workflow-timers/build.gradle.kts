apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    api(project(":workflow-core"))
    implementation(project(":workflow-persistence-jdbc"))
    implementation(project(":workflow-transport-kafka"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
