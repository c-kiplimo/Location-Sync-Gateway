package com.locationsync.gateway.service.ai.validator

import com.locationsync.gateway.kafka.event.Coordinates
import com.locationsync.gateway.model.ai.QualityIssue
import com.locationsync.gateway.model.enums.Severity
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CoordinatesValidator Tests")
class CoordinatesValidatorTest {

    private val validator = CoordinatesValidator()

    @Nested
    @DisplayName("Valid Coordinates")
    inner class ValidCoordinates {

        @Test
        fun `should give perfect score for valid coordinates`() {
            // Given
            val coords = Coordinates(latitude = -1.286389, longitude = 36.817223) // Nairobi
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should validate coordinates at equator`() {
            // Given
            val coords = Coordinates(latitude = 0.0, longitude = 36.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should validate coordinates at prime meridian`() {
            // Given
            val coords = Coordinates(latitude = 51.5074, longitude = 0.0) // London
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should validate extreme valid coordinates`() {
            // Given
            val coords = Coordinates(latitude = 89.9, longitude = 179.9)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Missing Coordinates")
    inner class MissingCoordinates {

        @Test
        fun `should return zero score for null coordinates`() {
            // Given
            val coords: Coordinates? = null
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "MISSING_COORDINATES"
                severity shouldBe Severity.CRITICAL
                description shouldBe "Coordinates are missing"
            }
        }
    }

    @Nested
    @DisplayName("Invalid Coordinate Ranges")
    inner class InvalidRanges {

        @Test
        fun `should return zero score for latitude exceeding 90 degrees`() {
            // Given
            val coords = Coordinates(latitude = 91.0, longitude = 0.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "INVALID_LATITUDE"
                severity shouldBe Severity.CRITICAL
            }
        }

        @Test
        fun `should return zero score for latitude below -90 degrees`() {
            // Given
            val coords = Coordinates(latitude = -91.0, longitude = 0.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().issue shouldBe "INVALID_LATITUDE"
        }

        @Test
        fun `should return zero score for longitude exceeding 180 degrees`() {
            // Given
            val coords = Coordinates(latitude = 0.0, longitude = 181.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().issue shouldBe "INVALID_LONGITUDE"
        }

        @Test
        fun `should return zero score for longitude below -180 degrees`() {
            // Given
            val coords = Coordinates(latitude = 0.0, longitude = -181.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().issue shouldBe "INVALID_LONGITUDE"
        }

        @Test
        fun `should return zero score for both invalid latitude and longitude`() {
            // Given
            val coords = Coordinates(latitude = 100.0, longitude = 200.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().issue shouldBe "INVALID_LATITUDE" // First check fails
        }
    }

    @Nested
    @DisplayName("Suspicious Coordinates")
    inner class SuspiciousCoordinates {

        @Test
        fun `should flag null island coordinates as suspicious`() {
            // Given - Null Island (0, 0) is a common error
            val coords = Coordinates(latitude = 0.0, longitude = 0.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 50
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "SUSPICIOUS_NULL_ISLAND"
                severity shouldBe Severity.HIGH
                description shouldBe "Coordinates at (0, 0) are suspicious"
            }
        }

        @Test
        fun `should flag coordinates very close to null island`() {
            // Given - Within 0.01 degrees of Null Island
            val coords = Coordinates(latitude = 0.005, longitude = 0.005)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 50
            issues shouldHaveSize 1
            issues.first().issue shouldBe "SUSPICIOUS_NULL_ISLAND"
        }

        @Test
        fun `should not flag coordinates near but not at null island`() {
            // Given - More than 0.01 degrees away from Null Island
            val coords = Coordinates(latitude = 0.02, longitude = 0.02)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Coordinate Precision")
    inner class CoordinatePrecision {

        @Test
        fun `should give full score for high precision coordinates`() {
            // Given - 6 decimal places (accurate to ~11cm)
            val coords = Coordinates(latitude = -1.286389, longitude = 36.817223)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should give reduced score for low precision coordinates`() {
            // Given - Only 2 decimal places (accurate to ~1.1km)
            val coords = Coordinates(latitude = -1.29, longitude = 36.82)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 80
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "LOW_PRECISION"
                severity shouldBe Severity.MEDIUM
                description shouldBe "Coordinate precision is low (< 4 decimal places)"
            }
        }

        @Test
        fun `should give very low score for integer coordinates`() {
            // Given - Zero decimal places (accurate to ~111km)
            val coords = Coordinates(latitude = -1.0, longitude = 37.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 60
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "LOW_PRECISION"
                severity shouldBe Severity.MEDIUM
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should handle coordinates at north pole`() {
            // Given
            val coords = Coordinates(latitude = 90.0, longitude = 0.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should handle coordinates at south pole`() {
            // Given
            val coords = Coordinates(latitude = -90.0, longitude = 0.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should handle coordinates at international date line`() {
            // Given
            val coords = Coordinates(latitude = 0.0, longitude = 180.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should handle coordinates at negative international date line`() {
            // Given
            val coords = Coordinates(latitude = 0.0, longitude = -180.0)
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(coords, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }
}
