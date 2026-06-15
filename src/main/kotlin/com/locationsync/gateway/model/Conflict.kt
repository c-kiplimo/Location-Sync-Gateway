package com.locationsync.gateway.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a detected conflict between external source data and Uberall data.
 */
data class Conflict(
    val id: UUID = UUID.randomUUID(),
    val locationId: Long,
    val fieldType: String,
    val source: String,
    val externalValue: String,
    val uberallValue: String?,
    val detectedAt: Instant = Instant.now()
)