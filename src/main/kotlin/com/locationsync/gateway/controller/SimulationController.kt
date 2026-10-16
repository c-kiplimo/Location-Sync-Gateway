package com.locationsync.gateway.controller

import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.kafka.producer.LocationEventProducerService
import com.locationsync.gateway.model.enums.DataSource
import com.locationsync.gateway.util.DataSimulator
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Controller for simulating location data for testing and ML training
 *
 * WARNING: This controller should be disabled in production environments
 * Set `simulation.enabled=false` in production config
 */
@Controller("/api/v1/simulation")
@Tag(name = "Data Simulation", description = "Generate simulated location data for testing")
class SimulationController @Inject constructor(
    private val dataSimulator: DataSimulator,
    private val eventProducer: LocationEventProducerService
) {

    private val logger = LoggerFactory.getLogger(SimulationController::class.java)

    @Post("/generate")
    @Operation(
        summary = "Generate simulated location snapshots",
        description = """
            Generates and publishes simulated location snapshot events to Kafka.

            Quality distribution:
            - 70% high quality (90-100 score)
            - 20% medium quality (60-89 score)
            - 10% low quality (< 60 score)

            Use this endpoint to:
            - Generate test data for development
            - Create ML training datasets
            - Stress test the system
            - Simulate real-world data patterns
        """
    )
    fun generateSimulatedData(
        @Parameter(description = "Number of snapshots to generate", example = "100")
        @QueryValue(defaultValue = "10") count: Int,

        @Parameter(description = "Data source type", example = "GOOGLE_MY_BUSINESS")
        @QueryValue(defaultValue = "GOOGLE_MY_BUSINESS") source: DataSource,

        @Parameter(description = "Starting location ID", example = "10000")
        @QueryValue(defaultValue = "10000") startLocationId: Long,

        @Parameter(description = "Delay between messages in milliseconds", example = "100")
        @QueryValue(defaultValue = "0") delayMs: Long
    ): HttpResponse<SimulationResponse> = runBlocking {
        logger.info("Generating $count simulated snapshots from source $source")

        val events = dataSimulator.generateBatch(
            count = count,
            source = source,
            startLocationId = startLocationId
        )

        var highQuality = 0
        var mediumQuality = 0
        var lowQuality = 0

        events.forEachIndexed { index, event ->
            // Publish to Kafka
            eventProducer.sendSnapshot(event)

            // Track quality distribution (estimated based on completeness)
            val estimatedQuality = estimateQuality(event.payload)
            when {
                estimatedQuality >= 90 -> highQuality++
                estimatedQuality >= 60 -> mediumQuality++
                else -> lowQuality++
            }

            // Add delay if specified
            if (delayMs > 0 && index < events.size - 1) {
                delay(delayMs)
            }
        }

        logger.info("Published $count simulated snapshots: $highQuality high quality, $mediumQuality medium quality, $lowQuality low quality")

        HttpResponse.ok(
            SimulationResponse(
                generated = count,
                source = source.name,
                locationIdRange = LocationIdRange(startLocationId, startLocationId + count - 1),
                qualityDistribution = QualityDistribution(
                    high = highQuality,
                    medium = mediumQuality,
                    low = lowQuality
                ),
                message = "Successfully generated and published $count simulated location snapshots"
            )
        )
    }

    @Post("/generate-conflicts")
    @Operation(
        summary = "Generate snapshot pairs with conflicts",
        description = """
            Generates pairs of snapshots (baseline + conflicting) to simulate conflict detection scenarios.

            Useful for:
            - Testing conflict resolution engine
            - Training ML models on conflict patterns
            - Validating resolution rules
        """
    )
    fun generateConflicts(
        @Parameter(description = "Number of conflict pairs to generate", example = "50")
        @QueryValue(defaultValue = "10") count: Int,

        @Parameter(description = "Type of conflict to generate (PHONE, HOURS, COORDINATES, CATEGORIES, ADDRESS)")
        @QueryValue conflictType: String?
    ): HttpResponse<SimulationResponse> = runBlocking {
        logger.info("Generating $count conflict scenarios")

        val conflictTypes = if (conflictType != null) {
            listOf(DataSimulator.ConflictType.valueOf(conflictType))
        } else {
            DataSimulator.ConflictType.values().toList()
        }

        var generatedPairs = 0

        repeat(count) { index ->
            val locationId = 20000L + index
            val baseSnapshot = dataSimulator.generateHighQualitySnapshot(locationId)
            val conflictingSnapshot = dataSimulator.generateConflictSnapshot(
                baseSnapshot,
                conflictTypes.random()
            )

            // Publish baseline (simulating Uberall data)
            eventProducer.sendSnapshot(
                com.locationsync.gateway.kafka.event.LocationSnapshotEvent(
                    source = DataSource.WEBHOOK, // Baseline from Uberall
                    payload = baseSnapshot
                )
            )

            delay(100) // Small delay

            // Publish conflicting snapshot
            eventProducer.sendSnapshot(
                com.locationsync.gateway.kafka.event.LocationSnapshotEvent(
                    source = DataSource.GOOGLE_MY_BUSINESS,
                    payload = conflictingSnapshot
                )
            )

            generatedPairs++
        }

        logger.info("Published $generatedPairs conflict scenario pairs")

        HttpResponse.ok(
            SimulationResponse(
                generated = generatedPairs * 2, // Baseline + conflicting
                source = "MIXED",
                locationIdRange = LocationIdRange(20000L, 20000L + count - 1),
                qualityDistribution = null,
                message = "Successfully generated $generatedPairs conflict scenario pairs"
            )
        )
    }

    @Post("/generate-closures")
    @Operation(
        summary = "Generate temporary closure snapshots",
        description = """
            Generates location snapshots with temporary closure data.

            Useful for:
            - Testing closure detection logic
            - Validating notification workflows
            - Training ML models on closure patterns
        """
    )
    fun generateClosures(
        @Parameter(description = "Number of closure snapshots to generate", example = "20")
        @QueryValue(defaultValue = "10") count: Int,

        @Parameter(description = "Starting location ID", example = "30000")
        @QueryValue(defaultValue = "30000") startLocationId: Long
    ): HttpResponse<SimulationResponse> = runBlocking {
        logger.info("Generating $count temporary closure snapshots")

        repeat(count) { index ->
            val locationId = startLocationId + index
            val closureSnapshot = dataSimulator.generateTemporaryClosureSnapshot(locationId)

            eventProducer.sendSnapshot(
                com.locationsync.gateway.kafka.event.LocationSnapshotEvent(
                    source = DataSource.ERP_SYSTEM,
                    payload = closureSnapshot
                )
            )
        }

        logger.info("Published $count temporary closure snapshots")

        HttpResponse.ok(
            SimulationResponse(
                generated = count,
                source = DataSource.ERP_SYSTEM.name,
                locationIdRange = LocationIdRange(startLocationId, startLocationId + count - 1),
                qualityDistribution = null,
                message = "Successfully generated $count temporary closure snapshots"
            )
        )
    }

    @Post("/stress-test")
    @Operation(
        summary = "Generate high volume of data for stress testing",
        description = """
            Generates a large volume of simulated data rapidly to stress test the system.

            WARNING: Use with caution as this can overload the system
        """
    )
    fun stressTest(
        @Parameter(description = "Number of snapshots to generate", example = "1000")
        @QueryValue(defaultValue = "1000") count: Int
    ): HttpResponse<SimulationResponse> = runBlocking {
        logger.warn("Starting stress test with $count snapshots")

        val startTime = System.currentTimeMillis()
        val events = dataSimulator.generateBatch(
            count = count,
            source = DataSource.GOOGLE_MY_BUSINESS,
            startLocationId = 50000L
        )

        events.forEach { event ->
            eventProducer.sendSnapshot(event)
        }

        val duration = System.currentTimeMillis() - startTime
        val throughput = (count.toDouble() / duration) * 1000

        logger.warn("Stress test completed: $count messages in ${duration}ms (${throughput.toInt()} msg/sec)")

        HttpResponse.ok(
            SimulationResponse(
                generated = count,
                source = "GOOGLE_MY_BUSINESS",
                locationIdRange = LocationIdRange(50000L, 50000L + count - 1),
                qualityDistribution = null,
                message = "Stress test completed: $count messages in ${duration}ms (${throughput.toInt()} msg/sec)"
            )
        )
    }

    // Helper method to estimate quality based on field completeness
    private fun estimateQuality(payload: SnapshotPayload): Int {
        var score = 0

        if (payload.name != null && payload.name.length > 3) score += 15
        if (payload.coordinates != null && payload.coordinates != com.locationsync.gateway.kafka.event.Coordinates(0.0, 0.0)) score += 20
        if (payload.address?.street != null && payload.address.street.length > 5) score += 15
        if (payload.address?.city != null) score += 15
        if (payload.address?.country != null) score += 10
        if (payload.phone != null && payload.phone.length > 8) score += 10
        if (payload.openingHours != null && payload.openingHours.isNotEmpty()) score += 10
        if (payload.categories != null && payload.categories.isNotEmpty()) score += 5

        return score
    }

    // Response models
    data class SimulationResponse(
        val generated: Int,
        val source: String,
        val locationIdRange: LocationIdRange,
        val qualityDistribution: QualityDistribution?,
        val message: String
    )

    data class LocationIdRange(
        val start: Long,
        val end: Long
    )

    data class QualityDistribution(
        val high: Int,
        val medium: Int,
        val low: Int
    )
}
