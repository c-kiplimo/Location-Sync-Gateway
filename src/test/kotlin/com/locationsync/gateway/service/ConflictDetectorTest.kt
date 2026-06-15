package com.locationsync.gateway.service

import com.locationsync.gateway.model.LocationSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ConflictDetectorTest {

    private val detector = ConflictDetector()

    @Test
    fun `should detect conflict when values differ`() {
        val snapshot = LocationSnapshot(
            locationId = 123L,
            source = "GOOGLE_MY_BUSINESS",
            fields = mapOf("PHONE" to "+254799999999"),
            capturedAt = Instant.now()
        )

        val uberallData = mapOf("PHONE" to "+254712345678")

        val conflicts = detector.detect(snapshot, uberallData)

        assertEquals(1, conflicts.size)
        assertEquals("PHONE", conflicts[0].fieldType)
        assertEquals("+254799999999", conflicts[0].externalValue)
        assertEquals("+254712345678", conflicts[0].uberallValue)
    }

    @Test
    fun `should not detect conflict when values match`() {
        val snapshot = LocationSnapshot(
            locationId = 123L,
            source = "GOOGLE_MY_BUSINESS",
            fields = mapOf("PHONE" to "+254712345678"),
            capturedAt = Instant.now()
        )

        val uberallData = mapOf("PHONE" to "+254712345678")

        val conflicts = detector.detect(snapshot, uberallData)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `should detect multiple conflicts for different fields`() {
        val snapshot = LocationSnapshot(
            locationId = 123L,
            source = "GOOGLE_MY_BUSINESS",
            fields = mapOf(
                "PHONE" to "+254799999999",
                "OPENING_HOURS" to "Mon-Fri: 08:00-20:00",
                "WEBSITE" to "https://example.com"
            ),
            capturedAt = Instant.now()
        )

        val uberallData = mapOf(
            "PHONE" to "+254712345678",
            "OPENING_HOURS" to "Mon-Fri: 09:00-18:00",
            "WEBSITE" to "https://example.com"
        )

        val conflicts = detector.detect(snapshot, uberallData)

        assertEquals(2, conflicts.size)
        assertTrue(conflicts.any { it.fieldType == "PHONE" })
        assertTrue(conflicts.any { it.fieldType == "OPENING_HOURS" })
        assertTrue(conflicts.none { it.fieldType == "WEBSITE" })
    }

    @Test
    fun `should detect conflict when field missing in Uberall`() {
        val snapshot = LocationSnapshot(
            locationId = 123L,
            source = "GOOGLE_MY_BUSINESS",
            fields = mapOf("CATEGORIES" to "Coffee Shop"),
            capturedAt = Instant.now()
        )

        val uberallData = emptyMap<String, String>()

        val conflicts = detector.detect(snapshot, uberallData)

        assertEquals(1, conflicts.size)
        assertEquals("CATEGORIES", conflicts[0].fieldType)
        assertEquals("Coffee Shop", conflicts[0].externalValue)
        assertNull(conflicts[0].uberallValue)
    }
}