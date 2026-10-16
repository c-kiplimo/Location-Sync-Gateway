package com.locationsync.gateway.kafka.event

import com.locationsync.gateway.model.ai.QualityScore
import com.locationsync.gateway.model.enums.DataSource
import com.locationsync.gateway.model.enums.FieldType
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.UUID

/**
 * Event published when a conflict is detected.
 * Topic: location.conflicts
 */
@Serdeable
data class ConflictDetectedEvent(
    val messageId: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val conflictId: String,
    val locationId: Long,
    val fieldType: FieldType,
    val source: DataSource,
    val externalValue: Any?,
    val uberallValue: Any?,
    val confidence: Double,
    val qualityScore: QualityScore? = null,
    val anomalyScore: Double? = null,
    val detectedAt: Instant = Instant.now(),
    val correlationId: String? = null
) {
    fun toKey(): String = "$locationId:${fieldType.name}"
}
