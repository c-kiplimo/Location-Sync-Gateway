package com.locationsync.gateway.util

import com.locationsync.gateway.kafka.event.Address
import com.locationsync.gateway.kafka.event.Coordinates
import com.locationsync.gateway.kafka.event.LocationSnapshotEvent
import com.locationsync.gateway.kafka.event.SnapshotPayload
import com.locationsync.gateway.model.enums.DataSource
import jakarta.inject.Singleton
import java.time.Instant
import kotlin.random.Random

/**
 * Data simulator for generating realistic location snapshots for testing and ML training
 *
 * Generates diverse, realistic data covering various scenarios:
 * - High quality data (70%)
 * - Medium quality data (20%)
 * - Low quality data (10%)
 * - Various geographic locations
 * - Different business types
 * - Edge cases and anomalies
 */
@Singleton
class DataSimulator {

    private val random = Random.Default

    /**
     * Generates a batch of location snapshots with varied quality
     */
    fun generateBatch(
        count: Int,
        source: DataSource = DataSource.GOOGLE_MY_BUSINESS,
        startLocationId: Long = 10000L
    ): List<LocationSnapshotEvent> {
        return (0 until count).map { index ->
            val locationId = startLocationId + index
            val qualityTier = random.nextDouble()

            val payload = when {
                qualityTier < 0.70 -> generateHighQualitySnapshot(locationId)
                qualityTier < 0.90 -> generateMediumQualitySnapshot(locationId)
                else -> generateLowQualitySnapshot(locationId)
            }

            LocationSnapshotEvent(
                source = source,
                payload = payload,
                timestamp = Instant.now().minusSeconds(random.nextLong(0, 86400))
            )
        }
    }

    /**
     * Generates a high-quality snapshot (90-100 quality score)
     */
    fun generateHighQualitySnapshot(locationId: Long): SnapshotPayload {
        val city = cities.random()
        val businessType = businessTypes.random()

        return SnapshotPayload(
            locationId = locationId,
            name = generateBusinessName(businessType, city.name),
            coordinates = Coordinates(
                latitude = city.coords.latitude + random.nextDouble(-0.1, 0.1),
                longitude = city.coords.longitude + random.nextDouble(-0.1, 0.1)
            ),
            address = Address(
                street = generateStreetAddress(),
                city = city.name,
                state = city.state,
                postalCode = generatePostalCode(city.country),
                country = city.country
            ),
            phone = generatePhoneNumber(city.countryCode),
            openingHours = generateOpeningHours(),
            categories = businessType.categories,
            website = generateWebsite(locationId),
            description = generateDescription(businessType.name, city.name),
            temporaryClosure = null
        )
    }

    /**
     * Generates a medium-quality snapshot (60-89 quality score)
     * Missing some optional fields
     */
    fun generateMediumQualitySnapshot(locationId: Long): SnapshotPayload {
        val city = cities.random()
        val businessType = businessTypes.random()

        return SnapshotPayload(
            locationId = locationId,
            name = generateBusinessName(businessType, city.name),
            coordinates = Coordinates(
                latitude = city.coords.latitude + random.nextDouble(-0.1, 0.1),
                longitude = city.coords.longitude + random.nextDouble(-0.1, 0.1)
            ),
            address = Address(
                street = generateStreetAddress(),
                city = city.name,
                state = if (random.nextBoolean()) city.state else null,
                postalCode = if (random.nextBoolean()) generatePostalCode(city.country) else null,
                country = city.country
            ),
            phone = if (random.nextBoolean()) generatePhoneNumber(city.countryCode) else null,
            openingHours = if (random.nextBoolean()) generateOpeningHours() else null,
            categories = if (random.nextBoolean()) businessType.categories else emptyList(),
            website = if (random.nextBoolean()) generateWebsite(locationId) else null,
            description = if (random.nextBoolean()) generateDescription(businessType.name, city.name) else null,
            temporaryClosure = null
        )
    }

    /**
     * Generates a low-quality snapshot (< 60 quality score)
     * Missing critical fields or has suspicious data
     */
    fun generateLowQualitySnapshot(locationId: Long): SnapshotPayload {
        val anomalyType = random.nextInt(5)

        return when (anomalyType) {
            0 -> generateNullIslandSnapshot(locationId)
            1 -> generateMissingCriticalFieldsSnapshot(locationId)
            2 -> generateLowPrecisionSnapshot(locationId)
            3 -> generateIncompleteAddressSnapshot(locationId)
            else -> generateInvalidHoursSnapshot(locationId)
        }
    }

    /**
     * Generates snapshot with temporary closure
     */
    fun generateTemporaryClosureSnapshot(locationId: Long): SnapshotPayload {
        val baseSnapshot = generateHighQualitySnapshot(locationId)
        val durationDays = random.nextInt(1, 30)

        return baseSnapshot.copy(
            temporaryClosure = com.locationsync.gateway.kafka.event.TemporaryClosure(
                closed = true,
                reason = closureReasons.random(),
                startDate = Instant.now().toString(),
                endDate = Instant.now().plusSeconds((durationDays * 86400).toLong()).toString()
            )
        )
    }

    /**
     * Generates a conflicting snapshot (different from Uberall baseline)
     */
    fun generateConflictSnapshot(
        baseSnapshot: SnapshotPayload,
        conflictType: ConflictType
    ): SnapshotPayload {
        return when (conflictType) {
            ConflictType.PHONE -> baseSnapshot.copy(
                phone = generatePhoneNumber("+254")
            )
            ConflictType.HOURS -> baseSnapshot.copy(
                openingHours = generateOpeningHours()
            )
            ConflictType.COORDINATES -> baseSnapshot.copy(
                coordinates = Coordinates(
                    latitude = baseSnapshot.coordinates!!.latitude + random.nextDouble(-0.5, 0.5),
                    longitude = baseSnapshot.coordinates.longitude + random.nextDouble(-0.5, 0.5)
                )
            )
            ConflictType.CATEGORIES -> baseSnapshot.copy(
                categories = businessTypes.random().categories
            )
            ConflictType.ADDRESS -> baseSnapshot.copy(
                address = baseSnapshot.address?.copy(
                    street = generateStreetAddress()
                )
            )
        }
    }

    // Private helper methods

    private fun generateNullIslandSnapshot(locationId: Long) = SnapshotPayload(
        locationId = locationId,
        name = "Null Island Location",
        coordinates = Coordinates(0.0, 0.0), // Suspicious
        address = null,
        phone = null,
        openingHours = null,
        categories = null,
        website = null,
        description = null,
        temporaryClosure = null
    )

    private fun generateMissingCriticalFieldsSnapshot(locationId: Long) = SnapshotPayload(
        locationId = locationId,
        name = null, // Missing critical field
        coordinates = null, // Missing critical field
        address = null,
        phone = null,
        openingHours = null,
        categories = null,
        website = null,
        description = null,
        temporaryClosure = null
    )

    private fun generateLowPrecisionSnapshot(locationId: Long): SnapshotPayload {
        val city = cities.random()
        return SnapshotPayload(
            locationId = locationId,
            name = "Low Precision Location",
            coordinates = Coordinates(
                latitude = city.coords.latitude.toInt().toDouble(), // Only integer precision
                longitude = city.coords.longitude.toInt().toDouble()
            ),
            address = Address(
                street = "123", // Too short
                city = city.name,
                state = null,
                postalCode = null,
                country = city.country
            ),
            phone = "+254", // Incomplete
            openingHours = null,
            categories = null,
            website = null,
            description = null,
            temporaryClosure = null
        )
    }

    private fun generateIncompleteAddressSnapshot(locationId: Long) = SnapshotPayload(
        locationId = locationId,
        name = "Incomplete Address Location",
        coordinates = Coordinates(-1.286389, 36.817223),
        address = Address(
            street = generateStreetAddress(),
            city = null, // Missing critical field
            state = null,
            postalCode = null,
            country = null // Missing critical field
        ),
        phone = generatePhoneNumber("+254"),
        openingHours = null,
        categories = null,
        website = null,
        description = null,
        temporaryClosure = null
    )

    private fun generateInvalidHoursSnapshot(locationId: Long): SnapshotPayload {
        val city = cities.random()
        return SnapshotPayload(
            locationId = locationId,
            name = "Invalid Hours Location",
            coordinates = Coordinates(city.coords.latitude, city.coords.longitude),
            address = Address(
                street = generateStreetAddress(),
                city = city.name,
                state = city.state,
                postalCode = generatePostalCode(city.country),
                country = city.country
            ),
            phone = generatePhoneNumber(city.countryCode),
            openingHours = mapOf(
                "Monday" to "25:00-26:00", // Invalid hours
                "Tuesday" to "09:00-18:00"
            ),
            categories = businessTypes.random().categories,
            website = null,
            description = null,
            temporaryClosure = null
        )
    }

    private fun generateBusinessName(businessType: BusinessType, city: String): String {
        val prefixes = listOf("The", "Royal", "Golden", "Silver", "Prime", "Elite", "Central", "Grand")
        val suffixes = listOf("Plaza", "Hub", "Corner", "Spot", "Place", "Center", "House", "Station")

        return when (random.nextInt(3)) {
            0 -> "${prefixes.random()} ${businessType.name}"
            1 -> "${city} ${businessType.name}"
            else -> "${businessType.name} ${suffixes.random()}"
        }
    }

    private fun generateStreetAddress(): String {
        val streetNumbers = (1..999).random()
        val streetNames = listOf(
            "Kimathi Street", "Moi Avenue", "Kenyatta Avenue", "Uhuru Highway",
            "Main Street", "Market Street", "Oak Avenue", "Park Road",
            "High Street", "Station Road", "Church Lane", "Mill Street"
        )
        return "$streetNumbers ${streetNames.random()}"
    }

    private fun generatePhoneNumber(countryCode: String): String {
        val digits = (100000000..999999999).random()
        return "$countryCode$digits"
    }

    private fun generatePostalCode(country: String): String {
        return when (country) {
            "Kenya" -> (10000..99999).random().toString().padStart(5, '0')
            "United States" -> (10000..99999).random().toString()
            "United Kingdom" -> "SW${(1..99).random()}A ${(1..9).random()}AA"
            else -> (10000..99999).random().toString()
        }
    }

    private fun generateOpeningHours(): Map<String, String> {
        val openHour = (7..10).random()
        val closeHour = (17..21).random()

        return mapOf(
            "Monday" to "${openHour.toString().padStart(2, '0')}:00-${closeHour.toString().padStart(2, '0')}:00",
            "Tuesday" to "${openHour.toString().padStart(2, '0')}:00-${closeHour.toString().padStart(2, '0')}:00",
            "Wednesday" to "${openHour.toString().padStart(2, '0')}:00-${closeHour.toString().padStart(2, '0')}:00",
            "Thursday" to "${openHour.toString().padStart(2, '0')}:00-${closeHour.toString().padStart(2, '0')}:00",
            "Friday" to "${openHour.toString().padStart(2, '0')}:00-${closeHour.toString().padStart(2, '0')}:00",
            "Saturday" to "${(openHour + 1).toString().padStart(2, '0')}:00-${(closeHour - 2).toString().padStart(2, '0')}:00",
            "Sunday" to if (random.nextBoolean()) "Closed" else "${(openHour + 2).toString().padStart(2, '0')}:00-${(closeHour - 4).toString().padStart(2, '0')}:00"
        )
    }

    private fun generateWebsite(locationId: Long): String {
        val tlds = listOf("com", "co.ke", "org", "net")
        return "https://location-${locationId}.${tlds.random()}"
    }

    private fun generateDescription(businessType: String, city: String): String {
        val templates = listOf(
            "Welcome to $city's premier $businessType. We offer exceptional service and quality products.",
            "Your go-to $businessType in $city. Visit us for an unforgettable experience.",
            "Established $businessType serving $city since ${(1990..2020).random()}. Quality you can trust.",
            "Experience the best $businessType has to offer in the heart of $city."
        )
        return templates.random()
    }

    // Data structures

    data class City(
        val name: String,
        val coords: Coordinates,
        val state: String,
        val country: String,
        val countryCode: String
    )

    data class BusinessType(
        val name: String,
        val categories: List<String>
    )

    enum class ConflictType {
        PHONE, HOURS, COORDINATES, CATEGORIES, ADDRESS
    }

    companion object {
        private val cities = listOf(
            City("Nairobi", Coordinates(-1.286389, 36.817223), "Nairobi County", "Kenya", "+254"),
            City("Mombasa", Coordinates(-4.043477, 39.668206), "Mombasa County", "Kenya", "+254"),
            City("Kisumu", Coordinates(-0.091702, 34.767956), "Kisumu County", "Kenya", "+254"),
            City("New York", Coordinates(40.712776, -74.005974), "New York", "United States", "+1"),
            City("London", Coordinates(51.507351, -0.127758), "Greater London", "United Kingdom", "+44"),
            City("Paris", Coordinates(48.856614, 2.352222), "Île-de-France", "France", "+33")
        )

        private val businessTypes = listOf(
            BusinessType("Restaurant", listOf("Restaurant", "Food & Dining", "Eatery")),
            BusinessType("Cafe", listOf("Cafe", "Coffee Shop", "Bakery")),
            BusinessType("Retail Store", listOf("Retail", "Shopping", "Store")),
            BusinessType("Bank", listOf("Bank", "Financial Services", "ATM")),
            BusinessType("Pharmacy", listOf("Pharmacy", "Drugstore", "Health")),
            BusinessType("Gym", listOf("Gym", "Fitness Center", "Sports")),
            BusinessType("Hotel", listOf("Hotel", "Accommodation", "Lodging")),
            BusinessType("Supermarket", listOf("Supermarket", "Grocery", "Food Store"))
        )

        private val closureReasons = listOf(
            "Renovation",
            "Maintenance",
            "Staff Training",
            "Inventory Update",
            "Deep Cleaning",
            "Equipment Upgrade",
            "Holiday Closure"
        )
    }
}
