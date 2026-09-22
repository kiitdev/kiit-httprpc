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
}
