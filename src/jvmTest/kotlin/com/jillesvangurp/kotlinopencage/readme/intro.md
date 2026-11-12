[![Process Pull Request](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml)

Kotlin multiplatform client for the [OpenCage](https://opencagedata.com/) geocoding and geosearch APIs.

## Features

- Geocode and reverse geocode using the Opencage API
- Search for place and postcode suggestions using the geosearch service.
- Kotlin friendly API and data classes that model the response.
- Access unknown response fields through raw `JsonObject` values and pass new parameters through `additionalRequestParams`.
- Built with Ktor and kotlinx.serialization for JVM, JavaScript, WebAssembly, and native targets.

## Gradle

This library is published to our own maven repository.

```kotlin
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
}
```

And then you can add the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:kotlin-opencage-client:2.0.0")
```
