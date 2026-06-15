package com.locationsync.gateway.connector

import com.locationsync.gateway.model.LocationSnapshot
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Mock implementation of Google My Business connector for MVP1.
 *
 * Returns hardcoded location data simulating scenarios:
 * - Location 1001: Phone differs from Uberall (tests AUTO_REVERT)
 * - Location 1002: Opening hours differ from Uberall (tests AUTO_ACCEPT)
 * - Location 1003: Category differs from Uberall (tests FLAG_FOR_REVIEW)
 * - Location 1004: All fields match Uberall (no conflict)
 */
@Singleton
class MockGoogleMyBusinessConnector : LocationConnector {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun connectorType() = "GOOGLE_MY_BUSINESS"

    override suspend fun pullSnapshots(locationIds: List<Long>): List<LocationSnapshot> {
        logger.info("MockGoogleMyBusinessConnector pulling snapshots for ${locationIds.size} locations")

        return locationIds.mapNotNull { locationId ->
            when (locationId) {
                1001L -> LocationSnapshot(
                    locationId = locationId,
                    source = connectorType(),
                    fields = mapOf(
                        "PHONE" to "+254799999999",  // Different from Uberall
                        "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                        "WEBSITE" to "https://example.com"
                    ),
                    capturedAt = Instant.now()
                )

                1002L -> LocationSnapshot(
                    locationId = locationId,
                    source = connectorType(),
                    fields = mapOf(
                        "PHONE" to "+254712345678",
                        "OPENING_HOURS" to "Mon-Fri: 08:00-20:00",  // Different from Uberall
                        "WEBSITE" to "https://example.com"
                    ),
                    capturedAt = Instant.now()
                )

                1003L -> LocationSnapshot(
                    locationId = locationId,
                    source = connectorType(),
                    fields = mapOf(
                        "PHONE" to "+254712345678",
                        "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                        "WEBSITE" to "https://example.com",
                        "CATEGORIES" to "Coffee Shop"  // Different from Uberall
                    ),
                    capturedAt = Instant.now()
                )

                1004L -> LocationSnapshot(
                    locationId = locationId,
                    source = connectorType(),
                    fields = mapOf(
                        "PHONE" to "+254712345678",
                        "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
                        "WEBSITE" to "https://example.com"
                    ),
                    capturedAt = Instant.now()
                )

                else -> {
                    logger.warn("No mock data for location $locationId")
                    null
                }
            }
        }
    }
}