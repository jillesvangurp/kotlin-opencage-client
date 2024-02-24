package com.jillesvangurp.kotlinopencage.readme

import com.jillesvangurp.kotlin4example.SourceRepository
import com.jillesvangurp.kotlinopencage.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
        +"""
            The main feature of [kotlin4example](https://github.com/jillesvangurp/kotlin4example) is of course integrating code samples into your documentation.   
        """.trimIndent()
        subSection("Create client") {
            +"""
                Get your api key from Opencage and provide it to the client. 
                
                Note. **never commit your key to a git repository and use e.g. a secret manager**.
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
                }
            }.let {
                +"""
                    As you can see from the output, all the important parts of the response are parsed
                    and exposed via nice data classes.
                    
                    This produces the following output.
                """.trimIndent()
                mdCodeBlock(it.stdOut, "text", wrap = true)
            }
        }
        subSection("Working with the raw response") {
            +"""
                The return type of `geocode` is actually a kotlinx.serialization `JsonObject`.
                
                All the important parts of that are exposed via extension properties.
                
                However, if Opencage adds new things to the response, you can always get to those
                by poking around in the JsonObject.
            """.trimIndent()

            val response = runBlocking {  client.geocode(
                "52.54125444670068, 13.390771722807354"
            )}
            example {
                val response = client.geocode(
                    "52.54125444670068, 13.390771722807354"
                )

                println("This is the same total of ${response.totalResults} ..")
                println(".. as the raw value in the json: ${response["total_results"]?.jsonPrimitive?.long}")
            }.let {
                mdCodeBlock(it.stdOut,"text")
            }

            +"""
                And of course you can print the full response as well
            """.trimIndent()
            example {
                println(DEFAULT_PRETTY_JSON.encodeToString(response))
            }.let {
                mdCodeBlock(it.stdOut,"json", allowLongLines = true)
            }
        }
    }
    includeMdFile("outro.md")
}

