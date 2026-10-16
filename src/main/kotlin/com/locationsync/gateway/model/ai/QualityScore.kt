package com.locationsync.gateway.model.ai

import com.locationsync.gateway.model.enums.Severity
import io.micronaut.serde.annotation.Serdeable

/**
 * Quality score for a location snapshot (0-100).
 * Used to validate data quality before conflict resolution.
 */
@Serdeable
data class QualityScore(
    /** Overall quality score (0-100) */
    val overallScore: Int,

    /** Breakdown by field category */
    val breakdown: Map<String, Int>,

    /** Quality issues detected */
    val issues: List<QualityIssue>,

    /** Severity level based on overall score */
    val severity: Severity,

    /** Timestamp when score was calculated */
    val calculatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(overallScore in 0..100) { "Overall score must be between 0 and 100" }
    }

    fun isCritical(): Boolean = severity == Severity.CRITICAL

    fun isAcceptable(): Boolean = overallScore >= 60

    fun hasIssues(): Boolean = issues.isNotEmpty()

    companion object {
        fun perfect(): QualityScore = QualityScore(
            overallScore = 100,
            breakdown = mapOf(
                "coordinates" to 20,
                "address" to 20,
                "openingHours" to 20,
                "keywords" to 20,
                "consistency" to 20
            ),
            issues = emptyList(),
            severity = Severity.LOW
        )

        fun fromBreakdown(breakdown: Map<String, Int>): QualityScore {
            val overall = breakdown.values.sum().coerceIn(0, 100)
            return QualityScore(
                overallScore = overall,
                breakdown = breakdown,
                issues = emptyList(),
                severity = Severity.fromScore(overall)
            )
        }
    }
}

/**
 * Individual quality issue detected in a snapshot.
 */
@Serdeable
data class QualityIssue(
    val field: String,
    val issueType: QualityIssueType,
    val description: String,
    val severity: Severity
)

/**
 * Types of quality issues.
 */
enum class QualityIssueType {
    MISSING_REQUIRED_FIELD,
    INVALID_FORMAT,
    IMPLAUSIBLE_VALUE,
    INCOMPLETE_DATA,
    INCONSISTENT_WITH_BASELINE,
    SUSPICIOUS_PATTERN;

    fun displayName(): String = name.lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
