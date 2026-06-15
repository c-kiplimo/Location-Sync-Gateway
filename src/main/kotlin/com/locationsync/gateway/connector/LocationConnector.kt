package com.locationsync.gateway.connector

import com.locationsync.gateway.model.LocationSnapshot

/**
 * Interface for connectors that pull location data from external sources.
 *
 * Each external source (Google My Business, POS system, ERP, etc.) implements
 * this interface to provide location snapshots.
 */
interface LocationConnector {
    /**
     * Unique identifier for this connector type.
     */
    fun connectorType(): String

    /**
     * Pull location snapshots for the specified location IDs.
     *
     * @param locationIds List of Uberall location IDs to pull data for
     * @return List of location snapshots from this source
     */
    suspend fun pullSnapshots(locationIds: List<Long>): List<LocationSnapshot>
}
