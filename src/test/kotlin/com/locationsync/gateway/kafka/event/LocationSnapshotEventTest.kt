package com.locationsync.gateway.kafka.event

import com.locationsync.gateway.model.enums.DataSource
import com.locationsync.gateway.util.TestFixtures
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@MicronautTest
@DisplayName("LocationSnapshotEvent Tests")
class LocationSnapshotEventTest {

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("Event Creation")
    inner class EventCreation {

        @Test
        fun `should create event with all fields`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()
            val source = DataSource.GOOGLE_MY_BUSINESS
            val correlationId = "test-correlation-123"

            // When
            val event = LocationSnapshotEvent(
                source = source,
                payload = payload,
                correlationId = correlationId
            )

            // Then
            event.source shouldBe source
            event.payload shouldBe payload
            event.correlationId shouldBe correlationId
            event.messageId.shouldNotBeBlank()
            event.timestamp shouldNotBe null
        }

        @Test
        fun `should generate unique message IDs`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()

            // When
            val event1 = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = payload
            )
            val event2 = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = payload
            )

            // Then
            event1.messageId shouldNotBe event2.messageId
        }

        @Test
        fun `should set timestamp automatically`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()
            val beforeTime = Instant.now()

            // When
            val event = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = payload
            )

            // Then
            val afterTime = Instant.now()
            event.timestamp.isAfter(beforeTime.minusSeconds(1)) shouldBe true
            event.timestamp.isBefore(afterTime.plusSeconds(1)) shouldBe true
        }

        @Test
        fun `should allow null correlation ID`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()

            // When
            val event = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = payload,
                correlationId = null
            )

            // Then
            event.correlationId shouldBe null
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {

        @Test
        fun `should serialize to JSON`() {
            // Given
            val event = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createPerfectSnapshot()
            )

            // When
            val json = objectMapper.writeValueAsString(event)

            // Then
            json shouldNotBe null
            json.contains("messageId") shouldBe true
            json.contains("timestamp") shouldBe true
            json.contains("source") shouldBe true
            json.contains("payload") shouldBe true
        }

        @Test
        fun `should deserialize from JSON`() {
            // Given
            val originalEvent = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createPerfectSnapshot(),
                correlationId = "test-123"
            )
            val json = objectMapper.writeValueAsString(originalEvent)

            // When
            val deserializedEvent = objectMapper.readValue(json, LocationSnapshotEvent::class.java)

            // Then
            deserializedEvent.messageId shouldBe originalEvent.messageId
            deserializedEvent.source shouldBe originalEvent.source
            deserializedEvent.correlationId shouldBe originalEvent.correlationId
            deserializedEvent.payload.locationId shouldBe originalEvent.payload.locationId
        }

        @Test
        fun `should serialize all data sources`() {
            // Given
            val sources = listOf(
                DataSource.GOOGLE_MY_BUSINESS,
                DataSource.POS_SYSTEM,
                DataSource.ERP_SYSTEM,
                DataSource.WEBHOOK
            )

            sources.forEach { source ->
                // When
                val event = LocationSnapshotEvent(
                    source = source,
                    payload = TestFixtures.createMinimalSnapshot()
                )
                val json = objectMapper.writeValueAsString(event)
                val deserialized = objectMapper.readValue(json, LocationSnapshotEvent::class.java)

                // Then
                deserialized.source shouldBe source
            }
        }

        @Test
        fun `should handle null optional fields in payload`() {
            // Given
            val event = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createMinimalSnapshot()
            )

            // When
            val json = objectMapper.writeValueAsString(event)
            val deserialized = objectMapper.readValue(json, LocationSnapshotEvent::class.java)

            // Then
            deserialized.payload.address shouldBe null
            deserialized.payload.phone shouldBe null
            deserialized.payload.openingHours shouldBe null
        }
    }

    @Nested
    @DisplayName("Coordinates Serialization")
    inner class CoordinatesSerialization {

        @Test
        fun `should serialize coordinates correctly`() {
            // Given
            val coords = Coordinates(
                latitude = -1.286389,
                longitude = 36.817223
            )

            // When
            val json = objectMapper.writeValueAsString(coords)

            // Then
            json.contains("latitude") shouldBe true
            json.contains("longitude") shouldBe true
            json.contains("-1.286389") shouldBe true
            json.contains("36.817223") shouldBe true
        }

        @Test
        fun `should deserialize coordinates correctly`() {
            // Given
            val originalCoords = Coordinates(
                latitude = -1.286389,
                longitude = 36.817223
            )
            val json = objectMapper.writeValueAsString(originalCoords)

            // When
            val deserialized = objectMapper.readValue(json, Coordinates::class.java)

            // Then
            deserialized.latitude shouldBe originalCoords.latitude
            deserialized.longitude shouldBe originalCoords.longitude
        }

        @Test
        fun `should calculate distance between coordinates`() {
            // Given
            val nairobi = TestFixtures.nairobiCoordinates()
            val mombasa = TestFixtures.mombasaCoordinates()

            // When
            val distance = nairobi.distanceTo(mombasa)

            // Then
            // Distance between Nairobi and Mombasa is ~440km
            distance shouldBe 440.0 // Approximate
        }

        @Test
        fun `should calculate zero distance for same location`() {
            // Given
            val coords = TestFixtures.nairobiCoordinates()

            // When
            val distance = coords.distanceTo(coords)

            // Then
            distance shouldBe 0.0
        }
    }

    @Nested
    @DisplayName("Address Serialization")
    inner class AddressSerialization {

        @Test
        fun `should serialize address correctly`() {
            // Given
            val address = TestFixtures.kenyanAddress()

            // When
            val json = objectMapper.writeValueAsString(address)

            // Then
            json.contains("street") shouldBe true
            json.contains("city") shouldBe true
            json.contains("state") shouldBe true
            json.contains("postalCode") shouldBe true
            json.contains("country") shouldBe true
        }

        @Test
        fun `should deserialize address correctly`() {
            // Given
            val originalAddress = TestFixtures.kenyanAddress()
            val json = objectMapper.writeValueAsString(originalAddress)

            // When
            val deserialized = objectMapper.readValue(json, Address::class.java)

            // Then
            deserialized.street shouldBe originalAddress.street
            deserialized.city shouldBe originalAddress.city
            deserialized.state shouldBe originalAddress.state
            deserialized.postalCode shouldBe originalAddress.postalCode
            deserialized.country shouldBe originalAddress.country
        }

        @Test
        fun `should handle null address fields`() {
            // Given
            val address = Address(
                street = "123 Main St",
                city = null,
                state = null,
                postalCode = null,
                country = null
            )

            // When
            val json = objectMapper.writeValueAsString(address)
            val deserialized = objectMapper.readValue(json, Address::class.java)

            // Then
            deserialized.street shouldBe "123 Main St"
            deserialized.city shouldBe null
            deserialized.state shouldBe null
            deserialized.postalCode shouldBe null
            deserialized.country shouldBe null
        }
    }

    @Nested
    @DisplayName("Complex Payload Serialization")
    inner class ComplexPayloadSerialization {

        @Test
        fun `should serialize opening hours map`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()

            // When
            val json = objectMapper.writeValueAsString(payload)
            val deserialized = objectMapper.readValue(json, SnapshotPayload::class.java)

            // Then
            deserialized.openingHours shouldBe payload.openingHours
        }

        @Test
        fun `should serialize categories list`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot()

            // When
            val json = objectMapper.writeValueAsString(payload)
            val deserialized = objectMapper.readValue(json, SnapshotPayload::class.java)

            // Then
            deserialized.categories shouldBe payload.categories
        }

        @Test
        fun `should serialize temporary closure map`() {
            // Given
            val payload = TestFixtures.createTemporaryClosureSnapshot()

            // When
            val json = objectMapper.writeValueAsString(payload)
            val deserialized = objectMapper.readValue(json, SnapshotPayload::class.java)

            // Then
            deserialized.temporaryClosure shouldBe payload.temporaryClosure
        }

        @Test
        fun `should handle empty collections`() {
            // Given
            val payload = TestFixtures.createPerfectSnapshot().copy(
                openingHours = emptyMap(),
                categories = emptyList()
            )

            // When
            val json = objectMapper.writeValueAsString(payload)
            val deserialized = objectMapper.readValue(json, SnapshotPayload::class.java)

            // Then
            deserialized.openingHours shouldBe emptyMap()
            deserialized.categories shouldBe emptyList()
        }
    }

    @Nested
    @DisplayName("Event Key Generation")
    inner class EventKeyGeneration {

        @Test
        fun `should generate consistent partition key`() {
            // Given
            val event = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createPerfectSnapshot(locationId = 1001L)
            )

            // When
            val key1 = event.toKey()
            val key2 = event.toKey()

            // Then
            key1 shouldBe key2
            key1 shouldBe "1001"
        }

        @Test
        fun `should generate different keys for different locations`() {
            // Given
            val event1 = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createPerfectSnapshot(locationId = 1001L)
            )
            val event2 = LocationSnapshotEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                payload = TestFixtures.createPerfectSnapshot(locationId = 1002L)
            )

            // When
            val key1 = event1.toKey()
            val key2 = event2.toKey()

            // Then
            key1 shouldNotBe key2
            key1 shouldBe "1001"
            key2 shouldBe "1002"
        }
    }
}
