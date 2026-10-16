package com.locationsync.gateway.service.ai

import com.locationsync.gateway.kafka.event.SnapshotPayload
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Anomaly detection service using statistical methods.
 * Production implementation would use Isolation Forest or similar ML algorithm.
 *
 * For this portfolio project, we simulate anomaly detection with realistic scoring.
 */
@Singleton
class AnomalyDetectionService {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calculate anomaly score for a snapshot.
     * Score: 0.0-1.0 (higher = more anomalous)
     * Threshold: 0.7 (scores above this are considered anomalous)
     */
    suspend fun calculateAnomalyScore(payload: SnapshotPayload): Double {
        logger.debug("Calculating anomaly score for locationId={}", payload.locationId)

        val features = extractFeatures(payload)
        val score = calculateScore(features)

        logger.info(
            "Anomaly score calculated: locationId={}, score={}, anomalous={}",
            payload.locationId,
            String.format("%.3f", score),
            score > ANOMALY_THRESHOLD
        )

        return score
    }

    /**
     * Extract features for anomaly detection.
     */
    private fun extractFeatures(payload: SnapshotPayload): Map<String, Double> {
        val features = mutableMapOf<String, Double>()

        // Feature 1: Coordinate distance from expected location
        payload.coordinates?.let {
            // Simulate baseline comparison
            // In production, this would compare against historical baseline
            val baselineLat = -1.2921 // Nairobi example
            val baselineLng = 36.8219
            val distance = calculateDistance(
                it.latitude,
                it.longitude,
                baselineLat,
                baselineLng
            )
            features["coord_distance_km"] = distance
        }

        // Feature 2: Number of fields changed
        var fieldsPresent = 0
        if (payload.phone != null) fieldsPresent++
        if (payload.openingHours != null) fieldsPresent++
        if (payload.coordinates != null) fieldsPresent++
        if (payload.categories != null) fieldsPresent++
        if (payload.address != null) fieldsPresent++
        if (payload.website != null) fieldsPresent++
        if (payload.description != null) fieldsPresent++

        features["fields_present"] = fieldsPresent.toDouble()

        // Feature 3: Time since last change (simulated)
        // In production, this would compare against last snapshot timestamp
        features["hours_since_last_change"] = Random.nextDouble(1.0, 168.0) // 1 hour to 1 week

        // Feature 4: Quality score delta (simulated)
        // In production, this would compare current vs historical quality
        features["quality_delta"] = Random.nextDouble(-20.0, 5.0)

        return features
    }

    /**
     * Calculate anomaly score using Isolation Forest-like logic.
     * Simulated for portfolio demonstration.
     */
    private fun calculateScore(features: Map<String, Double>): Double {
        var score = 0.0

        // Suspicious coordinate movement (> 10km)
        val coordDistance = features["coord_distance_km"] ?: 0.0
        if (coordDistance > 10.0) {
            score += 0.4
        } else if (coordDistance > 1.0) {
            score += 0.2
        }

        // Too many fields changed at once (bulk edit pattern)
        val fieldsPresent = features["fields_present"] ?: 0.0
        if (fieldsPresent > 5) {
            score += 0.2
        }

        // Recent change (< 1 hour since last change is suspicious)
        val hoursSinceChange = features["hours_since_last_change"] ?: 0.0
        if (hoursSinceChange < 1.0) {
            score += 0.3
        }

        // Large quality degradation
        val qualityDelta = features["quality_delta"] ?: 0.0
        if (qualityDelta < -15.0) {
            score += 0.3
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusKm * c
    }

    companion object {
        const val ANOMALY_THRESHOLD = 0.7
    }
}
