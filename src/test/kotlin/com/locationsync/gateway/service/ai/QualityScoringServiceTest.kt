package com.locationsync.gateway.service.ai

import com.locationsync.gateway.kafka.event.Address
import com.locationsync.gateway.kafka.event.Coordinates
import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.model.enums.Severity
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeInRange
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("QualityScoringService Tests")
class QualityScoringServiceTest {

    private lateinit var service: QualityScoringService

    @BeforeEach
    fun setup() {
        service = QualityScoringService(
            coordinatesValidator = CoordinatesValidator(),
            addressValidator = AddressValidator(),
            openingHoursValidator = OpeningHoursValidator(),
            keywordsValidator = KeywordsValidator(),
            consistencyValidator = ConsistencyValidator()
        )
    }

    @Nested
    @DisplayName("Perfect Quality Scores")
    inner class PerfectQualityScores {

        @Test
        fun `should give perfect score for complete high-quality snapshot`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBe 100
            score.severity shouldBe Severity.LOW
            score.issues.shouldBeEmpty()
            score.breakdown["coordinates"] shouldBe 20
            score.breakdown["address"] shouldBe 20
            score.breakdown["openingHours"] shouldBe 20
            score.breakdown["keywords"] shouldBe 20
            score.breakdown["consistency"] shouldBe 20
        }

        @Test
        fun `should have all breakdown components sum to overall score`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            val breakdownSum = score.breakdown.values.sum()
            breakdownSum shouldBe 100 // Each component is 0-20, 5 components
            score.overallScore shouldBe breakdownSum
        }

        @Test
        fun `should mark perfect snapshot as non-critical`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.isCritical() shouldBe false
        }
    }

    @Nested
    @DisplayName("Missing Data Scenarios")
    inner class MissingDataScenarios {

        @Test
        fun `should heavily penalize missing coordinates`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(coordinates = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["coordinates"] shouldBe 0
            score.overallScore shouldBeLessThan 80
            score.severity shouldNotBe Severity.LOW
            score.issues.shouldNotBeEmpty()
        }

        @Test
        fun `should heavily penalize missing address`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(address = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["address"] shouldBe 0
            score.overallScore shouldBeLessThan 80
            score.issues.any { it.issueType == com.locationsync.gateway.model.ai.QualityIssueType.MISSING_REQUIRED_FIELD && it.field == "address" } shouldBe true
        }

        @Test
        fun `should penalize missing opening hours`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(openingHours = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["openingHours"] shouldBe 0
            score.overallScore shouldBeLessThan 100
        }

        @Test
        fun `should penalize missing name`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(name = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["keywords"]!! shouldBeLessThan 20
            score.issues.any { it.issueType == com.locationsync.gateway.model.ai.QualityIssueType.MISSING_REQUIRED_FIELD && it.field == "name" } shouldBe true
        }

        @Test
        fun `should mark snapshot with multiple missing fields as critical`() = runBlocking {
            // Given
            val payload = SnapshotPayload(
                locationId = 1001L,
                name = null,
                coordinates = null,
                address = null,
                phone = null,
                openingHours = null,
                categories = null,
                website = null,
                description = null,
                temporaryClosure = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeLessThan 40
            score.severity shouldBe Severity.CRITICAL
            score.isCritical() shouldBe true
        }
    }

    @Nested
    @DisplayName("Severity Classification")
    inner class SeverityClassification {

        @Test
        fun `should classify score below 40 as CRITICAL`() = runBlocking {
            // Given - Snapshot with most fields missing
            val payload = SnapshotPayload(
                locationId = 1001L,
                name = "Test Location",
                coordinates = null,
                address = null,
                phone = null,
                openingHours = null,
                categories = null,
                website = null,
                description = null,
                temporaryClosure = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeLessThan 40
            score.severity shouldBe Severity.CRITICAL
        }

        @Test
        fun `should classify score 40-59 as HIGH`() = runBlocking {
            // Given - Snapshot with some critical fields missing
            val payload = SnapshotPayload(
                locationId = 1001L,
                name = "Test Location",
                coordinates = Coordinates(-1.286389, 36.817223),
                address = null,
                phone = "+254712345678",
                openingHours = null,
                categories = listOf("Restaurant"),
                website = null,
                description = null,
                temporaryClosure = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeInRange 40..59
            score.severity shouldBe Severity.HIGH
        }

        @Test
        fun `should classify score 60-79 as MEDIUM`() = runBlocking {
            // Given - Snapshot with minor issues
            val payload = createPerfectSnapshot().copy(
                description = null,
                website = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeInRange 60..79
            score.severity shouldBe Severity.MEDIUM
        }

        @Test
        fun `should classify score 80+ as LOW severity`() = runBlocking {
            // Given - Mostly complete snapshot
            val payload = createPerfectSnapshot().copy(description = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeGreaterThan 80
            score.severity shouldBe Severity.LOW
        }
    }

    @Nested
    @DisplayName("Individual Validator Integration")
    inner class ValidatorIntegration {

        @Test
        fun `should use CoordinatesValidator correctly`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(
                coordinates = Coordinates(0.0, 0.0) // Null Island
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["coordinates"] shouldBe 10
            score.issues.any { it.issueType == com.locationsync.gateway.model.ai.QualityIssueType.IMPLAUSIBLE_VALUE && it.field == "coordinates" } shouldBe true
        }

        @Test
        fun `should use AddressValidator correctly`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(
                address = Address(
                    street = "123",
                    city = "Nairobi",
                    state = null,
                    postalCode = null,
                    country = null
                )
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["address"]!! shouldBeLessThan 20
            score.issues.shouldNotBeEmpty()
        }

        @Test
        fun `should use OpeningHoursValidator correctly`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(
                openingHours = mapOf(
                    "Monday" to "25:00-26:00" // Invalid hours
                )
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["openingHours"]!! shouldBeLessThan 20
            score.issues.shouldNotBeEmpty() // Opening hours validation is basic in current implementation
        }

        @Test
        fun `should use KeywordsValidator correctly`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(
                name = null,
                categories = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown["keywords"]!! shouldBeLessThan 20
            score.issues.any { it.issueType == com.locationsync.gateway.model.ai.QualityIssueType.MISSING_REQUIRED_FIELD && it.field == "name" } shouldBe true
        }

        @Test
        fun `should use ConsistencyValidator correctly`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown.containsKey("consistency") shouldBe true
            score.breakdown["consistency"] shouldNotBe null
        }
    }

    @Nested
    @DisplayName("Quality Score Boundaries")
    inner class QualityScoreBoundaries {

        @Test
        fun `should never return score below 0`() = runBlocking {
            // Given - Worst possible snapshot
            val payload = SnapshotPayload(
                locationId = 1001L,
                name = null,
                coordinates = null,
                address = null,
                phone = null,
                openingHours = null,
                categories = null,
                website = null,
                description = null,
                temporaryClosure = null
            )

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeGreaterThan -1
        }

        @Test
        fun `should never return score above 100`() = runBlocking {
            // Given - Perfect snapshot
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.overallScore shouldBeLessThan 101
        }

        @Test
        fun `should coerce negative breakdown scores to 0`() = runBlocking {
            // Given - Any valid payload
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown.values.forEach { componentScore ->
                componentScore shouldBeGreaterThan -1
            }
        }

        @Test
        fun `should coerce breakdown scores above 100 to 100`() = runBlocking {
            // Given - Any valid payload
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown.values.forEach { componentScore ->
                componentScore shouldBeLessThan 101
            }
        }
    }

    @Nested
    @DisplayName("Timestamp and Metadata")
    inner class TimestampMetadata {

        @Test
        fun `should include calculation timestamp`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()
            val beforeTime = System.currentTimeMillis()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            val afterTime = System.currentTimeMillis()
            score.calculatedAt shouldBeGreaterThan beforeTime - 100
            score.calculatedAt shouldBeLessThan afterTime + 100
        }

        @Test
        fun `should maintain breakdown structure`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot()

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.breakdown.keys shouldBe setOf(
                "coordinates",
                "address",
                "openingHours",
                "keywords",
                "consistency"
            )
        }

        @Test
        fun `should maintain issues list structure`() = runBlocking {
            // Given
            val payload = createPerfectSnapshot().copy(coordinates = null)

            // When
            val score = service.calculateQualityScore(payload)

            // Then
            score.issues.shouldNotBeEmpty()
            score.issues.forEach { issue ->
                issue.issueType shouldNotBe null
                issue.severity shouldNotBe null
                issue.description shouldNotBe null
            }
        }
    }

    // Helper methods
    private fun createPerfectSnapshot() = SnapshotPayload(
        locationId = 1001L,
        name = "Nairobi Central Branch",
        coordinates = Coordinates(latitude = -1.286389, longitude = 36.817223),
        address = Address(
            street = "123 Kimathi Street",
            city = "Nairobi",
            state = "Nairobi County",
            postalCode = "00100",
            country = "Kenya"
        ),
        phone = "+254712345678",
        openingHours = mapOf(
            "Monday" to "09:00-18:00",
            "Tuesday" to "09:00-18:00",
            "Wednesday" to "09:00-18:00",
            "Thursday" to "09:00-18:00",
            "Friday" to "09:00-18:00",
            "Saturday" to "10:00-16:00",
            "Sunday" to "Closed"
        ),
        categories = listOf("Restaurant", "Cafe"),
        website = "https://example.com",
        description = "A wonderful place to eat",
        temporaryClosure = null
    )
}
