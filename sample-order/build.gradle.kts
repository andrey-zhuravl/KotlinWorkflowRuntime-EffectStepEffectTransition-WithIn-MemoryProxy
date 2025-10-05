plugins {
    application
}

dependencies {
    implementation(project(":platform-core"))
    implementation(project(":platform-runtime"))
    implementation(project(":platform-persistence-jdbc"))
    implementation(project(":platform-transport-kafka"))
    implementation(project(":platform-timers"))
    implementation(project(":platform-human"))
    implementation(project(":platform-projections"))
    implementation(project(":platform-security"))
    implementation(project(":platform-devtools"))
}

application {
    mainClass.set("com.example.sample.order.OrderSampleAppKt")
}
