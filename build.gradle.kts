import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

val nodeVersion = libs.versions.node.get()
plugins.withType<NodeJsPlugin> {
    the<NodeJsEnvSpec>().version = nodeVersion
}
plugins.withType<WasmNodeJsPlugin> {
    the<WasmNodeJsEnvSpec>().version = nodeVersion
}

repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        // optional but it speeds up the gradle dependency resolution
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.github.jillesvangurp")
            includeGroup("com.tryformation")
        }
    }
    maven(url = "https://jitpack.io") {
        content {
            includeGroup("com.github.jillesvangurp")
        }
    }
}

kotlin {
    jvm {
        // should work for android as well
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    js {
        nodejs {
            testTask {
                useMocha {
                    // javascript is a lot slower than Java, we hit the default timeout of 2000
                    timeout = "60s"
                }
            }
        }
    }
    linuxX64()
    linuxArm64()
    mingwX64()
    macosArm64()
    iosArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // Ktor/datetime use npm modules that the standalone D8 shell cannot resolve.
        browser()
        nodejs()
    }

    sourceSets {

        commonMain {
            dependencies {
                implementation(libs.kotlin.logging)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.geogeometry)
                implementation(libs.kotlinx.datetime)

            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test.common)
                implementation(libs.kotest.assertions.core)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain  {
            dependencies {
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit5)
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.engine)

                implementation(libs.ktor.client.java)
                implementation(libs.kotlin4example)

                implementation(libs.slf4j.jul)
                implementation(libs.logback.classic)
            }
        }

        jsMain  {
            dependencies {
            }
        }

        jsTest  {
            dependencies {
                implementation(libs.kotlin.test.common)
                implementation(libs.ktor.client.js)
            }
        }

        wasmJsTest {
            dependencies {
                implementation(libs.kotlin.test.common)
                implementation(libs.ktor.client.wasm.js)
            }
        }

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                languageVersion = "2.2"
                apiVersion = "2.2"
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                url.set("https://github.com/jillesvangurp/kotlin-opencage-client")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/jillesvangurp/kotlin-opencage-client/blob/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("jillesvangurp")
                        name.set("Jilles van Gurp")
                        email.set("jilles@no-reply.github.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/jillesvangurp/kotlin-opencage-client.git")
                    developerConnection.set("scm:git:ssh://github.com:jillesvangurp/kotlin-opencage-client.git")
                    url.set("https://github.com/jillesvangurp/kotlin-opencage-client")
                }
            }
        }
    }

    repositories {
        maven {
            // GOOGLE_APPLICATION_CREDENTIALS env var must be set for this to work
            // public repository is at https://maven.tryformation.com/releases
            url = uri("gcs://mvn-public-tryformation/releases")
            name = "FormationPublic"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // run tests in parallel
    systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
    // executes test classes concurrently
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    // executes tests inside a class concurrently
    systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
    systemProperties["junit.jupiter.execution.parallel.config.strategy"] = "dynamic"
    systemProperties["junit.jupiter.execution.parallel.config.executor-service"] = "WORKER_THREAD_POOL"
    // random order of test class execution
    systemProperties["junit.jupiter.testclass.order.default"] = "org.junit.jupiter.api.ClassOrderer\$Random"

    testLogging.exceptionFormat = TestExceptionFormat.FULL
    testLogging.events = setOf(
        TestLogEvent.FAILED,
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.STANDARD_ERROR,
        TestLogEvent.STANDARD_OUT
    )
    addTestListener(object : TestListener {
        val failures = mutableListOf<String>()
        override fun beforeSuite(desc: TestDescriptor) {
        }

        override fun afterSuite(desc: TestDescriptor, result: TestResult) {
        }

        override fun beforeTest(desc: TestDescriptor) {
        }

        override fun afterTest(desc: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                val report =
                    """
                    TESTFAILURE ${desc.className} - ${desc.name}
                    ${
                        result.exception?.let { e ->
                            """
                            ${e::class.simpleName} ${e.message}
                        """.trimIndent()
                        }
                    }
                    -----------------
                    """.trimIndent()
                failures.add(report)
            }
        }
    })
}
