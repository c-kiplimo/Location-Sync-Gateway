package com.locationsync.gateway.kafka.consumer

import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import com.locationsync.gateway.kafka.producer.LocationEventProducerService
import com.locationsync.gateway.service.ConflictDetectionService
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

/**
 * Kafka consumer for processing location snapshot events.
 * Consumes from: location.snapshots
 * Produces to: location.conflicts
 */
@KafkaListener(
    groupId = "location-sync-gateway-snapshot-consumer",
    offsetReset = OffsetReset.EARLIEST,
    clientId = "snapshot-consumer-\${random.uuid}"
)
@Requires(property = "kafka.enabled", value = "true", defaultValue = "false")
class SnapshotConsumer(
    private val conflictDetectionService: ConflictDetectionService,
    private val eventProducer: LocationEventProducerService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Topic("location.snapshots")
    fun receiveSnapshot(
        @KafkaKey key: String,
        event: LocationSnapshotEvent,
        @MessageHeader("kafka_offset") offset: Long,
        @MessageHeader("kafka_partition") partition: Int,
        record: ConsumerRecord<String, LocationSnapshotEvent>
    ) = runBlocking {
        val startTime = System.currentTimeMillis()

        try {
            logger.info(
                "Processing snapshot event: locationId={}, source={}, messageId={}, partition={}, offset={}",
                event.payload.locationId,
                event.source,
                event.messageId,
                partition,
                offset
            )

            // Detect conflicts using the conflict detection service
            val conflicts = conflictDetectionService.detectConflicts(event)

            if (conflicts.isNotEmpty()) {
                logger.info(
                    "Detected {} conflicts for locationId={}, source={}",
                    conflicts.size,
                    event.payload.locationId,
                    event.source
                )

                // Publish conflict events
                eventProducer.sendBatchConflicts(conflicts)
            } else {
                logger.debug(
                    "No conflicts detected for locationId={}, source={}",
                    event.payload.locationId,
                    event.source
                )
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info(
                "Completed processing snapshot: locationId={}, conflicts={}, duration={}ms",
                event.payload.locationId,
                conflicts.size,
                duration
            )

        } catch (e: Exception) {
            logger.error(
                "Error processing snapshot event: messageId={}, locationId={}, partition={}, offset={}",
                event.messageId,
                event.payload.locationId,
                partition,
                offset,
                e
            )
            // In production, this would be sent to a dead letter queue
            throw e // Causes Kafka to retry
        }
    }
}
