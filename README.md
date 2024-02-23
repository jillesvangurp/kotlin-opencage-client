# JsonDsl

This is an opinionated template for creating kotlin multi platform library projects.

## Batteries included

- Gradle wrapper
- [Refresh versions plugin](https://splitties.github.io/refreshVersions/) - Great way to manage dependencies.
- [kotlin4example](https://github.com/jillesvangurp/kotlin4example) integrated to generate the readme and any other documentation you are going to write. This is all driven via the tests.
- Some dependencies for testing (junit, kotest-assertions, etc.)

## Gradle

This library is published to our own maven repository.

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        // optional but it speeds up the gradle dependency resolution
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.tryformation")
        }
    }
}
```

And then you can add the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:json-dsl:1.x.y")
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

