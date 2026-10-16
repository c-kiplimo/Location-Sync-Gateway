package com.locationsync.gateway.model.enums

/**
 * Severity levels for quality scores and issues.
 * Maps to database enum: severity_type
 */
enum class Severity {
    CRITICAL,  // Score < 40
    HIGH,      // Score < 60
    MEDIUM,    // Score < 80
    LOW;       // Score >= 80

    companion object {
        fun fromScore(score: Int): Severity = when {
            score < 40 -> CRITICAL
            score < 60 -> HIGH
            score < 80 -> MEDIUM
            else -> LOW
        }

        fun fromString(value: String): Severity {
            return valueOf(value.uppercase())
        }
    }

    fun getColor(): String = when (this) {
        CRITICAL -> "#D32F2F"  // Red
        HIGH -> "#F57C00"      // Orange
        MEDIUM -> "#FBC02D"    // Yellow
        LOW -> "#388E3C"       // Green
    }

    fun requiresImmediate(): Boolean = this == CRITICAL
}