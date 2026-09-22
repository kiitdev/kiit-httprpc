plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "sample.SampleKt"
}

dependencies {
    implementation(project(":kiit-rpc"))
    implementation(libs.kotlinx.serialization.json)
    // Needed for runBlocking — kiit-rpc itself doesn't need this on a consumer's classpath, but
    // any real main() calling a suspend function does, same as it would in a real app.
    implementation(libs.kotlinx.coroutines.core)
}
