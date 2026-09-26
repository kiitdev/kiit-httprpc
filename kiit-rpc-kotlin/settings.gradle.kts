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

// Composite builds: kiit-call and kiit-inputs aren't published to Maven Central yet, so build
// them from the local checkout instead. Explicit substitution rather than relying on
// group/version matching, since the included subprojects don't set `group`/`version` as real
// Gradle project properties (only inside their mavenPublishing { coordinates(...) } blocks).
// Remove each block once the corresponding module is actually published.
includeBuild("../../kiit-call/kiit-call-kotlin") {
    dependencySubstitution {
        substitute(module("dev.kiit:kiit-call")).using(project(":kiit-call"))
    }
}

includeBuild("../../kiit-inputs/kiit-inputs-kotlin") {
    dependencySubstitution {
        substitute(module("dev.kiit:kiit-inputs")).using(project(":kiit-inputs"))
    }
}

includeBuild("../../kiit-requests/kiit-requests-kotlin") {
    dependencySubstitution {
        substitute(module("dev.kiit:kiit-requests")).using(project(":kiit-requests"))
    }
}

include(":kiit-rpc")
include(":sample-kotlin")

// sample-kotlin stays in the shared ./samples/ folder alongside sample-java/sample-swift, one
// level up from this settings file, matching kiit-codes' layout. sample-java/sample-swift are
// still empty placeholders (see _prd/kiit-httprpc/260921-setup-plan.md, Step 7), not included yet.
project(":sample-kotlin").projectDir = file("../samples/sample-kotlin")
