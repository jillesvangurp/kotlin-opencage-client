package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

class OpenCageTest {
    val client by lazy {
        OpencageClient(
            apiKey = openCageKey,
            // makes it easy to dig out responses from logs for debugging
            logging = true
        )
    }

    @Test
    fun `should  geocode`() {
        runBlocking {
            val resp = client.geocode("wattstr 11", proximity = Geometry.Point.of(lat=52.54116,lon=13.39098))
            resp.size shouldBeGreaterThanOrEqual 1
            resp.results.first { it.components?.postcode == "13355"}.let { first ->
                first.components?.city shouldBe "Berlin"
                first.components?.suburb shouldBe "Gesundbrunnen"
            }
        }
    }

    @Test
    fun `should reverse geocode`() {
        runBlocking {
            client.geocode(q="52.54125444670068, 13.390771722807354").let { resp->
                println(DEFAULT_PRETTY_JSON.encodeToString(resp))

                resp.totalResults shouldBeGreaterThan 0
                resp.results.size shouldBeGreaterThan 0
                resp.results.firstOrNull {
                    it.components?.road?.startsWith("Watt") == true
                }?.also {wattStr ->
                    wattStr.components?.city shouldBe "Berlin"
                    wattStr.geometry?.coordinates?.longitude!! shouldBeGreaterThan 13.0
                    wattStr.geometry?.coordinates?.longitude!! shouldBeLessThan 14.0
                    wattStr.geometry?.coordinates?.latitude!! shouldBeGreaterThan 52.0
                    wattStr.geometry?.coordinates?.latitude!! shouldBeLessThan 53.0
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