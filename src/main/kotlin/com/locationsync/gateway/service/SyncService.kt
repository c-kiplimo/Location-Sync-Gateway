package com.locationsync.gateway.service

import com.locationsync.gateway.connector.LocationConnector
import com.locationsync.gateway.storage.InMemoryStorage
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Orchestrates the full sync cycle: pull → detect → resolve → store.
 */
@Singleton
class SyncService(
    private val connectors: List<LocationConnector>,
    private val conflictDetector: ConflictDetector,
    private val resolutionEngine: ResolutionEngine,
    private val storage: InMemoryStorage,
    private val mockUberallData: MockUberallDataService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Execute a manual sync for the specified location IDs.
     *
     * @param locationIds Location IDs to sync
     * @return Summary of sync results
     */
    suspend fun triggerSync(locationIds: List<Long>): SyncResult {
        logger.info("Starting sync for ${locationIds.size} locations")

        var totalConflicts = 0
        var autoAccepted = 0
        var autoReverted = 0
        var flaggedForReview = 0

        for (connector in connectors) {
            logger.debug("Pulling snapshots from ${connector.connectorType()}")

            val snapshots = connector.pullSnapshots(locationIds)
            logger.debug("Retrieved ${snapshots.size} snapshots from ${connector.connectorType()}")

            for (snapshot in snapshots) {
                // Get current Uberall data for this location
                val uberallData = mockUberallData.getLocationData(snapshot.locationId)

                // Detect conflicts
                val conflicts = conflictDetector.detect(snapshot, uberallData)
                totalConflicts += conflicts.size

                // Resolve each conflict
                for (conflict in conflicts) {
                    storage.saveConflict(conflict)

                    val decision = resolutionEngine.resolve(conflict)
                    storage.saveDecision(decision)

                    when (decision.outcome) {
                        com.locationsync.gateway.model.ResolutionOutcome.AUTO_ACCEPT -> autoAccepted++
                        com.locationsync.gateway.model.ResolutionOutcome.AUTO_REVERT -> autoReverted++
                        com.locationsync.gateway.model.ResolutionOutcome.FLAG_FOR_REVIEW -> flaggedForReview++
                    }
                }
            }
        }

        val result = SyncResult(
            locationsProcessed = locationIds.size,
            conflictsDetected = totalConflicts,
            autoAccepted = autoAccepted,
            autoReverted = autoReverted,
            flaggedForReview = flaggedForReview
        )

        logger.info("Sync completed: $result")
        return result
    }
}

/**
 * Result summary of a sync cycle.
 */
data class SyncResult(
    val locationsProcessed: Int,
    val conflictsDetected: Int,
    val autoAccepted: Int,
    val autoReverted: Int,
    val flaggedForReview: Int
)