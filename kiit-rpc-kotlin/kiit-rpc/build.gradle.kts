plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.skie)
    id("signing")
}

// Single source of truth for the published version — mirrors kiit-codes/kiit-result. Left as a
// placeholder: the starting version and first publish target (GitHub Packages pre-release vs.
// Maven Central stable) haven't been decided yet.
val libraryVersion = "0.0.0"

kotlin {
    jvm {
        compilerOptions {
            // JVM 21 so Kotlin emits PermittedSubclasses for the sealed Auth/Body hierarchies,
            // enabling exhaustive Java pattern-matching `switch`, same as kiit-codes/kiit-result.
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework {
            baseName = "KiitHttprpc"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            // Hardcoded coordinates rather than version-catalog entries, matching kiit-result's
            // own dependency on kiit-codes — these are external kiit libraries, not part of this
            // repo's own version catalog.
            implementation("dev.kiit:kiit-codes:1.1.0")
            implementation("dev.kiit:kiit-result:1.0.2")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

// Disabled: SKIE's default analytics upload sends git/hardware/project data to Touchlab — off
// until that's something we explicitly want, not because it's a default worth silently keeping.
skie {
    analytics {
        enabled.set(false)
    }
}

android {
    namespace = "kiit.httprpc"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

/**
 * Store the following in ~/.gradle/gradle.properties
 *
 * signingInMemoryKeyPassword=
 * signingInMemoryKey=
 * signing.gnupg.keyName=
 * signing.gnupg.passphrase=
 *
 * Maven local: ~/.m2/repository/dev/kiit/kiit-httprpc/
 */
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    coordinates(
        groupId = "dev.kiit",
        artifactId = "kiit-httprpc",
        version = libraryVersion,
    )
    pom {
        name = "kiit-httprpc"
        description = "Simple, declarative RPC-style HTTP client for Kotlin Multiplatform"
        url = "https://kiit.dev"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "codehelix"
                name = "CodeHelix"
                url = "https://kiit.dev"
            }
        }
        scm {
            url = "https://github.com/kiitdev/kiit-httprpc"
            connection = "scm:git:git://github.com/kiitdev/kiit-httprpc.git"
            developerConnection = "scm:git:ssh://git@github.com/kiitdev/kiit-httprpc.git"
        }
    }
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    source.setFrom(
        "src/commonMain/kotlin",
        "src/iosMain/kotlin",
    )
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

// Read by the release workflow (once one exists) to derive the git tag/GitHub release name from
// the same version published to Maven Central, same convention as kiit-codes/kiit-result.
tasks.register("printVersion") {
    doLast { println(libraryVersion) }
}
