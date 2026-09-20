package com.example.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Domain utility for approximate distance calculation (Haversine Formula)
 * and privacy masking of exact coordinates to protect user privacy.
 */
object LocationHelper {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates great-circle distance between two points in kilometers.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == 0.0 && lon1 == 0.0 || lat2 == 0.0 && lon2 == 0.0) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = EARTH_RADIUS_KM * c

        // Round to 1 decimal place
        return Math.round(distance * 10.0) / 10.0
    }

    /**
     * Applies privacy fuzzing/rounding to prevent exposing exact private home addresses.
     * Fuzzes coordinates to ~1 km resolution.
     */
    fun getPrivacyMaskedCoordinates(lat: Double, lon: Double): Pair<Double, Double> {
        val fuzzedLat = Math.round(lat * 100.0) / 100.0
        val fuzzedLon = Math.round(lon * 100.0) / 100.0
        return Pair(fuzzedLat, fuzzedLon)
    }
}
