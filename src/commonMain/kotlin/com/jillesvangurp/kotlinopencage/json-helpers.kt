@file:OptIn(ExperimentalSerializationApi::class)

package com.jillesvangurp.kotlinopencage

import com.jillesvangurp.geojson.Geometry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlin.reflect.KProperty

/**
 * Default kotlinx.serialization `Json` that does all the right things.
 *
 * Used by e.g. the [SearchClient] to deserialize REST responses.
 *
 */
val DEFAULT_JSON: Json = Json {
    // don't rely on external systems being written in kotlin or even having a language with default values
    // the default of false is insane and dangerous
    encodeDefaults = true
    // save space
    prettyPrint = false
    // people adding shit to the json is OK, we're forward compatible and will just ignore it
    isLenient = true
    // encoding nulls is meaningless and a waste of space.
    explicitNulls = false
    // adding enum values is OK even if older clients won't understand it
    ignoreUnknownKeys=true
    // make sure new enum values don't break deserialization (will be null)
    coerceInputValues=true
    // elasticsearch can return NaN values
    allowSpecialFloatingPointValues=true
    // handle NaN and itinity
    allowSpecialFloatingPointValues=true
}

/**
 * Pretty printing kotlinx.serialization `Json` that you may use for debugging.
 */
val DEFAULT_PRETTY_JSON: Json = Json {
    encodeDefaults = true
    prettyPrint = true
    isLenient = true
    explicitNulls = false
    ignoreUnknownKeys=true
    coerceInputValues=true
    allowSpecialFloatingPointValues=true
}

private fun Any.stringify(): String {
    return when(this) {
        is String -> this
        is KProperty<*> -> this.name
        is Enum<*> -> this.name
        else -> this.toString()
    }
}

/**
 * Extract a json element from the receiver using the vararg [keys].
 *
 * Returns the element if this is a JsonObject and it has the element or null otherwise. It will dig out the elements by name recursively.
 *
 * Note keys are of type [Any] and will be converted to String. This is so you
 * can safely use numbers, enum values, property references (of type KProperty<*>), etc.
 * in addition to String values. Anything else is converted using toString.
 */
fun JsonElement?.getElement(vararg keys: Any): JsonElement? {
    return if(keys.isNotEmpty()) {
        when (this) {
            is JsonObject -> get(keys[0].stringify())?.let {
                if(keys.size==1) {
                    it
                } else {
                    it.getElement(*keys.sliceArray(1..<keys.size))
                }
            }
            else -> null
        }
    } else null
}

fun JsonObject?.getPrimitive(vararg keys: Any): JsonPrimitive? = getElement(*keys)?.jsonPrimitive

fun JsonObject?.getDouble(vararg keys: Any): Double? = getPrimitive(*keys)?.doubleOrNull
fun JsonObject?.getLong(vararg keys: Any): Long? = getPrimitive(*keys)?.longOrNull
fun JsonObject?.getString(vararg keys: Any): String? = getPrimitive(*keys)?.contentOrNull
fun JsonObject?.getBoolean(vararg keys: Any): Boolean = getPrimitive(*keys)?.booleanOrNull?:false

fun JsonObject?.getObject(vararg keys: Any): JsonObject? = getElement(*keys)?.jsonObject
fun JsonObject?.getArray(vararg keys: Any): JsonArray? = getElement(*keys)?.jsonArray

fun JsonObject?.getStringList(vararg keys: Any): List<String>? = getElement(*keys)?.jsonArray?.let { a ->
    a.map { e -> e.jsonPrimitive.content }
}
fun JsonObject?.getDoubleList(vararg keys: Any): List<Double>? = getElement(*keys)?.jsonArray?.let { a ->
    a.map { e -> e.jsonPrimitive.double }
}

fun <T> JsonObject?.deserialize(serializer: KSerializer<T>, vararg keys: Any) = getElement(keys)?.let {
    DEFAULT_JSON.decodeFromJsonElement(serializer,it)
}

inline fun <reified T> JsonObject?.deserialize(vararg keys: Any) = getElement(keys)?.let {
    DEFAULT_JSON.decodeFromJsonElement<T>(it)
}

fun <T> JsonObject?.deserializeList(serializer: KSerializer<T>, vararg keys: Any) = getArray(keys)?.map {
    DEFAULT_JSON.decodeFromJsonElement(serializer,it)
}

inline fun <reified T> JsonObject?.deserializeList(vararg keys: Any) = getArray(keys)?.map {
    DEFAULT_JSON.decodeFromJsonElement<T>(it)
}

fun JsonObject?.getPoint(vararg keys: Any) = getObject(keys).asPoint

val JsonObject?.asPoint get() = this.getDouble("lng")?.let {longitude ->
    val latitude = this.getDouble("lat")?: error("missing lat")
    Geometry.Point(doubleArrayOf(longitude,latitude))
}










