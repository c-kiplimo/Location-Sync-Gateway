package com.locationsync.gateway.service

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.ResolutionDecision
import com.locationsync.gateway.model.ResolutionOutcome
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Resolves conflicts using hardcoded rules for MVP1.
 *
 * Rules:
 * - PHONE from Google → AUTO_REVERT (HQ controls phone numbers)
 * - OPENING_HOURS from Google → AUTO_ACCEPT (franchisees can set their own hours)
 * - Everything else → FLAG_FOR_REVIEW
 */
@Singleton
class ResolutionEngine {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Resolve a conflict using hardcoded rules.
     *
     * @param conflict The detected conflict
     * @return Resolution decision
     */
    fun resolve(conflict: Conflict): ResolutionDecision {
        val (outcome, reason, ruleApplied) = when {
            conflict.fieldType == "PHONE" && conflict.source == "GOOGLE_MY_BUSINESS" -> {
                Triple(
                    ResolutionOutcome.AUTO_REVERT,
                    "Phone numbers are centrally managed by head office",
                    "google-phone-hq-override"
                )
            }

            conflict.fieldType == "OPENING_HOURS" && conflict.source == "GOOGLE_MY_BUSINESS" -> {
                Triple(
                    ResolutionOutcome.AUTO_ACCEPT,
                    "Franchisees are allowed to set their own opening hours",
                    "google-opening-hours-franchisee"
                )
            }

            else -> {
                Triple(
                    ResolutionOutcome.FLAG_FOR_REVIEW,
                    "No automatic resolution rule configured for ${conflict.fieldType} from ${conflict.source}",
                    null
                )
            }
        }

        val decision = ResolutionDecision(
            conflictId = conflict.id,
            locationId = conflict.locationId,
            fieldType = conflict.fieldType,
            source = conflict.source,
            externalValue = conflict.externalValue,
            uberallValue = conflict.uberallValue,
            outcome = outcome,
            reason = reason,
            ruleApplied = ruleApplied
        )

        logger.info(
            "Resolved conflict ${conflict.id}: outcome=$outcome, " +
                "rule=$ruleApplied, location=${conflict.locationId}, field=${conflict.fieldType}"
        )

        return decision
    }
}