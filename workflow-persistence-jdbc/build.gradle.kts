apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    api(project(":workflow-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:9.22.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
