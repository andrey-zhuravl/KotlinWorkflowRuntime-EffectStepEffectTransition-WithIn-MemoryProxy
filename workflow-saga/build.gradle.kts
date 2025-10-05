apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    api(project(":workflow-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
