package com.locationsync.gateway.util

import com.locationsync.gateway.kafka.event.Address
import com.locationsync.gateway.kafka.event.Coordinates
import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.model.ai.QualityIssue
import com.locationsync.gateway.model.ai.QualityScore
import com.locationsync.gateway.model.enums.DataSource
import com.locationsync.gateway.model.enums.Severity
import java.time.Instant

/**
 * Test fixtures and builders for creating test data
 * Provides consistent test data across all test suites
 */
object TestFixtures {

    /**
     * Creates a perfect quality snapshot with all fields populated
     */
    fun createPerfectSnapshot(
        locationId: Long = 1001L,
        name: String = "Perfect Test Location",
        coordinates: Coordinates = Coordinates(-1.286389, 36.817223),
        phone: String = "+254712345678"
    ): SnapshotPayload = SnapshotPayload(
        locationId = locationId,
        name = name,
        coordinates = coordinates,
        address = Address(
            street = "123 Kimathi Street",
            city = "Nairobi",
            state = "Nairobi County",
            postalCode = "00100",
            country = "Kenya"
        ),
        phone = phone,
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
        description = "A wonderful place to eat and relax",
        temporaryClosure = null
    )

    /**
     * Creates a minimal snapshot with only required fields
     */
    fun createMinimalSnapshot(
        locationId: Long = 1002L
    ): SnapshotPayload = SnapshotPayload(
        locationId = locationId,
        name = "Minimal Location",
        coordinates = Coordinates(-1.286389, 36.817223),
        address = null,
        phone = null,
        openingHours = null,
        categories = null,
        website = null,
        description = null,
        temporaryClosure = null
    )

    /**
     * Creates a snapshot with poor data quality
     */
    fun createPoorQualitySnapshot(
        locationId: Long = 1003L
    ): SnapshotPayload = SnapshotPayload(
        locationId = locationId,
        name = "Bad",
        coordinates = Coordinates(0.0, 0.0), // Null Island - suspicious
        address = Address(
            street = "123",
            city = null,
            state = null,
            postalCode = null,
            country = null
        ),
        phone = null,
        openingHours = mapOf("Monday" to "25:00-26:00"), // Invalid hours
        categories = null,
        website = null,
        description = null,
        temporaryClosure = null
    )

    /**
     * Creates a snapshot with temporary closure
     */
    fun createTemporaryClosureSnapshot(
        locationId: Long = 1004L,
        startDate: String = "2026-10-01",
        endDate: String = "2026-10-15"
    ): SnapshotPayload = createPerfectSnapshot(locationId = locationId).copy(
        temporaryClosure = com.locationsync.gateway.kafka.event.TemporaryClosure(
            closed = true,
            reason = "Renovation",
            startDate = startDate,
            endDate = endDate
        )
    )

    /**
     * Creates a location snapshot event
     */
    fun createSnapshotEvent(
        payload: SnapshotPayload,
        source: DataSource = DataSource.GOOGLE_MY_BUSINESS,
        correlationId: String? = null
    ): LocationSnapshotEvent = LocationSnapshotEvent(
        source = source,
        payload = payload,
        timestamp = Instant.now(),
        correlationId = correlationId
    )

    /**
     * Creates Nairobi coordinates
     */
    fun nairobiCoordinates() = Coordinates(
        latitude = -1.286389,
        longitude = 36.817223
    )

    /**
     * Creates Mombasa coordinates
     */
    fun mombasaCoordinates() = Coordinates(
        latitude = -4.043477,
        longitude = 39.668206
    )

    /**
     * Creates Kisumu coordinates
     */
    fun kisumuCoordinates() = Coordinates(
        latitude = -0.091702,
        longitude = 34.767956
    )

    /**
     * Creates a complete Kenyan address
     */
    fun kenyanAddress(
        street: String = "123 Kimathi Street",
        city: String = "Nairobi",
        postalCode: String = "00100"
    ) = Address(
        street = street,
        city = city,
        state = "$city County",
        postalCode = postalCode,
        country = "Kenya"
    )

    /**
     * Creates a complete US address
     */
    fun usAddress(
        street: String = "1600 Pennsylvania Avenue NW",
        city: String = "Washington",
        state: String = "DC",
        postalCode: String = "20500"
    ) = Address(
        street = street,
        city = city,
        state = state,
        postalCode = postalCode,
        country = "United States"
    )

    /**
     * Creates a quality score
     */
    fun createQualityScore(
        overallScore: Int = 100,
        coordinates: Int = 100,
        address: Int = 100,
        openingHours: Int = 100,
        keywords: Int = 100,
        consistency: Int = 100,
        issues: List<QualityIssue> = emptyList()
    ) = QualityScore(
        overallScore = overallScore,
        breakdown = mapOf(
            "coordinates" to coordinates,
            "address" to address,
            "openingHours" to openingHours,
            "keywords" to keywords,
            "consistency" to consistency
        ),
        issues = issues,
        severity = when {
            overallScore < 40 -> Severity.CRITICAL
            overallScore < 60 -> Severity.HIGH
            overallScore < 80 -> Severity.MEDIUM
            else -> Severity.LOW
        }
    )

    /**
     * Creates a quality issue
     */
    fun createQualityIssue(
        field: String = "test_field",
        issueType: com.locationsync.gateway.model.ai.QualityIssueType = com.locationsync.gateway.model.ai.QualityIssueType.INCOMPLETE_DATA,
        severity: Severity = Severity.MEDIUM,
        description: String = "Test issue description"
    ) = QualityIssue(
        field = field,
        issueType = issueType,
        severity = severity,
        description = description
    )

    /**
     * Creates standard opening hours (Mon-Fri 9-6, Sat 10-4, Sun closed)
     */
    fun standardOpeningHours() = mapOf(
        "Monday" to "09:00-18:00",
        "Tuesday" to "09:00-18:00",
        "Wednesday" to "09:00-18:00",
        "Thursday" to "09:00-18:00",
        "Friday" to "09:00-18:00",
        "Saturday" to "10:00-16:00",
        "Sunday" to "Closed"
    )

    /**
     * Creates extended opening hours (Mon-Sat 8-8, Sun 10-6)
     */
    fun extendedOpeningHours() = mapOf(
        "Monday" to "08:00-20:00",
        "Tuesday" to "08:00-20:00",
        "Wednesday" to "08:00-20:00",
        "Thursday" to "08:00-20:00",
        "Friday" to "08:00-20:00",
        "Saturday" to "08:00-20:00",
        "Sunday" to "10:00-18:00"
    )

    /**
     * Creates invalid opening hours (hours exceeding 24)
     */
    fun invalidOpeningHours() = mapOf(
        "Monday" to "25:00-26:00",
        "Tuesday" to "09:00-18:00"
    )

    /**
     * Creates restaurant categories
     */
    fun restaurantCategories() = listOf(
        "Restaurant",
        "Cafe",
        "Food & Beverage"
    )

    /**
     * Creates retail store categories
     */
    fun retailCategories() = listOf(
        "Retail Store",
        "Shopping",
        "Boutique"
    )

    /**
     * Creates a batch of test snapshots for load testing
     */
    fun createBatchSnapshots(
        count: Int = 10,
        startLocationId: Long = 5000L
    ): List<SnapshotPayload> = (0 until count).map { index ->
        createPerfectSnapshot(
            locationId = startLocationId + index,
            name = "Batch Test Location ${index + 1}",
            phone = "+25471234${index.toString().padStart(4, '0')}"
        )
    }

    /**
     * Creates test phone numbers
     */
    object PhoneNumbers {
        const val KENYA_MOBILE = "+254712345678"
        const val KENYA_LANDLINE = "+254202345678"
        const val US_MOBILE = "+14155551234"
        const val UK_MOBILE = "+447911123456"
        const val INVALID = "123" // Too short
    }

    /**
     * Creates test URLs
     */
    object Urls {
        const val VALID_HTTPS = "https://example.com"
        const val VALID_HTTP = "http://example.com"
        const val VALID_WITH_PATH = "https://example.com/locations/nairobi"
        const val INVALID = "not-a-url"
    }

    /**
     * Creates test location IDs
     */
    object LocationIds {
        const val NAIROBI_BRANCH = 1001L
        const val MOMBASA_BRANCH = 1002L
        const val KISUMU_BRANCH = 1003L
        const val TEST_BRANCH = 9999L
    }
}

/**
 * Builder for creating custom snapshot payloads
 */
class SnapshotPayloadBuilder {
    private var locationId: Long = 1001L
    private var name: String? = "Test Location"
    private var coordinates: Coordinates? = TestFixtures.nairobiCoordinates()
    private var address: Address? = TestFixtures.kenyanAddress()
    private var phone: String? = TestFixtures.PhoneNumbers.KENYA_MOBILE
    private var openingHours: Map<String, Any>? = TestFixtures.standardOpeningHours()
    private var categories: List<String>? = TestFixtures.restaurantCategories()
    private var website: String? = TestFixtures.Urls.VALID_HTTPS
    private var description: String? = "Test location description"
    private var temporaryClosure: com.locationsync.gateway.kafka.event.TemporaryClosure? = null

    fun locationId(id: Long) = apply { this.locationId = id }
    fun name(name: String?) = apply { this.name = name }
    fun coordinates(coords: Coordinates?) = apply { this.coordinates = coords }
    fun address(address: Address?) = apply { this.address = address }
    fun phone(phone: String?) = apply { this.phone = phone }
    fun openingHours(hours: Map<String, Any>?) = apply { this.openingHours = hours }
    fun categories(categories: List<String>?) = apply { this.categories = categories }
    fun website(url: String?) = apply { this.website = url }
    fun description(desc: String?) = apply { this.description = desc }
    fun temporaryClosure(closure: com.locationsync.gateway.kafka.event.TemporaryClosure?) = apply { this.temporaryClosure = closure }

    fun build() = SnapshotPayload(
        locationId = locationId,
        name = name,
        coordinates = coordinates,
        address = address,
        phone = phone,
        openingHours = openingHours,
        categories = categories,
        website = website,
        description = description,
        temporaryClosure = temporaryClosure
    )
}

/**
 * Extension function for easy builder access
 */
fun snapshotPayload(block: SnapshotPayloadBuilder.() -> Unit): SnapshotPayload =
    SnapshotPayloadBuilder().apply(block).build()
