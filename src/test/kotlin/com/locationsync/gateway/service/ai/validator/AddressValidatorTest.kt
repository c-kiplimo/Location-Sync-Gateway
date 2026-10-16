package com.locationsync.gateway.service.ai.validator

import com.locationsync.gateway.kafka.event.Address
import com.locationsync.gateway.model.ai.QualityIssue
import com.locationsync.gateway.model.enums.Severity
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AddressValidator Tests")
class AddressValidatorTest {

    private val validator = AddressValidator()

    @Nested
    @DisplayName("Complete Addresses")
    inner class CompleteAddresses {

        @Test
        fun `should give perfect score for complete address`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should give perfect score for complete US address`() {
            // Given
            val address = Address(
                street = "1600 Pennsylvania Avenue NW",
                city = "Washington",
                state = "DC",
                postalCode = "20500",
                country = "United States"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should give perfect score for complete UK address`() {
            // Given
            val address = Address(
                street = "10 Downing Street",
                city = "London",
                state = "Greater London",
                postalCode = "SW1A 2AA",
                country = "United Kingdom"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Missing Address Components")
    inner class MissingComponents {

        @Test
        fun `should return zero for null address`() {
            // Given
            val address: Address? = null
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 0
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "MISSING_ADDRESS"
                severity shouldBe Severity.CRITICAL
                description shouldBe "Address is completely missing"
            }
        }

        @Test
        fun `should deduct points for missing street`() {
            // Given
            val address = Address(
                street = null,
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 80
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "MISSING_STREET"
                severity shouldBe Severity.HIGH
            }
        }

        @Test
        fun `should deduct points for blank street`() {
            // Given
            val address = Address(
                street = "   ",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 80
            issues shouldHaveSize 1
            issues.first().issue shouldBe "MISSING_STREET"
        }

        @Test
        fun `should deduct points for missing city`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = null,
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 80
            issues shouldHaveSize 1
            issues.first().issue shouldBe "MISSING_CITY"
        }

        @Test
        fun `should deduct points for missing postal code`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = null,
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 90
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "MISSING_POSTAL_CODE"
                severity shouldBe Severity.MEDIUM
            }
        }

        @Test
        fun `should deduct points for missing country`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = null
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 80
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "MISSING_COUNTRY"
                severity shouldBe Severity.HIGH
            }
        }

        @Test
        fun `should accumulate penalties for multiple missing fields`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = null,
                state = null,
                postalCode = null,
                country = null
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 50 // -20 city, -10 state, -10 postal, -20 country = 40 deducted
            issues shouldHaveSize 4
        }
    }

    @Nested
    @DisplayName("Address Format Validation")
    inner class AddressFormat {

        @Test
        fun `should flag very short street address`() {
            // Given
            val address = Address(
                street = "123",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 90
            issues shouldHaveSize 1
            issues.first().apply {
                issue shouldBe "INCOMPLETE_STREET"
                severity shouldBe Severity.MEDIUM
                description shouldBe "Street address seems incomplete (too short)"
            }
        }

        @Test
        fun `should not flag reasonable street address`() {
            // Given
            val address = Address(
                street = "123 Main St",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should flag suspiciously long street address`() {
            // Given
            val address = Address(
                street = "A".repeat(201),
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 90
            issues shouldHaveSize 1
            issues.first().issue shouldBe "OVERLY_LONG_STREET"
        }
    }

    @Nested
    @DisplayName("Postal Code Validation")
    inner class PostalCodeValidation {

        @Test
        fun `should accept valid US ZIP code`() {
            // Given
            val address = Address(
                street = "123 Main St",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "United States"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should accept valid US ZIP+4 code`() {
            // Given
            val address = Address(
                street = "123 Main St",
                city = "New York",
                state = "NY",
                postalCode = "10001-1234",
                country = "United States"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should accept valid UK postcode`() {
            // Given
            val address = Address(
                street = "10 Downing Street",
                city = "London",
                state = "Greater London",
                postalCode = "SW1A 2AA",
                country = "United Kingdom"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should accept valid Kenyan postal code`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Optional State Field")
    inner class OptionalState {

        @Test
        fun `should not penalize missing state for non-US addresses`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = null,
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 90 // Only minor deduction for missing state
            issues shouldHaveSize 1
            issues.first().severity shouldBe Severity.LOW
        }

        @Test
        fun `should accept address with state present`() {
            // Given
            val address = Address(
                street = "123 Kimathi Street",
                city = "Nairobi",
                state = "Nairobi County",
                postalCode = "00100",
                country = "Kenya"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should handle address with all empty strings`() {
            // Given
            val address = Address(
                street = "",
                city = "",
                state = "",
                postalCode = "",
                country = ""
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 30 // Heavily penalized
            issues.size shouldBe 5 // All fields missing
        }

        @Test
        fun `should handle address with only street`() {
            // Given
            val address = Address(
                street = "123 Main Street",
                city = null,
                state = null,
                postalCode = null,
                country = null
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 50
            issues shouldHaveSize 4
        }

        @Test
        fun `should handle special characters in address`() {
            // Given
            val address = Address(
                street = "Rue de l'Église #42",
                city = "Paris",
                state = "Île-de-France",
                postalCode = "75001",
                country = "France"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }

        @Test
        fun `should handle unicode characters in address`() {
            // Given
            val address = Address(
                street = "東京都渋谷区 1-2-3",
                city = "東京",
                state = "東京都",
                postalCode = "150-0001",
                country = "日本"
            )
            val issues = mutableListOf<QualityIssue>()

            // When
            val score = validator.validate(address, issues)

            // Then
            score shouldBe 100
            issues.shouldBeEmpty()
        }
    }
}
