pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Lets Gradle auto-provision a JDK toolchain for compiling/testing when only an older JDK is
    // installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kiit-rpc-kotlin"

include(":kiit-rpc")

// sample-kotlin / sample-java are added once they have real content (see
// _prd/kiit-httprpc/260921-setup-plan.md, Step 7) rather than included empty now.
