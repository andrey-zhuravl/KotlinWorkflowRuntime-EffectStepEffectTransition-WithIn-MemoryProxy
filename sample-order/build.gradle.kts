plugins {
    application
}

dependencies {
    implementation(project(":platform-core"))
}

application {
    mainClass.set("com.example.sample.order.OrderSampleAppKt")
}
