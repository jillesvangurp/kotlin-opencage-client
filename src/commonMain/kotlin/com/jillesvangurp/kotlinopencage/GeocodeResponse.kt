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
    get() = this["results"]?.jsonArray?.map {
        DEFAULT_JSON.decodeFromJsonElement<GeocodeResult>(it)
    } ?: error("results missing")

val GeocodeResponse.licenses
    get() = this["licenses"]?.jsonArray?.map {
        DEFAULT_JSON.decodeFromJsonElement<License>(it)
    } ?: error("licenses missing")

val GeocodeResponse.timestamp
    get() = this["timestamp"]?.jsonObject?.let {
        DEFAULT_JSON.decodeFromJsonElement<Timestamp>(it)
    } ?: error("timestamp missing")

val GeocodeResponse.rate
    get() = this["rate"]?.jsonObject?.let {
        DEFAULT_JSON.decodeFromJsonElement<Rate>(it)
    } ?: error("rate missing")

val GeocodeResponse.totalResults get() = this["total_results"]?.jsonPrimitive?.long ?: error("total_results missing")

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

val JsonObject.asPoint get() = this["lng"]?.jsonPrimitive?.double?.let {longitude ->
    val latitude = this["lat"]?.jsonPrimitive?.double?: error("missing lat")
    Geometry.Point(doubleArrayOf(longitude,latitude))
}

val OpencageGeometry.asBounds: BoundingBox? get() {
    return  this["bounds"]?.jsonObject?.let { bounds ->
        val northEast = this["northeast"]?.jsonObject?.asPoint ?: error("missing northeast in bounds")
        val southWest = this["southwest"]?.jsonObject?.asPoint ?: error("missing northeast in bounds")

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

/*
         "annotations" : {
            "DMS" : {
               "lat" : "22\u00b0 40' 45.05736'' S",
               "lng" : "14\u00b0 31' 36.48576'' E"
            },
            "MGRS" : "33KVQ5139191916",
            "Maidenhead" : "JG77gh36fx",
            "Mercator" : {
               "x" : 1617116.157,
               "y" : -2576798.589
            },
            "OSM" : {
               "edit_url" : "https://www.openstreetmap.org/edit?node=4488973891#map=17/-22.67918/14.52680",
               "note_url" : "https://www.openstreetmap.org/note/new#map=17/-22.67918/14.52680&layers=N",
               "url" : "https://www.openstreetmap.org/?mlat=-22.67918&mlon=14.52680#map=17/-22.67918/14.52680"
            },
            "UN_M49" : {
               "regions" : {
                  "AFRICA" : "002",
                  "NA" : "516",
                  "SOUTHERN_AFRICA" : "018",
                  "SUB-SAHARAN_AFRICA" : "202",
                  "WORLD" : "001"
               },
               "statistical_groupings" : [
                  "LEDC"
               ]
            },
            "callingcode" : 264,
            "currency" : {
               "alternate_symbols" : [
                  "N$"
               ],
               "decimal_mark" : ".",
               "disambiguate_symbol" : "N$",
               "format" : "%n %u",
               "html_entity" : "$",
               "iso_code" : "NAD",
               "iso_numeric" : "516",
               "name" : "Namibian Dollar",
               "smallest_denomination" : 5,
               "subunit" : "Cent",
               "subunit_to_unit" : 100,
               "symbol" : "$",
               "symbol_first" : 0,
               "thousands_separator" : ","
            },
            "flag" : "\ud83c\uddf3\ud83c\udde6",
            "geohash" : "k7fqfx6h5jbq5tn8tnpn",
            "qibla" : 31.02,
            "roadinfo" : {
               "drive_on" : "left",
               "road" : "Woermann Street",
               "speed_in" : "km/h"
            },
            "sun" : {
               "rise" : {
                  "apparent" : 1706762580,
                  "astronomical" : 1706757720,
                  "civil" : 1706761140,
                  "nautical" : 1706759460
               },
               "set" : {
                  "apparent" : 1706809680,
                  "astronomical" : 1706814540,
                  "civil" : 1706811060,
                  "nautical" : 1706812800
               }
            },
            "timezone" : {
               "name" : "Africa/Windhoek",
               "now_in_dst" : 0,
               "offset_sec" : 7200,
               "offset_string" : "+0200",
               "short_name" : "CAT"
            },
            "what3words" : {
               "words" : "integrate.laughter.teller"
            }
         },

 */
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
