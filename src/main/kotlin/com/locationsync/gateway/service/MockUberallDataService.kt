package com.locationsync.gateway.service

import jakarta.inject.Singleton

/**
 * Mock Uberall data service for MVP1.
 *
 * Simulates fetching location data from Uberall API.
 * Returns hardcoded data that creates specific conflict scenarios when compared to Google data.
 */
@Singleton
class MockUberallDataService {

    companion object {
        private const val DEFAULT_PHONE = "+254712345678"
        private const val DEFAULT_HOURS = "Mon-Fri: 09:00-18:00"
        private const val DEFAULT_WEBSITE = "https://example.com"

        private val DEFAULT_FIELDS = mapOf(
            "PHONE" to DEFAULT_PHONE,
            "OPENING_HOURS" to DEFAULT_HOURS,
            "WEBSITE" to DEFAULT_WEBSITE
        )
    }

    /**
     * Get mock Uberall data for a location.
     *
     * @param locationId Uberall location ID
     * @return Map of field type to value
     */
    fun getLocationData(locationId: Long): Map<String, String> {
        return when (locationId) {
            1001L -> DEFAULT_FIELDS  // Different phone from Google

            1002L -> DEFAULT_FIELDS  // Different hours from Google

            1003L -> DEFAULT_FIELDS + ("CATEGORIES" to "Restaurant")  // Different category from Google

            1004L -> DEFAULT_FIELDS  // Matches Google (no conflict)

            else -> emptyMap()
        }
    }
}