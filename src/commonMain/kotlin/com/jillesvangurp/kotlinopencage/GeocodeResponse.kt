package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.*
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.math.max
import kotlin.math.min

typealias GeocodeResponse = JsonObject

val GeocodeResponse.results
    get() = this.deserializeList<GeocodeResult>("results") ?: error("results missing")

val GeocodeResponse.licenses
    get() = this.deserializeList<License>("licenses") ?: error("licenses missing")

val GeocodeResponse.timestamp
    get() = this.deserialize<Timestamp>("timestamp") ?: error("timestamp missing")

val GeocodeResponse.rate
    get() = this.deserialize<Rate>("rate") ?: error("rate missing")

val GeocodeResponse.totalResults get() = this.getLong("total_results") ?: error("total_results missing")

@Serializable
data class GeocodeResult(
    val confidence: Double,
    @SerialName("distance_from_q")
    val distanceFromQ: Distance,
    val formatted: String,
    val geometry: OpencageGeometry?,
    val components: Components?,
    val annotations: Annotations?,
)

@Serializable
data class Distance(val meters: Long)

/**
 * Either a point or bounds. use [asPoint] or [asBounds] to get these as a geojson point or bbox.
 */
typealias OpencageGeometry=JsonObject


val OpencageGeometry.asBounds: BoundingBox? get() {
    return  this["bounds"]?.jsonObject?.let { bounds ->
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
}

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
    val type: String?,
    val city: String?,
    val continent: String?,
    val country: String?,
    @SerialName("country_code")
    val countryCode: String?,
    @SerialName("house_number")
    val houseNumber: String?,
    val postcode: String?,
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
)

typealias Annotations = JsonObject

@Serializable
data class Timestamp(
    @SerialName("created_unix")
    val createdUnix: Long
)

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

val Rate.resetTime get() = resetEpoch?.let { Instant.fromEpochSeconds(it) }
