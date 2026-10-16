package com.locationsync.gateway.model.enums

/**
 * Resolution outcomes for conflict decisions.
 * Maps to database enum: resolution_outcome_type
 */
enum class ResolutionOutcome {
    /** Accept the external value and update Uberall */
    AUTO_ACCEPT,

    /** Reject the external value and keep Uberall data */
    AUTO_REVERT,

    /** Flag for manual review by operations team */
    FLAG_FOR_REVIEW;

    companion object {
        fun fromString(value: String): ResolutionOutcome {
            return valueOf(value.uppercase().replace("-", "_"))
        }
    }

    fun displayName(): String = when (this) {
        AUTO_ACCEPT -> "Auto Accept"
        AUTO_REVERT -> "Auto Revert"
        FLAG_FOR_REVIEW -> "Flag for Review"
    }

    fun isAutomated(): Boolean = this == AUTO_ACCEPT || this == AUTO_REVERT

    fun requiresAction(): Boolean = this == AUTO_ACCEPT || this == FLAG_FOR_REVIEW
}