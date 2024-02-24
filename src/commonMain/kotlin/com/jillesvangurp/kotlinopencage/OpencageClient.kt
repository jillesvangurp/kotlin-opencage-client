package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.BoundingBox
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*

class OpencageClient(
    private val apiKey: String,
    val baseUrl: String = "https://api.opencagedata.com/geocode",
    val logging: Boolean = false,
    private val httpClient: HttpClient = HttpClient(

    ) {
        engine {
            pipelining = true
        }
        install(ContentNegotiation) {
            json(DEFAULT_JSON)
        }
        if (logging) {
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
) {
    /**
     * Call the opencage geocode API with a query [q].
     *
     *  - [bounds] Used only for forward geocoding. This value will restrict the possible results to a defined bounding box.
     *  - [countryCode] Used only for forward geocoding. Restricts results to the specified country/territory or countries.
     *  - [language] An IETF format language code (such as es for Spanish or pt-BR for Brazilian Portuguese), or native in which case we will attempt to return the response in the local language(s).
     *
     *  Feature flags:
     *
     *  - [abbrv] When true, we attempt to abbreviate and shorten the formatted string we return.
     *  - [addressOnly] When true, we include only the address (exluding POI names) in the formatted string we return.
     *  - [addRequest] When true, the various request parameters are added to the response for ease of debugging.
     *  - [noAnnotations] When true, results will not contain annotations.
     *  - [noDedupe] When true, results will not be deduplicated.
     *  - [noRecord] When true, the query contents are not logged.
     *  - [roadinfo] When true, the behaviour of the geocoder is changed to attempt to match the nearest road (as opposed to address).
     *
     * Use [additionalRequestParams] to pass on any request parameters not supported by this client.
     * Refer to [Opencage API docs](https://opencagedata.com/api) for more information on supported parameters.
     *
     */
    suspend fun geocode(
        q: String,
        bounds: BoundingBox? = null,
        countryCode: String? = null,
        language: String? = null,
        limit: Int? = null,
        proximity: Geometry.Point? = null,
        abbrv: Boolean? = null,
        addressOnly: Boolean? = null,
        addRequest: Boolean? = null,
        noAnnotations: Boolean? = null,
        noDedupe: Boolean? = null,
        noRecord: Boolean? = null,
        roadinfo: Boolean? = null,
        additionalRequestParams: Map<String, Any> = mapOf()
    ): GeocodeResponse {
        val response = httpClient.get("$baseUrl/v1/json") {
            parameter("key", apiKey)
            parameter("q", q)
            listOf(
                abbrv to "abbrv",
                addRequest to "add_request",
                addressOnly to "address_only",
                noAnnotations to "no_annotations",
                noDedupe to "no_dedupe",
                noRecord to "no_record",
                roadinfo to "roadinfo",
            ).filter { (value,_) -> value==true }
                .forEach { (_,param) ->
                parameter(param,"1")
            }
            bounds?.let {bbox->
                if(bbox.size!=4) error("invalid bounding box for bounds")
                parameter("bounds",bbox.joinToString(","))
            }
            countryCode?.let {
                parameter("countrycode", it)
            }
            language?.let {
                parameter("language",it)
            }
            limit?.let { amount ->
                if(amount<1 || amount >100) error("limit should be positive and <= 100")
                parameter("limit", amount)
            }
            proximity?.let { point ->
                val coordinates = point.coordinates ?: error("invalid point for proximity")
                parameter("proximity","${coordinates.latitude},${coordinates.longitude}")
            }
            additionalRequestParams.forEach {(key,value)->
                if(value is Boolean && value) {
                    parameter(key,"1")
                } else {
                    parameter(key, value.toString())
                }
            }
        }
        if(response.status.value < 300) {
            // OKish
            return DEFAULT_JSON.decodeFromString<GeocodeResponse>(response.bodyAsText())
        } else {
            val responseBody = response.bodyAsText()
            when(response.status.value) {
                400 -> throw BadRequestException(responseBody)
                401 -> throw KeyException(responseBody)
                402 -> throw QuotaException(responseBody)
                403 -> throw ForbiddenException(responseBody)
                408 -> throw TimeoutException(responseBody)
                429 -> throw TooManyRequestsException(responseBody)
                404,405,410,426 -> error("client bug, server returned ${response.status}: $responseBody")
                // if this persists, maybe contact opencage support
                500,503 -> error("server error: ${response.status}: $responseBody")
                else -> error("Unexpected status: ${response.status}: $responseBody")
            }
        }
    }
}

sealed class OpenCageException(m: String,val code: Int, val response: String) : Exception(m)
class BadRequestException(response: String): OpenCageException("validation failed", 400,response)
class KeyException(response: String): OpenCageException("api key not accepted", 401,response)
class QuotaException(response: String): OpenCageException("api key not accepted", 402,response)
class ForbiddenException(response: String): OpenCageException("not authorized for API or ip address blocked", 403,response)
class TimeoutException(response: String): OpenCageException("request timed out", 408,response)
class TooManyRequestsException(response: String): OpenCageException("too many requests, slow down", 429,response)

