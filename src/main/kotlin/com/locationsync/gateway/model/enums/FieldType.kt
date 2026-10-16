package com.locationsync.gateway.model.enums

/**
 * Location data field types that can have conflicts.
 * Maps to database enum: field_type_enum
 */
enum class FieldType {
    PHONE,
    OPENING_HOURS,
    COORDINATES,
    CATEGORIES,
    ADDRESS,
    TEMPORARY_CLOSURE,
    WEBSITE,
    DESCRIPTION;

    companion object {
        fun fromString(value: String): FieldType {
            return valueOf(value.uppercase().replace("-", "_"))
        }
    }

    fun displayName(): String = when (this) {
        PHONE -> "Phone Number"
        OPENING_HOURS -> "Opening Hours"
        COORDINATES -> "Coordinates"
        CATEGORIES -> "Categories"
        ADDRESS -> "Address"
        TEMPORARY_CLOSURE -> "Temporary Closure"
        WEBSITE -> "Website"
        DESCRIPTION -> "Description"
    }
}