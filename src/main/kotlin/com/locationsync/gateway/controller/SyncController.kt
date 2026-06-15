package com.locationsync.gateway.controller

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.ResolutionDecision
import com.locationsync.gateway.model.ResolutionOutcome
import com.locationsync.gateway.service.SyncResult
import com.locationsync.gateway.service.SyncService
import com.locationsync.gateway.storage.InMemoryStorage
import io.micronaut.http.annotation.*
import java.util.UUID

/**
 * REST API controller for manual sync triggering and viewing results.
 */
@Controller("/api/v1")
class SyncController(
    private val syncService: SyncService,
    private val storage: InMemoryStorage
) {

    /**
     * Trigger a manual sync for specific locations.
     *
     * Example: POST /api/v1/sync/trigger?locationIds=1001,1002,1003
     */
    @Post("/sync/trigger")
    suspend fun triggerSync(
        @QueryValue locationIds: List<Long>
    ): SyncResult {
        return syncService.triggerSync(locationIds)
    }

    /**
     * Get all detected conflicts.
     *
     * Example: GET /api/v1/conflicts
     */
    @Get("/conflicts")
    fun getAllConflicts(): List<Conflict> {
        return storage.getAllConflicts()
    }

    /**
     * Get conflicts for a specific location.
     *
     * Example: GET /api/v1/conflicts/location/1001
     */
    @Get("/conflicts/location/{locationId}")
    fun getConflictsByLocation(@PathVariable locationId: Long): List<Conflict> {
        return storage.getConflictsByLocationId(locationId)
    }

    /**
     * Get conflicts that require review.
     *
     * Example: GET /api/v1/conflicts/flagged
     */
    @Get("/conflicts/flagged")
    fun getFlaggedConflicts(): List<Conflict> {
        return storage.getConflictsByOutcome(ResolutionOutcome.FLAG_FOR_REVIEW)
    }

    /**
     * Get all resolution decisions.
     *
     * Example: GET /api/v1/decisions
     */
    @Get("/decisions")
    fun getAllDecisions(): List<ResolutionDecision> {
        return storage.getAllDecisions()
    }

    /**
     * Get decision for a specific conflict.
     *
     * Example: GET /api/v1/decisions/conflict/{conflictId}
     */
    @Get("/decisions/conflict/{conflictId}")
    fun getDecisionByConflict(@PathVariable conflictId: UUID): ResolutionDecision? {
        return storage.getDecisionByConflictId(conflictId)
    }

    /**
     * Health check endpoint.
     */
    @Get("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "location-sync-gateway",
            "version" to "MVP1"
        )
    }
}