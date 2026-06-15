package com.locationsync.gateway.service

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.LocationSnapshot
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Detects conflicts between external source data and Uberall data.
 */
@Singleton
class ConflictDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Detect conflicts by comparing snapshot fields against Uberall data.
     *
     * @param snapshot Data captured from external source
     * @param uberallData Current data from Uberall (field -> value)
     * @return List of detected conflicts
     */
    fun detect(
        snapshot: LocationSnapshot,
        uberallData: Map<String, String>
    ): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()

        for ((fieldType, externalValue) in snapshot.fields) {
            val uberallValue = uberallData[fieldType]

            // Skip if values match
            if (externalValue == uberallValue) {
                continue
            }

            // Conflict detected
            conflicts.add(
                Conflict(
                    locationId = snapshot.locationId,
                    fieldType = fieldType,
                    source = snapshot.source,
                    externalValue = externalValue,
                    uberallValue = uberallValue
                )
            )

            logger.debug(
                "Conflict detected: location=${snapshot.locationId}, " +
                    "field=$fieldType, external='$externalValue', uberall='$uberallValue'"
            )
        }

        return conflicts
    }
}