# Kotlin Opencage Client

[![Process Pull Request](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/kotlin-opencage-client/actions/workflows/pr_master.yaml)

Kotlin multi-platform client for the [Opencage](https://opencagedata.com/) geocoding API.

## Features

- Geocode and reverse geocode using the Opencage API
- Kotlin friendly API and data classes that model the response.
- Forward compatible with features that OpenCage adds; anyhting new and unsupported can be accessed via the raw response in `JsonObject` form. Any new unsupported request parameters may be added via an optional parameter that takes a map.
- Works on all Kotlin platforms. Built around ktor client and kotinx.serialization which work on all Kotlin platforms: jvm, mobile (android/ios), native (windows, linux, mac) and soon web assembly.

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
    implementation("com.jillesvangurp:kotlin-opencage-client:1.x.y")
```

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
  println(best.geometry?.asPoint?.geoJsonIOUrl)
}
```

As you can see from the output, all the important parts of the response are parsed
and exposed via nice data classes.                  

This produces the following output.

```text
Found 1
Wattstraße 11, 13355 Berlin, Germany
Components(iso3166TwoLetterCode=DE, iso3166ThreeLetterCode=DEU, countryWithSubdivision=[DE-BE], category=building, normalizedCity=Berlin, type=null, city=Berlin, continent=Europe, country=Germany, countryCode=de, houseNumber=11, postcode=13355, borrow=null, neighbourhood=Brunnenviertel, politicalUnion=European Union, restaurant=null, road=Wattstraße, state=Berlin, stateCode=BE, suburb=Gesundbrunnen)
confidence: 10.0
https://geojson.io/#data=data%3Aapplication%2Fjson%2C%7B%22features%22%3A%5B%7B%22geometry%22%3A%7B%22type%22%3A%22Point%22%2C%22coordinates%22%3A%5B13.3905952%2C52.5412642%5D%7D%2C%22type%22%3A%22Feature%22%7D%5D%2C%22type%22%3A%22FeatureCollection%22%7D
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
  "rate": {
    "limit": 2500,
    "remaining": 2473,
    "reset": 1708905600
  },
  "results": [
    {
      "annotations": {
        "DMS": {
          "lat": "52° 32' 28.55112'' N",
          "lng": "13° 23' 26.14272'' E"
        },
        "MGRS": "33UUU9085422458",
        "Maidenhead": "JO62qm69uv",
        "Mercator": {
          "x": 1490634.245,
          "y": 6864650.095
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
          "edit_url": "https://www.openstreetmap.org/edit?relation=1505873#map=16/52.54126/13.39060",
          "note_url": "https://www.openstreetmap.org/note/new#map=16/52.54126/13.39060&layers=N",
          "url": "https://www.openstreetmap.org/?mlat=52.54126&mlon=13.39060#map=16/52.54126/13.39060"
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
          "alternate_symbols": [
          ],
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
        "geohash": "u33dbsyep3x8bjhghnk0",
        "qibla": 136.68,
        "roadinfo": {
          "drive_on": "right",
          "road": "Wattstraße",
          "speed_in": "km/h"
        },
        "sun": {
          "rise": {
            "apparent": 1708841040,
            "astronomical": 1708834260,
            "civil": 1708839000,
            "nautical": 1708836600
          },
          "set": {
            "apparent": 1708878960,
            "astronomical": 1708885800,
            "civil": 1708881060,
            "nautical": 1708883400
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
          "words": "crunch.oven.arming"
        }
      },
      "bounds": {
        "northeast": {
          "lat": 52.5414917,
          "lng": 13.3911419
        },
        "southwest": {
          "lat": 52.5410011,
          "lng": 13.3903603
        }
      },
      "components": {
        "ISO_3166-1_alpha-2": "DE",
        "ISO_3166-1_alpha-3": "DEU",
        "ISO_3166-2": [
          "DE-BE"
        ],
        "_category": "building",
        "_normalized_city": "Berlin",
        "_type": "building",
        "borough": "Mitte",
        "city": "Berlin",
        "continent": "Europe",
        "country": "Germany",
        "country_code": "de",
        "house_number": "11",
        "neighbourhood": "Brunnenviertel",
        "political_union": "European Union",
        "postcode": "13355",
        "road": "Wattstraße",
        "state": "Berlin",
        "state_code": "BE",
        "suburb": "Gesundbrunnen"
      },
      "confidence": 10,
      "distance_from_q": {
        "meters": 11
      },
      "formatted": "Wattstraße 11, 13355 Berlin, Germany",
      "geometry": {
        "lat": 52.5412642,
        "lng": 13.3905952
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
    "created_http": "Sun, 25 Feb 2024 12:10:12 GMT",
    "created_unix": 1708863012
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

### Customizing ktor client & selecting a client implementation

This project use ktor-client. This makes it possible to use this library on all the 
different platforms that Kotlin has. However, it does mean that you have to 
select client implementations for your platform. Ktor provides quite a few of these.
                 
Refer to the [ktor documentation](https://ktor.io/docs/http-client-engines.html#minimal-version) 
for a list of available clients. 

For example, this readme is generated from a junit test running on the 
jvm platform. There are several options for the jvm and we picked the Java client 
(others are CIO, Apache, and Jetty).

To use the Java client on the jvm, simplye add this to your jvmMain dependencies in `build.gradle.kts` 

```kotlin
implementation("io.ktor:ktor-client-java:\{'$'}ktor_version")
```

The OpencageClient constructor has an httpClient parameter with a sane default that you can override.

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

This is a Kotlin multi platform library that should work on most  kotlin platforms (jvm, js, ios, android, etc). Wasm will be added later, after Kotlin 2.0 stabilizes.

