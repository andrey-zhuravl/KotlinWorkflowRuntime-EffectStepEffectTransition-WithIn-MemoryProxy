plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("workflowDevtools") {
            id = "com.example.workflow.devtools"
            implementationClass = "com.example.workflow.devtools.WorkflowDevtoolsPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
