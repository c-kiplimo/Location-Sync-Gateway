package com.locationsync.gateway.storage

import com.locationsync.gateway.model.Conflict
import com.locationsync.gateway.model.ResolutionDecision
import com.locationsync.gateway.model.ResolutionOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class InMemoryStorageTest {

    private lateinit var storage: InMemoryStorage

    @BeforeEach
    fun setup() {
        storage = InMemoryStorage()
    }

    @Test
    fun `should store and retrieve conflicts`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        storage.saveConflict(conflict)

        val retrieved = storage.getAllConflicts()
        assertEquals(1, retrieved.size)
        assertEquals(conflict.id, retrieved[0].id)
    }

    @Test
    fun `should filter conflicts by location ID`() {
        val conflict1 = Conflict(
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        val conflict2 = Conflict(
            locationId = 456L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        storage.saveConflict(conflict1)
        storage.saveConflict(conflict2)

        val location123Conflicts = storage.getConflictsByLocationId(123L)
        assertEquals(1, location123Conflicts.size)
        assertEquals(123L, location123Conflicts[0].locationId)
    }

    @Test
    fun `should store and retrieve decisions`() {
        val conflictId = UUID.randomUUID()
        val decision = ResolutionDecision(
            conflictId = conflictId,
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678",
            outcome = ResolutionOutcome.AUTO_REVERT,
            reason = "Test reason"
        )

        storage.saveDecision(decision)

        val retrieved = storage.getAllDecisions()
        assertEquals(1, retrieved.size)
        assertEquals(decision.id, retrieved[0].id)
    }

    @Test
    fun `should retrieve decision by conflict ID`() {
        val conflictId = UUID.randomUUID()
        val decision = ResolutionDecision(
            conflictId = conflictId,
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678",
            outcome = ResolutionOutcome.AUTO_REVERT,
            reason = "Test reason"
        )

        storage.saveDecision(decision)

        val retrieved = storage.getDecisionByConflictId(conflictId)
        assertNotNull(retrieved)
        assertEquals(conflictId, retrieved?.conflictId)
    }

    @Test
    fun `should filter conflicts by outcome`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "CATEGORIES",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "Coffee Shop",
            uberallValue = "Restaurant"
        )

        storage.saveConflict(conflict)

        val decision = ResolutionDecision(
            conflictId = conflict.id,
            locationId = 123L,
            fieldType = "CATEGORIES",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "Coffee Shop",
            uberallValue = "Restaurant",
            outcome = ResolutionOutcome.FLAG_FOR_REVIEW,
            reason = "No rule"
        )

        storage.saveDecision(decision)

        val flagged = storage.getConflictsByOutcome(ResolutionOutcome.FLAG_FOR_REVIEW)
        assertEquals(1, flagged.size)
        assertEquals(conflict.id, flagged[0].id)
    }

    @Test
    fun `clear should remove all data`() {
        val conflict = Conflict(
            locationId = 123L,
            fieldType = "PHONE",
            source = "GOOGLE_MY_BUSINESS",
            externalValue = "+254799999999",
            uberallValue = "+254712345678"
        )

        storage.saveConflict(conflict)
        storage.clear()

        assertTrue(storage.getAllConflicts().isEmpty())
        assertTrue(storage.getAllDecisions().isEmpty())
    }
}