package com.locationsync.gateway.storage

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.ResolutionDecision
import com.locationsync.gateway.model.ResolutionOutcome
import jakarta.inject.Singleton
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory storage for MVP1.
 *
 * Stores conflicts and decisions in memory. Data is lost on restart.
 */
@Singleton
class InMemoryStorage {

    private val conflicts = ConcurrentHashMap<UUID, Conflict>()
    private val decisions = ConcurrentHashMap<UUID, ResolutionDecision>()

    /**
     * Store a detected conflict.
     */
    fun saveConflict(conflict: Conflict) {
        conflicts[conflict.id] = conflict
    }

    /**
     * Get all conflicts.
     */
    fun getAllConflicts(): List<Conflict> {
        return conflicts.values.toList().sortedByDescending { it.detectedAt }
    }

    /**
     * Get conflicts by location ID.
     */
    fun getConflictsByLocationId(locationId: Long): List<Conflict> {
        return conflicts.values.filter { it.locationId == locationId }
            .sortedByDescending { it.detectedAt }
    }

    /**
     * Get conflicts by outcome (after resolution).
     */
    fun getConflictsByOutcome(outcome: ResolutionOutcome): List<Conflict> {
        val decisionsByConflictId = decisions.values.associateBy { it.conflictId }
        return conflicts.values.filter { conflict ->
            decisionsByConflictId[conflict.id]?.outcome == outcome
        }.sortedByDescending { it.detectedAt }
    }

    /**
     * Store a resolution decision.
     */
    fun saveDecision(decision: ResolutionDecision) {
        decisions[decision.id] = decision
    }

    /**
     * Get all resolution decisions.
     */
    fun getAllDecisions(): List<ResolutionDecision> {
        return decisions.values.toList().sortedByDescending { it.decidedAt }
    }

    /**
     * Get decision for a specific conflict.
     */
    fun getDecisionByConflictId(conflictId: UUID): ResolutionDecision? {
        return decisions.values.find { it.conflictId == conflictId }
    }

    /**
     * Clear all data (useful for testing).
     */
    fun clear() {
        conflicts.clear()
        decisions.clear()
    }
}