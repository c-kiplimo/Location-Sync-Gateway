package com.locationsync.gateway.service

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.ResolutionOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResolutionEngineTest {

    private val engine = ResolutionEngine()

    @Test
    fun `phone from Google should AUTO_REVERT`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        val decision = engine.resolve(conflict)

        assertEquals(ResolutionOutcome.AUTO_REVERT, decision.outcome)
        assertEquals("google-phone-hq-override", decision.ruleApplied)
        assertTrue(decision.reason.contains("centrally managed"))
    }

    @Test
    fun `opening hours from Google should AUTO_ACCEPT`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "OPENING_HOURS",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "Mon-Fri: 08:00-20:00",
            uberallValue = "Mon-Fri: 09:00-18:00"
        )

        val decision = engine.resolve(conflict)

        assertEquals(ResolutionOutcome.AUTO_ACCEPT, decision.outcome)
        assertEquals("google-opening-hours-franchisee", decision.ruleApplied)
        assertTrue(decision.reason.contains("franchisees"))
    }

    @Test
    fun `unknown field should FLAG_FOR_REVIEW`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "CATEGORIES",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "Coffee Shop",
            uberallValue = "Restaurant"
        )

        val decision = engine.resolve(conflict)

        assertEquals(ResolutionOutcome.FLAG_FOR_REVIEW, decision.outcome)
        assertNull(decision.ruleApplied)
        assertTrue(decision.reason.contains("No automatic resolution"))
    }

    @Test
    fun `decision should contain all conflict details`() {
        val conflict = Conflict(
            locationId = 456L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        val decision = engine.resolve(conflict)

        assertEquals(conflict.id, decision.conflictId)
        assertEquals(conflict.locationId, decision.locationId)
        assertEquals(conflict.fieldType, decision.fieldType)
        assertEquals(conflict.source, decision.source)
        assertEquals(conflict.externalValue, decision.externalValue)
        assertEquals(conflict.uberallValue, decision.uberallValue)
    }
}