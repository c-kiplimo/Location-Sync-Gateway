package com.locationsync.gateway.service

import com.locationsync.gateway.kafka.event.ConflictDetectedEvent
import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import com.locationsync.gateway.model.enums.FieldType
import com.locationsync.gateway.service.ai.AnomalyDetectionService
import com.locationsync.gateway.service.ai.QualityScoringService
import jakarta.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Production-grade conflict detection service.
 * Orchestrates quality scoring, anomaly detection, and field-level comparison.
 */
@Singleton
class ConflictDetectionService(
    private val qualityScoringService: QualityScoringService,
    private val anomalyDetectionService: AnomalyDetectionService,
    private val uberallDataService: UberallDataService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Detect conflicts between snapshot and Uberall data.
     * Returns list of conflict events ready to be published to Kafka.
     */
    suspend fun detectConflicts(event: LocationSnapshotEvent): List<ConflictDetectedEvent> = coroutineScope {
        val locationId = event.payload.locationId
        val source = event.source

        logger.debug("Starting conflict detection for locationId={}, source={}", locationId, source)

        // Step 1: Calculate quality score
        val qualityScoreDeferred = async {
            try {
                qualityScoringService.calculateQualityScore(event.payload)
            } catch (e: Exception) {
                logger.warn("Quality scoring failed for locationId={}: {}", locationId, e.message)
                null
            }
        }

        // Step 2: Fetch current Uberall data
        val uberallDataDeferred = async {
            try {
                uberallDataService.getLocationData(locationId)
            } catch (e: Exception) {
                logger.error("Failed to fetch Uberall data for locationId={}", locationId, e)
                throw e
            }
        }

        // Step 3: Calculate anomaly score
        val anomalyScoreDeferred = async {
            try {
                anomalyDetectionService.calculateAnomalyScore(event.payload)
            } catch (e: Exception) {
                logger.warn("Anomaly detection failed for locationId={}: {}", locationId, e.message)
                null
            }
        }

        // Await all async operations
        val qualityScore = qualityScoreDeferred.await()
        val uberallData = uberallDataDeferred.await()
        val anomalyScore = anomalyScoreDeferred.await()

        val conflicts = mutableListOf<ConflictDetectedEvent>()

        // Step 4: Field-level comparison
        val fieldsToCompare = extractFieldsForComparison(event.payload)

        for ((fieldType, externalValue) in fieldsToCompare) {
            val uberallValue = uberallData[fieldType.name.lowercase()]

            // Skip if values match
            if (valuesMatch(externalValue, uberallValue)) {
                continue
            }

            // Conflict detected - calculate confidence
            val confidence = calculateConfidence(fieldType, source, qualityScore)

            val conflictEvent = ConflictDetectedEvent(
                conflictId = UUID.randomUUID().toString(),
                locationId = locationId,
                fieldType = fieldType,
                source = source,
                externalValue = externalValue,
                uberallValue = uberallValue,
                confidence = confidence,
                qualityScore = qualityScore,
                anomalyScore = anomalyScore,
                correlationId = event.correlationId
            )

            conflicts.add(conflictEvent)

            logger.info(
                "Conflict detected: locationId={}, fieldType={}, source={}, confidence={}",
                locationId,
                fieldType,
                source,
                confidence
            )
        }

        logger.debug(
            "Conflict detection completed for locationId={}: {} conflicts found",
            locationId,
            conflicts.size
        )

        conflicts
    }

    /**
     * Extract fields for comparison from snapshot payload.
     */
    private fun extractFieldsForComparison(payload: com.locationsync.gateway.kafka.event.SnapshotPayload): Map<FieldType, Any> {
        val fields = mutableMapOf<FieldType, Any>()

        payload.phone?.let { fields[FieldType.PHONE] = it }
        payload.openingHours?.let { fields[FieldType.OPENING_HOURS] = it }
        payload.coordinates?.let { fields[FieldType.COORDINATES] = it }
        payload.categories?.let { fields[FieldType.CATEGORIES] = it }
        payload.address?.let { fields[FieldType.ADDRESS] = it }
        payload.website?.let { fields[FieldType.WEBSITE] = it }
        payload.description?.let { fields[FieldType.DESCRIPTION] = it }
        payload.temporaryClosure?.let { fields[FieldType.TEMPORARY_CLOSURE] = it }

        return fields
    }

    /**
     * Check if two values match (considering type differences).
     */
    private fun valuesMatch(value1: Any?, value2: Any?): Boolean {
        if (value1 == null && value2 == null) return true
        if (value1 == null || value2 == null) return false

        // Normalize strings for comparison
        return when {
            value1 is String && value2 is String -> {
                normalizeString(value1) == normalizeString(value2)
            }
            else -> value1 == value2
        }
    }

    /**
     * Normalize string values for comparison (trim, lowercase).
     */
    private fun normalizeString(value: String): String {
        return value.trim().lowercase()
    }

    /**
     * Calculate confidence score for the conflict.
     * Factors: field type, data source authority, quality score.
     */
    private fun calculateConfidence(
        fieldType: FieldType,
        source: com.locationsync.gateway.model.enums.DataSource,
        qualityScore: com.locationsync.gateway.model.ai.QualityScore?
    ): Double {
        var confidence = 1.0

        // Reduce confidence if quality is low
        if (qualityScore != null && qualityScore.overallScore < 80) {
            val qualityFactor = qualityScore.overallScore / 100.0
            confidence *= qualityFactor
        }

        // Adjust confidence based on source authority for specific fields
        confidence *= when (fieldType) {
            FieldType.TEMPORARY_CLOSURE -> when (source) {
                com.locationsync.gateway.model.enums.DataSource.ERP_SYSTEM -> 1.0
                com.locationsync.gateway.model.enums.DataSource.POS_SYSTEM -> 0.85
                else -> 0.7
            }
            FieldType.OPENING_HOURS -> when (source) {
                com.locationsync.gateway.model.enums.DataSource.GOOGLE_MY_BUSINESS -> 0.95
                else -> 0.8
            }
            else -> 1.0
        }

        return confidence.coerceIn(0.0, 1.0)
    }
}
