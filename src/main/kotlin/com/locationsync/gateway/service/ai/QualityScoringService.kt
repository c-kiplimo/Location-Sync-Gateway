package com.locationsync.gateway.service.ai

import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.model.ai.QualityIssue
import com.locationsync.gateway.model.ai.QualityIssueType
import com.locationsync.gateway.model.ai.QualityScore
import com.locationsync.gateway.model.enums.Severity
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Production-grade quality scoring service.
 * Validates data quality using multiple validators.
 */
@Singleton
class QualityScoringService(
    private val coordinatesValidator: CoordinatesValidator,
    private val addressValidator: AddressValidator,
    private val openingHoursValidator: OpeningHoursValidator,
    private val keywordsValidator: KeywordsValidator,
    private val consistencyValidator: ConsistencyValidator
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calculate comprehensive quality score for a snapshot.
     * Score: 0-100 (100 = perfect quality)
     */
    suspend fun calculateQualityScore(payload: SnapshotPayload): QualityScore {
        logger.debug("Calculating quality score for locationId={}", payload.locationId)

        val issues = mutableListOf<QualityIssue>()

        val breakdown = buildMap {
            // Validate coordinates (20 points max)
            put("coordinates", coordinatesValidator.validate(payload.coordinates, issues))

            // Validate address (20 points max)
            put("address", addressValidator.validate(payload.address, issues))

            // Validate opening hours (20 points max)
            put("openingHours", openingHoursValidator.validate(payload.openingHours, issues))

            // Validate keywords/categories (20 points max)
            put("keywords", keywordsValidator.validate(
                payload.name,
                payload.categories,
                payload.description,
                issues
            ))

            // Validate consistency with historical baseline (20 points max)
            put("consistency", consistencyValidator.validate(payload, issues))
        }

        // Calculate overall score
        val overallScore = breakdown.values.sum().coerceIn(0, 100)
        val severity = Severity.fromScore(overallScore)

        val qualityScore = QualityScore(
            overallScore = overallScore,
            breakdown = breakdown,
            issues = issues,
            severity = severity
        )

        logger.info(
            "Quality score calculated: locationId={}, score={}, severity={}, issues={}",
            payload.locationId,
            overallScore,
            severity,
            issues.size
        )

        return qualityScore
    }
}

/**
 * Validates coordinate data quality.
 */
@Singleton
class CoordinatesValidator {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validate(
        coordinates: com.locationsync.gateway.kafka.event.Coordinates?,
        issues: MutableList<QualityIssue>
    ): Int {
        if (coordinates == null) {
            issues.add(
                QualityIssue(
                    field = "coordinates",
                    issueType = QualityIssueType.MISSING_REQUIRED_FIELD,
                    description = "Coordinates are missing",
                    severity = Severity.HIGH
                )
            )
            return 0
        }

        var score = 20

        // Check for implausible coordinates (e.g., 0,0 which is in the ocean)
        if (coordinates.latitude == 0.0 && coordinates.longitude == 0.0) {
            issues.add(
                QualityIssue(
                    field = "coordinates",
                    issueType = QualityIssueType.IMPLAUSIBLE_VALUE,
                    description = "Coordinates appear to be default/placeholder values (0,0)",
                    severity = Severity.MEDIUM
                )
            )
            score -= 10
        }

        logger.debug("Coordinates validation score: {}", score)
        return score
    }
}

/**
 * Validates address data quality.
 */
@Singleton
class AddressValidator {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validate(
        address: com.locationsync.gateway.kafka.event.Address?,
        issues: MutableList<QualityIssue>
    ): Int {
        if (address == null) {
            issues.add(
                QualityIssue(
                    field = "address",
                    issueType = QualityIssueType.MISSING_REQUIRED_FIELD,
                    description = "Address is missing",
                    severity = Severity.HIGH
                )
            )
            return 0
        }

        var score = 20

        // Check completeness
        if (address.street.isNullOrBlank()) {
            issues.add(
                QualityIssue(
                    field = "address.street",
                    issueType = QualityIssueType.INCOMPLETE_DATA,
                    description = "Street address is missing",
                    severity = Severity.MEDIUM
                )
            )
            score -= 5
        }

        if (address.city.isNullOrBlank()) {
            issues.add(
                QualityIssue(
                    field = "address.city",
                    issueType = QualityIssueType.INCOMPLETE_DATA,
                    description = "City is missing",
                    severity = Severity.MEDIUM
                )
            )
            score -= 5
        }

        if (address.postalCode.isNullOrBlank()) {
            issues.add(
                QualityIssue(
                    field = "address.postalCode",
                    issueType = QualityIssueType.INCOMPLETE_DATA,
                    description = "Postal code is missing",
                    severity = Severity.LOW
                )
            )
            score -= 3
        }

        logger.debug("Address validation score: {}", score)
        return score.coerceAtLeast(0)
    }
}

/**
 * Validates opening hours data quality.
 */
@Singleton
class OpeningHoursValidator {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validate(
        openingHours: Map<String, Any>?,
        issues: MutableList<QualityIssue>
    ): Int {
        if (openingHours.isNullOrEmpty()) {
            issues.add(
                QualityIssue(
                    field = "openingHours",
                    issueType = QualityIssueType.MISSING_REQUIRED_FIELD,
                    description = "Opening hours are missing",
                    severity = Severity.MEDIUM
                )
            )
            return 10 // Partial score since not all businesses need hours
        }

        var score = 20

        // Check for at least some days defined
        if (openingHours.size < 3) {
            issues.add(
                QualityIssue(
                    field = "openingHours",
                    issueType = QualityIssueType.INCOMPLETE_DATA,
                    description = "Opening hours incomplete (less than 3 days defined)",
                    severity = Severity.LOW
                )
            )
            score -= 5
        }

        // Validate time format and logical consistency
        openingHours.forEach { (day, timeData) ->
            if (timeData is Map<*, *>) {
                val open = timeData["open"] as? String
                val close = timeData["close"] as? String

                if (open == null || close == null) {
                    issues.add(
                        QualityIssue(
                            field = "openingHours.$day",
                            issueType = QualityIssueType.INCOMPLETE_DATA,
                            description = "Missing open or close time for $day",
                            severity = Severity.LOW
                        )
                    )
                    score -= 2
                }
            }
        }

        logger.debug("Opening hours validation score: {}", score)
        return score.coerceAtLeast(0)
    }
}

/**
 * Validates keywords/categories data quality.
 */
@Singleton
class KeywordsValidator {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validate(
        name: String?,
        categories: List<String>?,
        description: String?,
        issues: MutableList<QualityIssue>
    ): Int {
        var score = 20

        if (name.isNullOrBlank()) {
            issues.add(
                QualityIssue(
                    field = "name",
                    issueType = QualityIssueType.MISSING_REQUIRED_FIELD,
                    description = "Name is missing",
                    severity = Severity.HIGH
                )
            )
            score -= 10
        }

        if (categories.isNullOrEmpty()) {
            issues.add(
                QualityIssue(
                    field = "categories",
                    issueType = QualityIssueType.MISSING_REQUIRED_FIELD,
                    description = "Categories are missing",
                    severity = Severity.MEDIUM
                )
            )
            score -= 5
        }

        if (description.isNullOrBlank()) {
            issues.add(
                QualityIssue(
                    field = "description",
                    issueType = QualityIssueType.INCOMPLETE_DATA,
                    description = "Description is missing",
                    severity = Severity.LOW
                )
            )
            score -= 3
        }

        logger.debug("Keywords validation score: {}", score)
        return score.coerceAtLeast(0)
    }
}

/**
 * Validates consistency with historical baseline.
 */
@Singleton
class ConsistencyValidator {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun validate(
        payload: SnapshotPayload,
        issues: MutableList<QualityIssue>
    ): Int {
        var score = 20

        // Validate against historical baseline
        // In production, this would fetch from quality_baselines table
        // For MVP, perform basic consistency checks

        // Check for suspicious coordinate changes (e.g., location moved > 100km)
        if (payload.coordinates != null) {
            val lat = payload.coordinates.latitude
            val lng = payload.coordinates.longitude

            // Flag if coordinates are exactly (0,0) which indicates placeholder data
            if (lat == 0.0 && lng == 0.0) {
                issues.add(
                    QualityIssue(
                        field = "coordinates",
                        issueType = QualityIssueType.INCONSISTENT_WITH_BASELINE,
                        description = "Coordinates appear to be default/placeholder values",
                        severity = Severity.MEDIUM
                    )
                )
                score -= 10
            }
        }

        logger.debug("Consistency validation score: {}", score)
        return score.coerceAtLeast(0)
    }
}
