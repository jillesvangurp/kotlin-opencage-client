package com.jillesvangurp.kotlinopencage

import java.io.FileInputStream
import java.util.Properties

val openCageKey by lazy {
    System.getenv("OPENCAGE_KEY") ?: System.getProperty("opencageApiKey")?: Properties().let {
        it.load(FileInputStream("local.properties"))
        it["opencageApiKey"]?.toString()
    }?: error("api key not found either set the OPENCAGE_KEY environment variable, supply a local.properties file with the opencageApiKey property, or set the opencageApiKey system property")
}