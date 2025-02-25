@file:Suppress("UNUSED_VARIABLE", "NAME_SHADOWING")

package com.jillesvangurp.kotlinopencage.readme

import com.jillesvangurp.geojson.geoJsonIOUrl
import com.jillesvangurp.kotlin4example.SourceRepository
import com.jillesvangurp.kotlinopencage.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import java.io.File

const val githubLink = "https://github.com/formation-res/pg-docstore"

val sourceGitRepository = SourceRepository(
    repoUrl = githubLink,
    sourcePaths = setOf("src/commonMain/kotlin", "src/commonTest/kotlin","src/jvmTest/kotlin")
)


class ReadmeGenerationTest {

    @Test
    fun `generate docs`() {
        File(".", "README.md").writeText(
            """
            # Kotlin Opencage Client

        """.trimIndent().trimMargin() + "\n\n" + readmeMd.value
        )
    }
}

val readmeMd = sourceGitRepository.md {
    includeMdFile("intro.md")
    val client = OpencageClient(apiKey = openCageKey)
    section("Examples") {
        subSection("Create client") {
            +"""
                Get your api key from Opencage and provide it to the client. 
                
                Note. **never commit your key to a git repository and use e.g. a secret manager**.
                
                Also, please read the [guidelines for protecting your keys](https://opencagedata.com/guides/how-to-protect-your-api-key)
            """.trimIndent()
            example(runExample = false) {
                val client = OpencageClient(
                    apiKey = "XXXXXX"
                )
            }
        }
        subSection("Calling the API") {
            +"""
                The only required parameter on geocode is the q parameter. All the other parameters are supported as well but default to null. 
                
                If Opencage adds new flags, you can use the `additionalRequestParams` map to add
                those as well.
            """.trimIndent()
            example {
                val response = client.geocode(
                    "52.54125444670068, 13.390771722807354"
                )

                println("Found ${response.totalResults}")
                response.results.first().let {best ->
                    println(best.formatted)
                    println(best.components)
                    println("confidence: ${best.confidence}")

                    // extract the point and construct a geojson.io link
                    println(best.geometry?.geoJsonIOUrl)
                }
            }.let {
                +"""
                    As you can see from the output, all the important parts of the response are parsed
                    and exposed via nice data classes.                  
                    
                    This produces the following output.
                """.trimIndent()
                mdCodeBlock(it.stdOut, "text", allowLongLines = true)
            }
            +"""
                The client depends on my [geogeometry](https://github.com/jillesvangurp/geogeometry) library,
                 which includes kotlinx.serialization compatible GeoJson model classes as well as 
                 some nice features to open geojson.io with a url that embeds the geojson.                
            """.trimIndent()

        }

        subSection("Working with the raw response") {
            +"""
                The return type of `geocode` is `GeocodeResponse` is a type alias for
                the kotlinx.serialization `JsonObject`.
                              
                All the important parts of that are exposed via extension properties.
                
                However, if Opencage adds new things to the response, you can always get to those things
                by poking around in the JsonObject directly.
                
                The reason this is implemented this way is forward compatibility.
            """.trimIndent()

            val response = runBlocking {  client.geocode(
                "52.54125444670068, 13.390771722807354"
            )}
            example {
                val response = client.geocode(
                    "52.54125444670068, 13.390771722807354"
                )
                // an extension property
                println("total: ${response.totalResults} ..")
                println(".. or get it from the json: " +
                        "${response.getLong("total_results")}")
            }.let {
                mdCodeBlock(it.stdOut,"text")
            }

            +"""
                Note we are using one of the provided convenience functions `getLong` for getting
                things out of JsonObjects. 
                
                And of course you can print the full response as well
            """.trimIndent()
            example {
                println(DEFAULT_PRETTY_JSON.encodeToString(response))
            }.let {
                mdCodeBlock(it.stdOut,"json", allowLongLines = true)
            }

            +"""
                This also works for annotations.
            """.trimIndent()
            example {
                val annotations = response.results.first().annotations
                println(annotations.getString("flag"))
                // you can recursively get stuff
                println(annotations.getString("what3words","words"))
                // Works for String, Double, Boolean, JsonArray, JsonObject ..
                println(annotations.getDouble("bounds","northeast","lat"))
            }.let {
                mdCodeBlock(it.stdOut,"json", allowLongLines = true)
            }
        }
        subSection("Customizing ktor client & selecting a client implementation") {
            +"""
                This project uses ktor-client for http connectivity. This makes it possible to
                use this library on all the different platforms that Kotlin has (jvm, native, js, wasm). 
                
                However, it does mean that you have to 
                pick a client implementation. This is as simple as selecting the right
                 dependency for your platform. Ktor provides quite a few client implementations.
                                 
                Refer to the [ktor documentation](https://ktor.io/docs/http-client-engines.html#minimal-version) 
                for a list of available clients. 
                
                For example, this readme is generated from a junit test running on the 
                jvm platform which means it needs a jvm client. There are four different implementations
                 that you can choose from. We picked the Java client (others are CIO, Apache, and Jetty).
                
                To use the Java client on the jvm, simply add this to your jvmMain dependencies in `build.gradle.kts` 
                
                ```kotlin
                    implementation("io.ktor:ktor-client-java:<ktor_version>")
                ```
                
                If you wish to tweak things further, the OpencageClient constructor has an httpClient parameter 
                with a sane default that you can override with a customized ktor `HttpClient`:
            """.trimIndent()

            example {
                val client = OpencageClient(
                    apiKey = "XXXXXX",
                    // configures an httpclient with trace logging installed
                    httpClient = HttpClient(
                    ) {
                        engine {
                            pipelining = true
                        }
                        install(ContentNegotiation) {
                            // we include sane defaults for kotlinx.serialization
                            json(DEFAULT_JSON)
                        }
                        install(Logging) {
                            level = LogLevel.ALL
                        }
                    }
                )
            }
        }
    }
    includeMdFile("outro.md")
}

