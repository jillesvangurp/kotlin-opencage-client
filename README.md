# JsonDsl

Kotlin multi-platform client for the Opencage geocoding API.

## Features

- Geocode and reverse geocode using the Opencage API
- Kotlin friendly API and data classes that model the response.
- Forward compatible with features that OpenCage adds; anyhting new and unsupported can be accessed via the raw response in `JsonObject` form. Any new unsupported request parameters may be added via an optional parameter that takes a map.
- Works on all Kotlin platforms. Built around ktor client and kotinx.serialization which work on all Kotlin platforms: jvm, mobile (android/ios), native (windows, linux, mac) and soon web assembly.

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
    implementation("com.jillesvangurp:kotlin-opencage-client:1.x.y")
```

## Example

The main feature of [kotlin4example](https://github.com/jillesvangurp/kotlin4example) is of course integrating code samples into your documentation.   

### Hello World

```kotlin

println("Hello World!")
```

And you can actually grab the output and show it in another code block:

```text
Hello World!
```

## Multi platform

This is a Kotlin multi platform library that should work on most  kotlin platforms (jvm, js, ios, android, etc). Wasm will be added later, after Kotlin 2.0 stabilizes.

