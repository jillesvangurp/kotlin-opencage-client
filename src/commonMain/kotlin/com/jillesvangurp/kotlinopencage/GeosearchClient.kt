package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.BoundingBox
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * OpenCage's place autosuggest service. Requires a separate geosearch key (`oc_gs_...`),
 * configured for your domain; a geocoding API key cannot be used here.
 * See https://github.com/OpenCageData/geosearch for service details.
 * Debouncing and choosing when to start a search are the caller's responsibility.
 */
class GeosearchClient(
    private val apiKey: String,
    val baseUrl: String = "https://api.opencagedata.com/geosearch",
    private val httpClient: HttpClient = HttpClient(),
) {
    /**
     * Suggest places matching [q]. [limit] is 1–10 (the server defaults to 5).
     * [countryCode] accepts comma-separated country codes. [type] accepts comma-separated
     * types such as `city,postcode`, or exclusions such as `!village`.
     * [bounds] uses GeoJSON order: west, south, east, north. [proximity] biases nearby results.
     * The server currently supports de, en, es, fr, it, and pt for [language].
     */
    suspend fun search(
        q: String,
        bounds: BoundingBox? = null,
        countryCode: String? = null,
        language: String? = null,
        limit: Int? = null,
        proximity: Geometry.Point? = null,
        type: String? = null,
        additionalRequestParams: Map<String, Any> = emptyMap(),
    ): GeosearchResponse {
        require(limit == null || limit in 1..10) { "limit must be between 1 and 10" }
        require(bounds == null || bounds.size == 4) { "bounds must have four coordinates" }
        val response = httpClient.get(baseUrl) {
            expectSuccess = false
            header("OpenCage-Geosearch-Key", apiKey)
            parameter("q", q)
            bounds?.let { parameter("bounds", it.joinToString(",")) }
            countryCode?.let { parameter("countrycode", it) }
            language?.let { parameter("language", it) }
            limit?.let { parameter("limit", it) }
            type?.let { parameter("_type", it) }
            proximity?.let {
                val coordinates = requireNotNull(it.coordinates) { "proximity must have coordinates" }
                parameter("proximity", "${coordinates.latitude},${coordinates.longitude}")
            }
            additionalRequestParams.forEach { (key, value) ->
                parameter(key, if (value is Boolean) if (value) "1" else "0" else value.toString())
            }
        }
        return GeosearchResponse(response.openCageResponse())
    }

    /** Closes the HTTP client, including a client supplied by the caller. */
    fun close() = httpClient.close()
}

/** Keeps unknown response fields accessible without conflating suggestions with geocoding results. */
data class GeosearchResponse(val raw: JsonObject) {
    val results get() = raw.deserializeList<GeosearchResult>("results") ?: error("results missing")
    val totalResults get() = raw.totalResults
    val status get() = raw.status
    val licenses get() = raw.licenses
    val timestamp get() = raw.timestamp
}

@Serializable
data class GeosearchResult(
    val formatted: String,
    val name: String? = null,
    @SerialName("_type")
    val type: String? = null,
    @SerialName("geometry")
    private val _geometry: JsonObject? = null,
    @SerialName("bounds")
    private val _bounds: JsonObject? = null,
) {
    val geometry get() = _geometry.asPoint
    val bounds: BoundingBox? get() = _bounds?.let {
        val southwest = requireNotNull(it.getPoint("southwest")?.coordinates)
        val northeast = requireNotNull(it.getPoint("northeast")?.coordinates)
        doubleArrayOf(southwest.longitude, southwest.latitude, northeast.longitude, northeast.latitude)
    }
}
