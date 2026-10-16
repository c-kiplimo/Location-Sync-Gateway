package com.locationsync.gateway.kafka.event

import com.locationsync.gateway.model.enums.DataSource
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.UUID

/**
 * Event published when a location snapshot is captured from an external source.
 * Topic: location.snapshots
 */
@Serdeable
data class LocationSnapshotEvent(
    /** Unique message ID */
    val messageId: String = UUID.randomUUID().toString(),

    /** Event timestamp */
    val timestamp: Instant = Instant.now(),

    /** Data source that produced this snapshot */
    val source: DataSource,

    /** Snapshot payload */
    val payload: SnapshotPayload,

    /** Correlation ID for tracking */
    val correlationId: String? = null,

    /** Metadata for additional context */
    val metadata: Map<String, String> = emptyMap()
) {
    fun toKey(): String = "${payload.locationId}:${source.name}"
}

/**
 * Snapshot payload containing location data.
 */
@Serdeable
data class SnapshotPayload(
    val locationId: Long,
    val name: String? = null,
    val phone: String? = null,
    val openingHours: Map<String, Any>? = null,
    val coordinates: Coordinates? = null,
    val categories: List<String>? = null,
    val address: Address? = null,
    val website: String? = null,
    val description: String? = null,
    val temporaryClosure: TemporaryClosure? = null,
    val capturedAt: Instant = Instant.now()
)

@Serdeable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    /**
     * Calculate distance to another coordinate using Haversine formula.
     * @return Distance in kilometers
     */
    fun distanceTo(other: Coordinates): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadiusKm * c
    }
}

@Serdeable
data class Address(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
) {
    fun isComplete(): Boolean {
        return !street.isNullOrBlank() &&
               !city.isNullOrBlank() &&
               !postalCode.isNullOrBlank()
    }
}

@Serdeable
data class TemporaryClosure(
    val closed: Boolean,
    val reason: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)
