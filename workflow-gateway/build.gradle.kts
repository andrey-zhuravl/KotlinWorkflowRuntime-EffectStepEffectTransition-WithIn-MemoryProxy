plugins {
    application
}

apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    implementation(project(":workflow-core"))
    implementation(project(":workflow-transport-kafka"))
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
