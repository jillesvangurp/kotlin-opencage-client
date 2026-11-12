package com.jillesvangurp.kotlinopencage

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResponseTest {
    @Test
    fun sparseDigipinResultAndUnknownFields() {
        val response = DEFAULT_JSON.decodeFromString<GeocodeResponse>("""{
            "results":[{
                "confidence":10, "formatted":"32C-849-5CKC, Agra, India",
                "components":{"_type":"digipin","_category":"postcode", "_normalized_city":"Agra",
                    "digipin":"32C-849-5CKC", "state_district":"Agra", "future_field":"new"},
                "annotations":{"DIGIPIN":"32C-849-5CKC", "future_annotation":{"value":42}}
            }],
            "status":{"code":200,"message":"OK"}, "total_results":1
        }""")
        val result = response.results.single()
        assertEquals("digipin", result.components?.type)
        assertEquals("postcode", result.components?.category)
        assertEquals("Agra", result.components?.normalizedCity)
        assertEquals("Agra", result.components?.stateDistrict)
        assertEquals("32C-849-5CKC", result.components?.digipin)
        assertEquals("32C-849-5CKC", result.annotations?.digipin)
        assertEquals(42L, result.annotations.getLong("future_annotation", "value"))
        assertNull(result.bounds)
        assertNull(result.geometry)
        assertNull(result.distanceFromQ)
        assertNull(response.rate)
        assertNull(response.request)
    }

    @Test
    fun addressComponentsAndOmittedAnnotations() {
        val result = DEFAULT_JSON.decodeFromString<GeocodeResult>("""{
            "confidence":9, "formatted":"Example address",
            "components":{"_type":"road","borough":"Mitte", "town":"Town", "village":"Village",
                "municipality":"Municipality", "county":"County", "county_code":"C",
                "city_district":"District", "road_type":"residential"}
        }""")
        val components = result.components!!
        assertEquals("road", components.type)
        assertEquals("Mitte", components.borough)
        @Suppress("DEPRECATION")
        assertEquals(components.borough, components.borrow)
        assertEquals("Town", components.town)
        assertEquals("Village", components.village)
        assertEquals("Municipality", components.municipality)
        assertEquals("County", components.county)
        assertEquals("C", components.countyCode)
        assertEquals("District", components.cityDistrict)
        assertEquals("residential", components.roadType)
        assertNull(result.annotations)
    }

    @Test
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun quotaRequestAndTimestampMetadata() {
        val response = DEFAULT_JSON.decodeFromString<GeocodeResponse>("""{
            "rate":{"limit":2500,"remaining":2498,"reset":1605312000},
            "timestamp":{"created_unix":1605300000},
            "request":{"key":"abcdef**************************","query":"Berlin"}
        }""")
        assertEquals(2500, response.rate?.limit)
        assertEquals(2498, response.rate?.remaining)
        assertEquals(1605312000L, response.rate?.resetTime?.epochSeconds)
        assertEquals(1605300000L, response.timestamp.instant.epochSeconds)
        assertEquals("abcdef**************************", response.request.getString("key"))
    }

    @Test
    fun extendedRoadInfoPreservesNumericAndTextValues() {
        val annotations = DEFAULT_JSON.decodeFromString<Annotations>("""{
            "roadinfo":{
                "drive_on":"right", "speed_in":"km/h", "road":"Example road", "road_type":"motorway",
                "road_reference":"A2", "road_reference_intl":"E 30", "lanes":2,
                "maxspeed":140, "maxspeed:bus":80, "maxspeed:hgv":"none",
                "maxspeed:conditional":"110 @ wet", "maxspeed:hgv:conditional":"90 @ (weight>3.5)",
                "maxspeed:variable":"weather", "minspeed":30, "maxheight":"below default",
                "maxweight":3.5, "maxwidth":"7.5", "width":7.5,
                "oneway":"yes", "surface":"asphalt", "toll":"yes",
                "toll_details":{"included":["bus","hgv"],"excluded":["bicycle"],"operator":"Operator"},
                "tunnel":"building_passage", "tunnel:name":"Example tunnel", "future_field":"retained"
            }
        }""")
        val info = annotations.roadInfo!!
        assertEquals("140", info.maxSpeed)
        assertEquals("80", info.maxSpeedBus)
        assertEquals("none", info.maxSpeedHgv)
        assertEquals("110 @ wet", info.maxSpeedConditional)
        assertEquals("90 @ (weight>3.5)", info.maxSpeedHgvConditional)
        assertEquals("weather", info.maxSpeedVariable)
        assertEquals("30", info.minSpeed)
        assertEquals("below default", info.maxHeight)
        assertEquals("3.5", info.maxWeight)
        assertEquals("7.5", info.maxWidth)
        assertEquals("7.5", info.width)
        assertEquals(2L, info.lanes)
        assertEquals("right", info.driveOn)
        assertEquals("km/h", info.speedIn)
        assertEquals("Example road", info.road)
        assertEquals("motorway", info.roadType)
        assertEquals("A2", info.roadReference)
        assertEquals("E 30", info.roadReferenceIntl)
        assertEquals("yes", info.oneWay)
        assertEquals("asphalt", info.surface)
        assertEquals("yes", info.toll)
        assertEquals(listOf("bus", "hgv"), info.tollDetails?.included)
        assertEquals(listOf("bicycle"), info.tollDetails?.excluded)
        assertEquals("Operator", info.tollDetails?.operator)
        assertEquals("building_passage", info.tunnel)
        assertEquals("Example tunnel", info.tunnelName)
        assertEquals("retained", info.raw.getString("future_field"))
    }

    @Test
    fun optionalAnnotationsAndPartialNutsCoverage() {
        val empty = JsonObject(emptyMap())
        assertNull(empty.digipin)
        assertNull(empty.roadInfo)
        assertNull(empty.nuts)
        assertNull(RoadInfo(empty).maxSpeed)
        assertNull(RoadInfo(empty).tollDetails)
        val annotations = DEFAULT_JSON.decodeFromString<Annotations>("""{
            "NUTS":{"NUTS0":{"code":"ES"},"NUTS1":{"code":"ES5"},"NUTS2":{"code":"ES51"}}
        }""")
        assertEquals("ES", annotations.nuts?.nuts0)
        assertEquals("ES5", annotations.nuts?.nuts1)
        assertEquals("ES51", annotations.nuts?.nuts2)
        assertNull(annotations.nuts?.nuts3)
    }
}
