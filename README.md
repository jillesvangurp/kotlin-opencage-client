# Kotlin Opencage Client

[![Process Pull Request](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml)

Kotlin multiplatform client for the [OpenCage](https://opencagedata.com/) geocoding and geosearch APIs.

## Features

- Geocode and reverse geocode using the Opencage API
- Search for place and postcode suggestions using the geosearch service.
- Kotlin friendly API and data classes that model the response.
- Access unknown response fields through raw `JsonObject` values and pass new parameters through `additionalRequestParams`.
- Built with Ktor and kotlinx.serialization for JVM, JavaScript, WebAssembly, and native targets.

## Gradle

This library is published to our own maven repository.

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        // optional but it speeds up the gradle dependency resolution
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.github.jillesvangurp")
            includeGroup("com.tryformation")
        }
    }
}
```

And then you can add the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:kotlin-opencage-client:2.0.0")
```

## API coverage reviewed November 2026

- `GeosearchClient` supports place and postcode suggestions, bounds, country filters, language, proximity, and type inclusion/exclusion. It uses a separate geosearch key and endpoint. See the [official geosearch client documentation](https://github.com/OpenCageData/geosearch), [proximity update](https://blog.opencagedata.com/post/geosearch-improvements), and [type-filtering update](https://blog.opencagedata.com/post/geosearch-with-type-filtering).
- Geocoding accepts [DIGIPIN queries](https://blog.opencagedata.com/post/digipin-support) through `geocode(q = "32C-849-5CKC")`. Read the identifier from `components.digipin` or `annotations.digipin`.
- `annotations.roadInfo` exposes [the extended road fields](https://blog.opencagedata.com/post/more-roadinfo-maxspeed-data), including bus/HGV speed limits, conditional and variable limits, minimum speeds, and tunnel information. Speeds and dimensions are strings so numeric and textual values are preserved. Road data is crowdsourced and may be incomplete or outdated.
- `annotations.nuts` exposes NUTS region codes. [OpenCage's dataset update](https://blog.opencagedata.com/post/nuts-2024-data) requires no new request parameter.
- Geocoding now has a `pretty` option, typed response `status`, and access to the optional `request` metadata returned with `addRequest = true`.

The [current geocoding API](https://opencagedata.com/api) still uses `/geocode/v1/json`. UN/LOCODE queries such as `LOCODE:DEBRV` already work through `geocode`. Annotations and the complete response remain accessible as JSON for fields added in the future. OpenCage's spreadsheet-upload service is documented as a dashboard feature, so this client does not invent an upload endpoint.

### Compatibility notes

`response.rate` is now nullable: subscription responses do not contain rate limits. Use `response.rate?.remaining`. `components.type` now reads the API's `_type` field, and `components.borough` replaces the misspelled `borrow` property, which remains as a deprecated alias. Additional address fields include town, village, municipality, county, city/state district, and road type.

The new `pretty` argument is appended after `additionalRequestParams`, preserving existing positional source calls. Recompile applications against this release because public method and data-class signatures have changed.

## Examples

### Create client

Get your api key from Opencage and provide it to the client. 

Note. **never commit your key to a git repository and use e.g. a secret manager**.

Also, please read the [guidelines for protecting your keys](https://opencagedata.com/guides/how-to-protect-your-api-key)

```kotlin
val client = OpencageClient(
  apiKey = "XXXXXX"
)
```

### Calling the API

The only required parameter on geocode is the q parameter. All the other parameters are supported as well but default to null. 

If Opencage adds new flags, you can use the `additionalRequestParams` map to add
those as well.

```kotlin
val response = client.geocode(
  "52.54125444670068, 13.390771722807354"
)

println("Found ${response.totalResults}")
response.results.first().let {best ->
  println(best.formatted)
  println(best.components)
  println("confidence: ${best.confidence}")

  // extract the point and construct a geojson.io link
  println(best.geometry?.geoJsonIOUrl)
}
```

As you can see from the output, all the important parts of the response are parsed
and exposed via nice data classes.                  

This produces the following output.

```text
Found 1
Ahoy! Berlin, Wattstraße 11, 13355 Berlin, Germany
Components(iso3166TwoLetterCode=DE, iso3166ThreeLetterCode=DEU, countryWithSubdivision=[DE-BE], category=commerce, normalizedCity=Berlin, type=coworking_space, city=Berlin, continent=Europe, country=Germany, countryCode=de, houseNumber=11, postcode=13355, borrow=Mitte, neighbourhood=Brunnenviertel, politicalUnion=European Union, restaurant=null, road=Wattstraße, state=Berlin, stateCode=BE, suburb=Gesundbrunnen, town=null, village=null, municipality=null, county=null, countyCode=null, stateDistrict=null, cityDistrict=null, roadType=null, digipin=null)
confidence: 9.0
https://geojson.io/#data=data%3Aapplication%2Fjson%2C%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22coordinates%22%3A%5B13.3906028%2C52.5412201%5D%2C%22type%22%3A%22Point%22%7D%2C%22type%22%3A%22Feature%22%7D%5D%2C%22type%22%3A%22FeatureCollection%22%7D
```

The client depends on my [geogeometry](https://github.com/jillesvangurp/geogeometry) library,
 which includes kotlinx.serialization compatible GeoJson model classes as well as 
 some nice features to open geojson.io with a url that embeds the geojson.                

### Working with the raw response

The return type of `geocode` is `GeocodeResponse` is a type alias for
the kotlinx.serialization `JsonObject`.
              
All the important parts of that are exposed via extension properties.

However, if Opencage adds new things to the response, you can always get to those things
by poking around in the JsonObject directly.

The reason this is implemented this way is forward compatibility.

```kotlin
val response = client.geocode(
  "52.54125444670068, 13.390771722807354"
)
// an extension property
println("total: ${response.totalResults} ..")
println(".. or get it from the json: " +
    "${response.getLong("total_results")}")
```

```text
total: 1 ..
.. or get it from the json: 1
```

Note we are using one of the provided convenience functions `getLong` for getting
things out of JsonObjects. 

And of course you can print the full response as well

```kotlin
println(DEFAULT_PRETTY_JSON.encodeToString(response))
```

```json
{
  "documentation": "https://opencagedata.com/api",
  "licenses": [
    {
      "name": "see attribution guide",
      "url": "https://opencagedata.com/credits"
    }
  ],
  "results": [
    {
      "annotations": {
        "DMS": {
          "lat": "52° 32' 28.39236'' N",
          "lng": "13° 23' 26.17008'' E"
        },
        "MGRS": "33UUU9085522453",
        "Maidenhead": "JO62qm69uv",
        "Mercator": {
          "x": 1490635.085,
          "y": 6864642.052
        },
        "NUTS": {
          "NUTS0": {
            "code": "DE"
          },
          "NUTS1": {
            "code": "DE3"
          },
          "NUTS2": {
            "code": "DE30"
          },
          "NUTS3": {
            "code": "DE300"
          }
        },
        "OSM": {
          "edit_url": "https://www.openstreetmap.org/edit?node=5103564057#map=17/52.54122/13.39060",
          "note_url": "https://www.openstreetmap.org/note/new#map=17/52.54122/13.39060&layers=N",
          "url": "https://www.openstreetmap.org/?mlat=52.54122&mlon=13.39060#map=17/52.54122/13.39060"
        },
        "UN_M49": {
          "regions": {
            "DE": "276",
            "EUROPE": "150",
            "WESTERN_EUROPE": "155",
            "WORLD": "001"
          },
          "statistical_groupings": [
            "MEDC"
          ]
        },
        "callingcode": 49,
        "currency": {
          "alternate_symbols": [],
          "decimal_mark": ",",
          "html_entity": "€",
          "iso_code": "EUR",
          "iso_numeric": "978",
          "name": "Euro",
          "smallest_denomination": 1,
          "subunit": "Cent",
          "subunit_to_unit": 100,
          "symbol": "€",
          "symbol_first": 0,
          "thousands_separator": "."
        },
        "flag": "🇩🇪",
        "geohash": "u33dbsydz9m4hkd87b8c",
        "qibla": 136.68,
        "roadinfo": {
          "drive_on": "right",
          "road": "Wattstraße",
          "speed_in": "km/h"
        },
        "sun": {
          "rise": {
            "apparent": 1794464580,
            "astronomical": 1794457440,
            "civil": 1794462300,
            "nautical": 1794459840
          },
          "set": {
            "apparent": 1794496680,
            "astronomical": 1794503820,
            "civil": 1794498900,
            "nautical": 1794501420
          }
        },
        "timezone": {
          "name": "Europe/Berlin",
          "now_in_dst": 0,
          "offset_sec": 3600,
          "offset_string": "+0100",
          "short_name": "CET"
        },
        "what3words": {
          "words": "pressing.survivor.miss"
        }
      },
      "bounds": {
        "northeast": {
          "lat": 52.5412701,
          "lng": 13.3906528
        },
        "southwest": {
          "lat": 52.5411701,
          "lng": 13.3905528
        }
      },
      "components": {
        "ISO_3166-1_alpha-2": "DE",
        "ISO_3166-1_alpha-3": "DEU",
        "ISO_3166-2": [
          "DE-BE"
        ],
        "_category": "commerce",
        "_normalized_city": "Berlin",
        "_type": "coworking_space",
        "borough": "Mitte",
        "city": "Berlin",
        "continent": "Europe",
        "country": "Germany",
        "country_code": "de",
        "coworking_space": "Ahoy! Berlin",
        "house_number": "11",
        "neighbourhood": "Brunnenviertel",
        "political_union": "European Union",
        "postcode": "13355",
        "quarter": "Brunnenviertel",
        "road": "Wattstraße",
        "state": "Berlin",
        "state_code": "BE",
        "suburb": "Gesundbrunnen"
      },
      "confidence": 9,
      "distance_from_q": {
        "meters": 12
      },
      "formatted": "Ahoy! Berlin, Wattstraße 11, 13355 Berlin, Germany",
      "geometry": {
        "lat": 52.5412201,
        "lng": 13.3906028
      }
    }
  ],
  "status": {
    "code": 200,
    "message": "OK"
  },
  "stay_informed": {
    "blog": "https://blog.opencagedata.com",
    "mastodon": "https://en.osm.town/@opencage"
  },
  "thanks": "For using an OpenCage API",
  "timestamp": {
    "created_http": "Thu, 12 Nov 2026 14:17:58 GMT",
    "created_unix": 1794493078
  },
  "total_results": 1
}
```

This also works for annotations.

```kotlin
val annotations = response.results.first().annotations
println(annotations.getString("flag"))
// you can recursively get stuff
println(annotations.getString("what3words","words"))
// Works for String, Double, Boolean, JsonArray, JsonObject ..
println(annotations.getDouble("bounds","northeast","lat"))
```

```json
🇩🇪
pressing.survivor.miss
null
```

### DIGIPIN and road information

Pass a DIGIPIN directly as the query. Road annotations expose speed limits as strings because the API can return numbers or values such as `none`. All fields are optional.

```kotlin
val response = client.geocode("32C-849-5CKC")
println(response.results.first().components?.digipin)
println(response.results.first().annotations?.digipin)

val road = client.geocode("52.54125,13.39077", roadinfo = true)
  .results.first().annotations?.roadInfo
println(road?.maxSpeedHgv)
println(road?.maxSpeedHgvConditional)
println(road?.tunnelName)
```

### Geosearch autosuggest

Use a separate geosearch key configured for your domain. The client sends it in the `OpenCage-Geosearch-Key` header. Debounce searches in your UI; the official widget starts searching at three characters.

```kotlin
val searchClient = GeosearchClient(apiKey = "oc_gs_YOUR_KEY")
try {
  val suggestions = searchClient.search(
    q = "Berl",
    countryCode = "de,at,ch",
    language = "de",
    type = "city,postcode",
    limit = 5,
  )
  suggestions.results.forEach { println("${it.name}: ${it.formatted}") }
  // Use type = "!village" to exclude villages.
  // The full JSON is available as suggestions.raw.
} finally {
  searchClient.close()
}
```

### Customizing ktor client & selecting a client implementation

This project uses ktor-client for http connectivity. This makes it possible to
use this library on all the different platforms that Kotlin has (jvm, native, js, wasm). 

However, it does mean that you have to 
pick a client implementation. This is as simple as selecting the right
 dependency for your platform. Ktor provides quite a few client implementations.
                 
Refer to the [ktor documentation](https://ktor.io/docs/http-client-engines.html#minimal-version) 
for a list of available clients. 

For example, this readme is generated from a junit test running on the 
jvm platform which means it needs a jvm client. There are four different implementations
 that you can choose from. We picked the Java client (others are CIO, Apache, and Jetty).

To use the Java client on the jvm, simply add this to your jvmMain dependencies in `build.gradle.kts` 

```kotlin
    implementation("io.ktor:ktor-client-java:<ktor_version>")
```

If you wish to tweak things further, the OpencageClient constructor has an httpClient parameter 
with a sane default that you can override with a customized ktor `HttpClient`:

```kotlin
val client = OpencageClient(
  apiKey = "XXXXXX",
  // configures an httpclient with trace logging installed
  httpClient = HttpClient(
  ) {
    engine {
      pipelining = true
    }
    install(ContentNegotiation) {
      // we include sane defaults for kotlinx.serialization
      json(DEFAULT_JSON)
    }
    install(Logging) {
      level = LogLevel.ALL
    }
  }
)
```

## Multi platform

Configured targets are JVM (Java 21 bytecode), JavaScript, Wasm JS, Linux x64/ARM64, Windows x64, macOS ARM64, and iOS ARM64. Choose a Ktor HTTP engine for your platform. CI uses Java 25 LTS. JavaScript and Wasm Node tests use Node 26 LTS.

Wasm runs in browsers and Node.js. The standalone D8 test runner cannot resolve the npm modules used by the dependencies, such as `@js-joda/core`, so it is not configured.

## Development

The common API-contract tests use Ktor's mock engine and need no key or network access. Run them on JVM with `./gradlew jvmTest --tests '*ApiContractTest' --tests '*ResponseTest'`, or on JavaScript with `./gradlew jsNodeTest`.

The live geocoding tests and README generation need a valid OpenCage geocoding key. The special test key documented by OpenCage returns a fixed location and does not satisfy the live Berlin-address assertions.

To configure this either set the `OPENCAGE_KEY` environment variable, supply a `local.properties` file with the `opencageApiKey` property, or set the `opencageApiKey` system property. Any of those should work.

Dependency, Kotlin plugin, and Node.js versions are managed in `gradle/libs.versions.toml`. Gradle itself is pinned in `gradle/wrapper/gradle-wrapper.properties`.

Run `./gradlew build` to build. The project should load fine in recent versions of Intellij (community edition).

## README.md generated by kotlin4example

The README is generated from source files in [`src/jvmTest/kotlin/com/jillesvangurp/kotlinopencage/readme/ReadmeGenerationTest.kt`](src/jvmTest/kotlin/com/jillesvangurp/kotlinopencage/readme/ReadmeGenerationTest.kt) by 
[kotlin4example](https://github.com/jillesvangurp/kotlin4example). This is a library that I created
to make generating markdown with working code examples really easy. If you want to create pull requests against the documentation, just modify the source and rerun the tests to regenerate the readme.

