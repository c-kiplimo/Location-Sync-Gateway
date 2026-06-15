package com.locationsync.gateway.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a decision made by the resolution engine for a conflict.
 */
data class ResolutionDecision(
    val id: UUID = UUID.randomUUID(),
    val conflictId: UUID,
    val locationId: Long,
    val fieldType: String,
    val source: String,
    val externalValue: String,
    val uberallValue: String?,
    val outcome: ResolutionOutcome,
    val reason: String,
    val ruleApplied: String? = null,
    val decidedAt: Instant = Instant.now()
)

/**
 * Possible outcomes of conflict resolution.
 */
enum class ResolutionOutcome {
    /**
     * External value is accepted and will update Uberall.
     */
    AUTO_ACCEPT,

    /**
     * External value is rejected; Uberall value is authoritative.
     */
    AUTO_REVERT,

    /**
     * Conflict cannot be resolved automatically; requires human review.
     */
    FLAG_FOR_REVIEW
}