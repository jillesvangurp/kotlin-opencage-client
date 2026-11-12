package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.math.max
import kotlin.math.min
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

typealias GeocodeResponse = JsonObject

val GeocodeResponse.results
    get() = this.deserializeList<GeocodeResult>("results") ?: error("results missing")

val GeocodeResponse.licenses
    get() = this.deserializeList<License>("licenses") ?: error("licenses missing")

val GeocodeResponse.timestamp
    get() = this.deserialize<Timestamp>("timestamp") ?: error("timestamp missing")

/** Absent for subscription customers, who do not have a hard daily quota. */
val GeocodeResponse.rate
    get() = this.deserialize<Rate>("rate")

val GeocodeResponse.status
    get() = this.deserialize<Status>("status") ?: error("status missing")

/** Present when the request includes `add_request=1`. */
val GeocodeResponse.request get() = this.getObject("request")

val GeocodeResponse.totalResults get() = this.getLong("total_results") ?: error("total_results missing")

@Serializable
data class GeocodeResult(
    val confidence: Double,
    @SerialName("distance_from_q")
    val distanceFromQ: Distance?,
    val formatted: String,
    val components: Components?,
    val annotations: Annotations?,
    // keep it private so we can properly pick it apart
    @SerialName("geometry")
    private val _geometry: JsonObject?,
    @SerialName("bounds")
    private val _bounds: JsonObject?
) {
    val bounds: BoundingBox? get() = _bounds?.let { bounds ->
        val northEast = bounds.getPoint("northeast") ?: error("missing northeast in bounds")
        val southWest = bounds.getPoint("southwest") ?: error("missing southwest in bounds")

        // Geojson bbox convention of min long, min lat followed by max lon, max lat
        doubleArrayOf(
            min(northEast.coordinates!!.longitude, southWest.coordinates!!.longitude),
            min(northEast.coordinates!!.latitude, southWest.coordinates!!.latitude),
            max(northEast.coordinates!!.longitude, southWest.coordinates!!.longitude),
            max(northEast.coordinates!!.latitude, southWest.coordinates!!.latitude),
        )
    }

    // assumed to always be a point for now
    val geometry get() = _geometry.asPoint
}

@Serializable
data class Distance(val meters: Long)

@Serializable
data class Components(
    @SerialName("ISO_3166-1_alpha-2")
    val iso3166TwoLetterCode: String?,
    @SerialName("ISO_3166-1_alpha-3")
    val iso3166ThreeLetterCode: String?,
    @SerialName("ISO_3166-2")
    val countryWithSubdivision: List<String>?,
    @SerialName("_category")
    val category: String?,
    @SerialName("_normalized_city")
    val normalizedCity: String?,
    @SerialName("_type")
    val type: String?,
    val city: String?,
    val continent: String?,
    val country: String?,
    @SerialName("country_code")
    val countryCode: String?,
    @SerialName("house_number")
    val houseNumber: String?,
    val postcode: String?,
    @SerialName("borough")
    @Deprecated("Use borough instead", ReplaceWith("borough"))
    val borrow: String?,
    val neighbourhood: String?,
    @SerialName("political_union")
    val politicalUnion: String?,
    val restaurant: String?,
    val road: String?,
    val state: String?,
    @SerialName("state_code")
    val stateCode: String?,
    val suburb: String?,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    @SerialName("county_code")
    val countyCode: String? = null,
    @SerialName("state_district")
    val stateDistrict: String? = null,
    @SerialName("city_district")
    val cityDistrict: String? = null,
    @SerialName("road_type")
    val roadType: String? = null,
    val digipin: String? = null,
) {
    @Suppress("DEPRECATION")
    val borough: String? get() = borrow
}

typealias Annotations = JsonObject

@Serializable
data class Status(val code: Int, val message: String)

@Serializable
data class Timestamp(
    @SerialName("created_unix")
    val createdUnix: Long
)

@OptIn(ExperimentalTime::class)
val Timestamp.instant get() = Instant.fromEpochSeconds(createdUnix)

@Serializable
data class License(
    val name: String,
    val url: String,
)

@Serializable
data class Rate(
    val limit: Int?,
    val remaining: Int?,
    @SerialName("reset")
    val resetEpoch: Long?
)

@OptIn(ExperimentalTime::class)
val Rate.resetTime get() = resetEpoch?.let { Instant.fromEpochSeconds(it) }
