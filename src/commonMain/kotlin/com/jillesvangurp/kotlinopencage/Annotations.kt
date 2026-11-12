package com.jillesvangurp.kotlinopencage

import kotlinx.serialization.json.JsonObject

/** India's DIGIPIN location identifier, when available for the result coordinates. */
val Annotations.digipin: String? get() = getString("DIGIPIN")

val Annotations.roadInfo: RoadInfo? get() = getObject("roadinfo")?.let(::RoadInfo)

/** NUTS regions reflect the server's dataset and are not available everywhere. */
val Annotations.nuts: Nuts? get() = getObject("NUTS")?.let(::Nuts)

data class Nuts(val raw: JsonObject) {
    val nuts0: String? get() = raw.getString("NUTS0", "code")
    val nuts1: String? get() = raw.getString("NUTS1", "code")
    val nuts2: String? get() = raw.getString("NUTS2", "code")
    val nuts3: String? get() = raw.getString("NUTS3", "code")
}

/**
 * Road annotations, including the speed-limit and tunnel fields added in June 2026.
 * Values can be missing. Speeds and dimensions use strings to preserve both JSON numbers
 * and textual values such as `none`, `default`, and conditional expressions.
 * [speedIn] gives speed units; dimensions are in metres and weights in metric tons.
 * This crowdsourced information may be incomplete or outdated.
 * Unknown fields remain available through [raw].
 */
data class RoadInfo(val raw: JsonObject) {
    val driveOn: String? get() = raw.getString("drive_on")
    val speedIn: String? get() = raw.getString("speed_in")
    val road: String? get() = raw.getString("road")
    val roadType: String? get() = raw.getString("road_type")
    val roadReference: String? get() = raw.getString("road_reference")
    val roadReferenceIntl: String? get() = raw.getString("road_reference_intl")
    val lanes: Long? get() = raw.getLong("lanes")
    val maxHeight: String? get() = raw.getString("maxheight")
    val maxSpeed: String? get() = raw.getString("maxspeed")
    val maxSpeedBus: String? get() = raw.getString("maxspeed:bus")
    val maxSpeedHgv: String? get() = raw.getString("maxspeed:hgv")
    val maxSpeedConditional: String? get() = raw.getString("maxspeed:conditional")
    val maxSpeedHgvConditional: String? get() = raw.getString("maxspeed:hgv:conditional")
    val maxSpeedVariable: String? get() = raw.getString("maxspeed:variable")
    val maxWeight: String? get() = raw.getString("maxweight")
    val maxWidth: String? get() = raw.getString("maxwidth")
    val minSpeed: String? get() = raw.getString("minspeed")
    val oneWay: String? get() = raw.getString("oneway")
    val surface: String? get() = raw.getString("surface")
    val toll: String? get() = raw.getString("toll")
    val tollDetails: TollDetails? get() = raw.getObject("toll_details")?.let(::TollDetails)
    val tunnel: String? get() = raw.getString("tunnel")
    val tunnelName: String? get() = raw.getString("tunnel:name")
    val width: String? get() = raw.getString("width")
}

data class TollDetails(val raw: JsonObject) {
    val included: List<String>? get() = raw.getStringList("included")
    val excluded: List<String>? get() = raw.getStringList("excluded")
    val operator: String? get() = raw.getString("operator")
}
