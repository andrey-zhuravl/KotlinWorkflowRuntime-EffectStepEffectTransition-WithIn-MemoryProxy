apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    api(project(":workflow-core"))
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
