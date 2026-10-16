package com.locationsync.gateway.kafka.producer

import com.locationsync.gateway.kafka.event.ConflictDetectedEvent
import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory

/**
 * Kafka producer for publishing location events.
 * Uses Micronaut Kafka annotations for simplified producer configuration.
 */
@KafkaClient(id = "location-event-producer")
@Requires(property = "kafka.enabled", value = "true", defaultValue = "true")
interface LocationEventProducer {

    companion object {
        const val SNAPSHOTS_TOPIC = "location.snapshots"
        const val CONFLICTS_TOPIC = "location.conflicts"
        const val DECISIONS_TOPIC = "location.decisions"
    }

    /**
     * Publish a location snapshot event.
     * Partitioned by location ID for ordering guarantees.
     */
    @Topic(SNAPSHOTS_TOPIC)
    fun publishSnapshot(
        @KafkaKey key: String,
        event: LocationSnapshotEvent
    )

    /**
     * Publish a conflict detected event.
     * Partitioned by location ID + field type for ordering guarantees.
     */
    @Topic(CONFLICTS_TOPIC)
    fun publishConflict(
        @KafkaKey key: String,
        event: ConflictDetectedEvent
    )
}

/**
 * Wrapper service for producing events with logging and error handling.
 */
@jakarta.inject.Singleton
class LocationEventProducerService(
    private val producer: LocationEventProducer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendSnapshot(event: LocationSnapshotEvent) {
        try {
            val key = event.toKey()
            producer.publishSnapshot(key, event)
            logger.info(
                "Published snapshot event: locationId={}, source={}, messageId={}",
                event.payload.locationId,
                event.source,
                event.messageId
            )
        } catch (e: Exception) {
            logger.error("Failed to publish snapshot event: ${event.messageId}", e)
            throw e
        }
    }

    fun sendConflict(event: ConflictDetectedEvent) {
        try {
            val key = event.toKey()
            producer.publishConflict(key, event)
            logger.info(
                "Published conflict event: conflictId={}, locationId={}, fieldType={}, messageId={}",
                event.conflictId,
                event.locationId,
                event.fieldType,
                event.messageId
            )
        } catch (e: Exception) {
            logger.error("Failed to publish conflict event: ${event.messageId}", e)
            throw e
        }
    }

    fun sendBatchSnapshots(events: List<LocationSnapshotEvent>) {
        logger.info("Publishing batch of {} snapshot events", events.size)
        events.forEach { sendSnapshot(it) }
    }

    fun sendBatchConflicts(events: List<ConflictDetectedEvent>) {
        logger.info("Publishing batch of {} conflict events", events.size)
        events.forEach { sendConflict(it) }
    }
}
