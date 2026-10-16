package com.locationsync.gateway.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Service for fetching location data from the central platform API.
 * Production implementation would use HTTP client to call the platform REST API.
 *
 * For this portfolio project, we simulate platform data with realistic responses.
 */
@Singleton
class PlatformDataService {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get current location data from the central platform.
     * @return Map of field names to values
     */
    suspend fun getLocationData(locationId: Long): Map<String, Any?> {
        logger.debug("Fetching platform data for locationId={}", locationId)

        // Simulate platform API call with cached/mock data
        // In production, this would:
        // 1. Check Redis cache first
        // 2. Call platform API if not cached
        // 3. Cache the result

        val data = when (locationId) {
            1001L -> mapOf(
                "phone" to "+254712345678",
                "openingHours" to mapOf(
                    "monday" to mapOf("open" to "09:00", "close" to "18:00"),
                    "tuesday" to mapOf("open" to "09:00", "close" to "18:00")
                ),
                "coordinates" to mapOf("lat" to -1.2921, "lng" to 36.8219),
                "categories" to listOf("restaurant"),
                "address" to mapOf(
                    "street" to "Moi Avenue",
                    "city" to "Nairobi",
                    "postalCode" to "00100"
                )
            )
            1002L -> mapOf(
                "phone" to "+254799999999",
                "openingHours" to mapOf(
                    "monday" to mapOf("open" to "09:00", "close" to "18:00"),
                    "tuesday" to mapOf("open" to "09:00", "close" to "18:00")
                ),
                "coordinates" to mapOf("lat" to -4.0435, "lng" to 39.6682),
                "categories" to listOf("restaurant", "cafe")
            )
            1003L -> mapOf(
                "phone" to "+254788888888",
                "openingHours" to mapOf(
                    "monday" to mapOf("open" to "10:00", "close" to "22:00")
                ),
                "coordinates" to mapOf("lat" to -0.0917, "lng" to 34.7680),
                "categories" to listOf("restaurant")
            )
            1004L -> mapOf(
                "phone" to "+254777777777",
                "temporaryClosure" to mapOf("closed" to false),
                "coordinates" to mapOf("lat" to -1.1743, "lng" to 36.9453)
            )
            else -> {
                // Default data for other locations
                mapOf(
                    "phone" to "+254700000000",
                    "coordinates" to mapOf("lat" to -1.2921, "lng" to 36.8219),
                    "categories" to listOf("business")
                )
            }
        }

        logger.info("Fetched platform data for locationId={}: {} fields", locationId, data.size)
        return data
    }

    /**
     * Get historical quality baseline for a location.
     */
    suspend fun getQualityBaseline(locationId: Long): Map<String, Any>? {
        logger.debug("Fetching quality baseline for locationId={}", locationId)

        // Simulated baseline data
        // In production, this would query the quality_baselines table
        return when (locationId) {
            in 1001..1004 -> mapOf(
                "avgQualityScore" to 85,
                "historicalAverage" to 84,
                "sampleCount" to 30
            )
            else -> null
        }
    }
}
