import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
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
    }
    js(IR) {
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
    macosX64()
    macosArm64()
    iosArm64()
    iosX64()
    // blocked on ktor-client wasm support
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        browser()
//        nodejs()
//        d8()
//    }

    sourceSets {

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("io.github.microutils:kotlin-logging:_")
                implementation("io.ktor:ktor-client-logging:_")
                implementation("io.ktor:ktor-serialization-kotlinx:_")
                implementation("io.ktor:ktor-serialization-kotlinx-json:_")
                implementation("io.ktor:ktor-client-content-negotiation:_")
                implementation("com.github.jillesvangurp:geogeometry:_")
                implementation(KotlinX.datetime)

            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.kotest:kotest-assertions-core:_")
            }
        }

        jvmMain  {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))

            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit5", "_"))
                implementation(Testing.junit.jupiter.api)
                implementation(Testing.junit.jupiter.engine)

                implementation(Ktor.client.java)
                implementation("com.github.jillesvangurp:kotlin4example:_")

                implementation("org.slf4j:jul-to-slf4j:_")
                implementation("ch.qos.logback:logback-classic:_")
            }
        }

        jsMain  {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        jsTest  {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("io.ktor:ktor-client-js:_")
            }
        }

//        wasmJsTest {
//            dependencies {
//                implementation(kotlin("test-wasm-js"))
//                implementation("io.ktor:ktor-client-wasm:_")
//            }
//        }

        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    jvmTargetValidationMode.set(JvmTargetValidationMode.WARNING)

    kotlinOptions {
        // this is the minimum LTS version we support, 8 is no longer supported
        jvmTarget = "11"
        languageVersion = "2.0"
    }
}

publishing {
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



