package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class OpenCageTest {
    val client by lazy {
        OpencageClient(apiKey = openCageKey)
    }

    @Test
    fun `should geocoode`() {
        runBlocking {
            client.geocode(q="52.54125444670068, 13.390771722807354").let { resp->
                println(DEFAULT_PRETTY_JSON.encodeToString(resp))

                resp.totalResults shouldBeGreaterThan 0
                resp.results.size shouldBeGreaterThan 0
                resp.results.firstOrNull {
                    it.components?.road?.startsWith("Watt") == true
                }?.also {wattStr ->
                    wattStr.components?.city shouldBe "Berlin"
                    wattStr.geometry?.asPoint?.coordinates?.longitude!! shouldBeGreaterThan 13.0
                    wattStr.geometry?.asPoint?.coordinates?.longitude!! shouldBeLessThan 14.0
                    wattStr.geometry?.asPoint?.coordinates?.latitude!! shouldBeGreaterThan 52.0
                    wattStr.geometry?.asPoint?.coordinates?.latitude!! shouldBeLessThan 53.0
                } shouldNotBe null


            }
        }
    }

    @Test
    fun `should find nothing`() {
        runBlocking {
            client.geocode("NOWHERE-INTERESTING").totalResults shouldBe 0
        }
    }
}