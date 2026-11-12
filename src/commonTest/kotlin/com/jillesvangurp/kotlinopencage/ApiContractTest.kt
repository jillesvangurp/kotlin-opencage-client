package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.Geometry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ApiContractTest {
    private val emptyResponse = """{"results":[],"total_results":0,"status":{"code":200,"message":"OK"}}"""

    @Test
    fun geocodingParametersAndCoordinateOrder() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/geocode/v1/json", request.url.encodedPath)
            assertEquals(mapOf(
                "key" to listOf("test-key"), "q" to listOf("München & Straße + 1"),
                "countrycode" to listOf("de,at"), "language" to listOf("native"),
                "limit" to listOf("100"),
                "abbrv" to listOf("1"), "add_request" to listOf("1"),
                "address_only" to listOf("1"), "no_annotations" to listOf("1"),
                "no_dedupe" to listOf("1"), "no_record" to listOf("1"),
                "roadinfo" to listOf("1"), "pretty" to listOf("1"),
                "future_option" to listOf("enabled"),
            ), request.url.parameters.entries().filter { it.key !in setOf("bounds", "proximity") }
                .associate { it.key to it.value })
            assertEquals(listOf(-1.0, 50.0, 2.0, 53.0), request.url.parameters["bounds"]!!.split(",").map(String::toDouble))
            assertEquals(listOf(52.0, 13.0), request.url.parameters["proximity"]!!.split(",").map(String::toDouble))
            respond(emptyResponse, headers = headersOf("Content-Type", "application/json"))
        })
        try {
            val client = OpencageClient("test-key", baseUrl = "https://example.test/geocode/", httpClient = http)
            val response = client.geocode(
                "München & Straße + 1", bounds = doubleArrayOf(-1.0, 50.0, 2.0, 53.0),
                countryCode = "de,at", language = "native", limit = 100,
                proximity = Geometry.Point.of(lat = 52.0, lon = 13.0), abbrv = true,
                addressOnly = true, addRequest = true, noAnnotations = true,
                noDedupe = true, noRecord = true, roadinfo = true, pretty = true,
                additionalRequestParams = mapOf("future_option" to "enabled"),
            )
            assertEquals(emptyList(), response.results)
            assertEquals(0L, response.totalResults)
            assertEquals(Status(200, "OK"), response.status)
            assertNull(response.rate)
        } finally {
            http.close()
        }
    }

    @Test
    fun locationCodesArePassedThroughAndFalseFlagsAreOmitted() = runTest {
        for (query in listOf("32C-849-5CKC", "LOCODE:DEBRV")) {
            val http = HttpClient(MockEngine { request ->
                assertEquals(query, request.url.parameters["q"])
                assertEquals(setOf("key", "q"), request.url.parameters.names())
                respond(emptyResponse)
            })
            try {
                OpencageClient("test-key", httpClient = http).geocode(
                    query, pretty = false, roadinfo = false, noAnnotations = false,
                )
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun geosearchUsesItsOwnEndpointAndHeaderAuthentication() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals("/geosearch", request.url.encodedPath)
            assertEquals("oc_gs_test", request.headers["OpenCage-Geosearch-Key"])
            assertNull(request.url.parameters["key"])
            assertEquals(mapOf(
                "q" to listOf("Mün & +"), "countrycode" to listOf("de,at,ch"),
                "language" to listOf("it"), "limit" to listOf("10"),
                "proximity" to listOf("45.5,5.5"), "_type" to listOf("city,postcode"),
                "future_flag" to listOf("1"),
            ), request.url.parameters.entries().filter { it.key != "bounds" }.associate { it.key to it.value })
            assertEquals(listOf(5.0, 45.0, 6.0, 46.0), request.url.parameters["bounds"]!!.split(",").map(String::toDouble))
            // The official JS client's fixtures use strings for coordinates.
            respond("""{
                "results":[{
                    "formatted":"Grenoble, France", "name":"Grenoble", "_type":"city",
                    "geometry":{"lat":"45.18756","lng":"5.73578"},
                    "bounds":{"southwest":{"lat":"45.15414","lng":"5.67761"},
                              "northeast":{"lat":"45.21408","lng":"5.75312"}},
                    "future_result_field":true
                }],
                "total_results":1, "status":{"code":200,"message":"OK"},
                "licenses":[{"name":"attribution","url":"https://opencagedata.com/credits"}],
                "timestamp":{"created_unix":1643221686}, "future_field":"preserved"
            }""")
        })
        val client = GeosearchClient("oc_gs_test", httpClient = http)
        try {
            val response = client.search(
                "Mün & +", countryCode = "de,at,ch", language = "it", limit = 10,
                bounds = doubleArrayOf(5.0, 45.0, 6.0, 46.0),
                proximity = Geometry.Point.of(lat = 45.5, lon = 5.5), type = "city,postcode",
                additionalRequestParams = mapOf("future_flag" to true),
            )
            assertEquals(1L, response.totalResults)
            assertEquals(200, response.status.code)
            assertEquals(1643221686L, response.timestamp.createdUnix)
            assertEquals("attribution", response.licenses.single().name)
            assertEquals("preserved", response.raw.getString("future_field"))
            val result = response.results.single()
            assertEquals("Grenoble", result.name)
            assertEquals("city", result.type)
            assertContentEquals(doubleArrayOf(5.73578, 45.18756), result.geometry?.coordinates)
            assertContentEquals(doubleArrayOf(5.67761, 45.15414, 5.75312, 45.21408), result.bounds)
        } finally {
            client.close()
        }
    }

    @Test
    fun geosearchExclusionsEmptyResultsAndDefaults() = runTest {
        val http = HttpClient(MockEngine { request ->
            assertEquals(setOf("q", "_type"), request.url.parameters.names())
            assertEquals("!village", request.url.parameters["_type"])
            respond(emptyResponse)
        })
        try {
            val response = GeosearchClient("oc_gs_test", httpClient = http).search("Berlin", type = "!village")
            assertEquals(emptyList(), response.results)
        } finally {
            http.close()
        }
    }

    @Test
    fun geosearchValidatesLimitAndBoundsBeforeSending() = runTest {
        val http = HttpClient(MockEngine { error("Invalid request must not be sent") })
        try {
            val client = GeosearchClient("oc_gs_test", httpClient = http)
            for (limit in listOf(0, 11)) {
                assertFailsWith<IllegalArgumentException> { client.search("Berlin", limit = limit) }
            }
            assertFailsWith<IllegalArgumentException> { client.search("Berlin", bounds = doubleArrayOf(1.0, 2.0)) }
        } finally {
            http.close()
        }
    }

    @Test
    fun bothClientsKeepTypedErrorsEvenWithKtorExpectSuccessEnabled() = runTest {
        for (code in listOf(400, 401, 402, 403, 408, 429)) {
            val body = """{"status":{"code":$code,"message":"test error"}}"""
            val http = HttpClient(MockEngine { respond(body, HttpStatusCode.fromValue(code)) }) {
                expectSuccess = true
            }
            try {
                val geocodeError = assertFailsWith<OpenCageException> {
                    OpencageClient("test-key", httpClient = http).geocode("Berlin")
                }
                val searchError = assertFailsWith<OpenCageException> {
                    GeosearchClient("oc_gs_test", httpClient = http).search("Berlin")
                }
                for (error in listOf(geocodeError, searchError)) {
                    assertEquals(code, error.code)
                    assertEquals(body, error.response)
                    when (code) {
                        400 -> assertIs<BadRequestException>(error)
                        401 -> assertIs<KeyException>(error)
                        402 -> assertIs<QuotaException>(error)
                        403 -> assertIs<ForbiddenException>(error)
                        408 -> assertIs<TimeoutException>(error)
                        429 -> assertIs<TooManyRequestsException>(error)
                    }
                }
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun geosearchResultsMayOmitOptionalFieldsAndUseNumericCoordinates() {
        val response = GeosearchResponse(DEFAULT_JSON.decodeFromString<JsonObject>("""{
            "results":[{"formatted":"10115, Berlin", "_type":"postcode", "geometry":{"lat":52.53,"lng":13.38}}]
        }"""))
        val result = response.results.single()
        assertEquals("postcode", result.type)
        assertNull(result.name)
        assertNull(result.bounds)
        assertContentEquals(doubleArrayOf(13.38, 52.53), result.geometry?.coordinates)
    }
}
