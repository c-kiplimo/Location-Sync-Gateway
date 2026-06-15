package com.locationsync.gateway.model

import java.time.Instant

/**
 * Represents a snapshot of location data captured from an external source.
 */
data class LocationSnapshot(
    val locationId: Long,
    val source: String,
    val fields: Map<String, String>,
    val capturedAt: Instant
)