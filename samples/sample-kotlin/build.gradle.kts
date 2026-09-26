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
    // Demonstrates supplying HttpRpc's own client with a Ktor plugin installed. OkHttp isn't a
    // kiit-rpc dependency of this sample module itself, added here the same way any consumer
    // building their own client would. HttpCache needs no separate artifact, it's part of
    // ktor-client-core (a transitive kiit-rpc dependency already).
    implementation(libs.ktor.client.okhttp)
}
