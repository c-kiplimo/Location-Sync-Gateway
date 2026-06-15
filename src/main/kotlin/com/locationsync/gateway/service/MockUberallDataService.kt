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

    /**
     * Get mock Uberall data for a location.
     *
     * @param locationId Uberall location ID
     * @return Map of field type to value
     */
    fun getLocationData(locationId: Long): Map<String, String> {
        return when (locationId) {
            1001L -> mapOf(
                "PHONE" to "+254712345678",  // Different from Google
                "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                "WEBSITE" to "https://example.com"
            )

            1002L -> mapOf(
                "PHONE" to "+254712345678",
                "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",  // Different from Google
                "WEBSITE" to "https://example.com"
            )

            1003L -> mapOf(
                "PHONE" to "+254712345678",
                "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                "WEBSITE" to "https://example.com",
                "CATEGORIES" to "Restaurant"  // Different from Google
            )

            1004L -> mapOf(
                "PHONE" to "+254712345678",
                "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                "WEBSITE" to "https://example.com"
            )

            else -> emptyMap()
        }
    }
}