package com.locationsync.gateway.service

import com.locationsync.gateway.kafka.event.Address
import com.locationsync.gateway.kafka.event.Coordinates
import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.model.enums.DataSource
import com.locationsync.gateway.model.enums.FieldType
import com.locationsync.gateway.service.ai.AnomalyDetectionService
import com.locationsync.gateway.service.ai.QualityScoringService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("ConflictDetectionService Tests")
class ConflictDetectionServiceTest {

    private lateinit var qualityScoringService: QualityScoringService
    private lateinit var anomalyDetectionService: AnomalyDetectionService
    private lateinit var uberallDataService: UberallDataService
    private lateinit var conflictDetectionService: ConflictDetectionService

    @BeforeEach
    fun setup() {
        qualityScoringService = mockk()
        anomalyDetectionService = mockk()
        uberallDataService = mockk()

        conflictDetectionService = ConflictDetectionService(
            qualityScoringService = qualityScoringService,
            anomalyDetectionService = anomalyDetectionService,
            uberallDataService = uberallDataService
        )
    }

    @Nested
    @DisplayName("No Conflicts Detected")
    inner class NoConflicts {

        @Test
        fun `should return empty list when all fields match`() = runBlocking {
            // Given
            val event = createTestEvent(
                phone = "+254712345678",
                coordinates = Coordinates(-1.286389, 36.817223)
            )

            val uberallData = mapOf(
                "phone" to "+254712345678",
                "coordinates" to Coordinates(-1.286389, 36.817223)
            )

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(100)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.1

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.shouldBeEmpty()
        }

        @Test
        fun `should not detect conflict for null external value when Uberall also null`() = runBlocking {
            // Given
            val event = createTestEvent(description = null)
            val uberallData = mapOf("description" to null)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(100)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.1

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Phone Number Conflicts")
    inner class PhoneConflicts {

        @Test
        fun `should detect phone number conflict`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(90)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.2

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 1
            conflicts.first().apply {
                fieldType shouldBe FieldType.PHONE
                source shouldBe DataSource.GOOGLE_MY_BUSINESS
                externalValue shouldBe "+254799999999"
                uberallValue shouldBe "+254712345678"
                qualityScore shouldNotBe null
                anomalyScore shouldBe 0.2
            }
        }

        @Test
        fun `should include quality score in phone conflict`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")
            val expectedQualityScore = mockQualityScore(85)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns expectedQualityScore
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.3

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.first().qualityScore shouldBe expectedQualityScore
        }
    }

    @Nested
    @DisplayName("Opening Hours Conflicts")
    inner class OpeningHoursConflicts {

        @Test
        fun `should detect opening hours conflict`() = runBlocking {
            // Given
            val externalHours = mapOf("Monday" to "08:00-20:00")
            val uberallHours = mapOf("Monday" to "09:00-18:00")

            val event = createTestEvent(openingHours = externalHours)
            val uberallData = mapOf("openingHours" to uberallHours)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(95)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.1

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 1
            conflicts.first().apply {
                fieldType shouldBe FieldType.OPENING_HOURS
                externalValue shouldBe externalHours
                uberallValue shouldBe uberallHours
            }
        }

        @Test
        fun `should not detect conflict for identical opening hours maps`() = runBlocking {
            // Given
            val hours = mapOf(
                "Monday" to "09:00-18:00",
                "Tuesday" to "09:00-18:00"
            )

            val event = createTestEvent(openingHours = hours)
            val uberallData = mapOf("openingHours" to hours)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(100)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.05

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Coordinates Conflicts")
    inner class CoordinatesConflicts {

        @Test
        fun `should detect coordinates conflict for different locations`() = runBlocking {
            // Given
            val externalCoords = Coordinates(-1.286389, 36.817223) // Nairobi
            val uberallCoords = Coordinates(-4.043477, 39.668206) // Mombasa

            val event = createTestEvent(coordinates = externalCoords)
            val uberallData = mapOf("coordinates" to uberallCoords)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(90)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.8

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 1
            conflicts.first().apply {
                fieldType shouldBe FieldType.COORDINATES
                externalValue shouldBe externalCoords
                uberallValue shouldBe uberallCoords
                anomalyScore shouldBe 0.8 // High anomaly score for large distance
            }
        }

        @Test
        fun `should not detect conflict for identical coordinates`() = runBlocking {
            // Given
            val coords = Coordinates(-1.286389, 36.817223)
            val event = createTestEvent(coordinates = coords)
            val uberallData = mapOf("coordinates" to coords)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(100)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.05

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.shouldBeEmpty()
        }

        @Test
        fun `should detect conflict when external coords null but Uberall has coords`() = runBlocking {
            // Given
            val event = createTestEvent(coordinates = null)
            val uberallData = mapOf("coordinates" to Coordinates(-1.286389, 36.817223))

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(60)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.4

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 1
            conflicts.first().apply {
                fieldType shouldBe FieldType.COORDINATES
                externalValue shouldBe null
                uberallValue shouldNotBe null
            }
        }
    }

    @Nested
    @DisplayName("Categories Conflicts")
    inner class CategoriesConflicts {

        @Test
        fun `should detect categories conflict`() = runBlocking {
            // Given
            val externalCategories = listOf("Restaurant", "Cafe")
            val uberallCategories = listOf("Restaurant")

            val event = createTestEvent(categories = externalCategories)
            val uberallData = mapOf("categories" to uberallCategories)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(95)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.15

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 1
            conflicts.first().apply {
                fieldType shouldBe FieldType.CATEGORIES
                externalValue shouldBe externalCategories
                uberallValue shouldBe uberallCategories
            }
        }

        @Test
        fun `should not detect conflict for same categories in different order`() = runBlocking {
            // Given
            val event = createTestEvent(categories = listOf("Cafe", "Restaurant"))
            val uberallData = mapOf("categories" to listOf("Restaurant", "Cafe"))

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(100)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.05

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Multiple Conflicts")
    inner class MultipleConflicts {

        @Test
        fun `should detect multiple conflicts in single snapshot`() = runBlocking {
            // Given
            val event = createTestEvent(
                phone = "+254799999999",
                coordinates = Coordinates(-4.043477, 39.668206)
            )
            val uberallData = mapOf(
                "phone" to "+254712345678",
                "coordinates" to Coordinates(-1.286389, 36.817223)
            )

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(85)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.6

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 2
            conflicts.map { it.fieldType }.toSet() shouldBe setOf(
                FieldType.PHONE,
                FieldType.COORDINATES
            )
        }

        @Test
        fun `should share quality score across all conflicts in same snapshot`() = runBlocking {
            // Given
            val event = createTestEvent(
                phone = "+254799999999",
                website = "https://new-site.com"
            )
            val uberallData = mapOf(
                "phone" to "+254712345678",
                "website" to "https://old-site.com"
            )
            val expectedQualityScore = mockQualityScore(80)

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns expectedQualityScore
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.3

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts shouldHaveSize 2
            conflicts.forEach { conflict ->
                conflict.qualityScore shouldBe expectedQualityScore
            }
        }
    }

    @Nested
    @DisplayName("Async Service Integration")
    inner class AsyncServiceIntegration {

        @Test
        fun `should call quality scoring service`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(90)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.2

            // When
            conflictDetectionService.detectConflicts(event)

            // Then
            coVerify(exactly = 1) { qualityScoringService.calculateQualityScore(event.payload) }
        }

        @Test
        fun `should call anomaly detection service`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(90)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.2

            // When
            conflictDetectionService.detectConflicts(event)

            // Then
            coVerify(exactly = 1) { anomalyDetectionService.calculateAnomalyScore(event.payload) }
        }

        @Test
        fun `should call Uberall data service`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(90)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.2

            // When
            conflictDetectionService.detectConflicts(event)

            // Then
            coVerify(exactly = 1) { uberallDataService.getLocationData(1001L) }
        }

        @Test
        fun `should run services concurrently`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } coAnswers {
                kotlinx.coroutines.delay(100)
                uberallData
            }
            coEvery { qualityScoringService.calculateQualityScore(any()) } coAnswers {
                kotlinx.coroutines.delay(100)
                mockQualityScore(90)
            }
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } coAnswers {
                kotlinx.coroutines.delay(100)
                0.2
            }

            val startTime = System.currentTimeMillis()

            // When
            conflictDetectionService.detectConflicts(event)

            // Then
            val duration = System.currentTimeMillis() - startTime
            // Should take ~100ms (concurrent) not ~300ms (sequential)
            assert(duration < 250) { "Expected concurrent execution but took ${duration}ms" }
        }
    }

    @Nested
    @DisplayName("Confidence Calculation")
    inner class ConfidenceCalculation {

        @Test
        fun `should calculate confidence based on quality score and data source`() = runBlocking {
            // Given
            val event = createTestEvent(
                source = DataSource.GOOGLE_MY_BUSINESS,
                phone = "+254799999999"
            )
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(95)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.1

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.first().confidence shouldNotBe null
            conflicts.first().confidence!! shouldBe 0.95 // High quality = high confidence
        }

        @Test
        fun `should reduce confidence for low quality scores`() = runBlocking {
            // Given
            val event = createTestEvent(phone = "+254799999999")
            val uberallData = mapOf("phone" to "+254712345678")

            coEvery { uberallDataService.getLocationData(any()) } returns uberallData
            coEvery { qualityScoringService.calculateQualityScore(any()) } returns mockQualityScore(50)
            coEvery { anomalyDetectionService.calculateAnomalyScore(any()) } returns 0.4

            // When
            val conflicts = conflictDetectionService.detectConflicts(event)

            // Then
            conflicts.first().confidence!! shouldBe 0.50
        }
    }

    // Helper methods
    private fun createTestEvent(
        locationId: Long = 1001L,
        source: DataSource = DataSource.GOOGLE_MY_BUSINESS,
        phone: String? = "+254712345678",
        coordinates: Coordinates? = Coordinates(-1.286389, 36.817223),
        openingHours: Map<String, String>? = mapOf("Monday" to "09:00-18:00"),
        categories: List<String>? = listOf("Restaurant"),
        website: String? = "https://example.com",
        description: String? = "A great place"
    ) = LocationSnapshotEvent(
        source = source,
        payload = SnapshotPayload(
            locationId = locationId,
            name = "Test Location",
            coordinates = coordinates,
            address = Address(
                street = "123 Test St",
                city = "Test City",
                state = "Test State",
                postalCode = "12345",
                country = "Test Country"
            ),
            phone = phone,
            openingHours = openingHours,
            categories = categories,
            website = website,
            description = description,
            temporaryClosure = null
        ),
        timestamp = Instant.now()
    )

    private fun mockQualityScore(overallScore: Int) = com.locationsync.gateway.model.ai.QualityScore(
        overallScore = overallScore,
        breakdown = mapOf(
            "coordinates" to 100,
            "address" to 100,
            "openingHours" to 100,
            "keywords" to 100,
            "consistency" to 100
        ),
        issues = emptyList(),
        severity = com.locationsync.gateway.model.enums.Severity.LOW
    )
}
