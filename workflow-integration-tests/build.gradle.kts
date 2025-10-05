apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    testImplementation(project(":workflow-runtime-local"))
    testImplementation(project(":workflow-persistence-jdbc"))
    testImplementation(project(":workflow-transport-kafka"))
    testImplementation(project(":workflow-timers"))
    testImplementation(project(":workflow-saga"))
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
