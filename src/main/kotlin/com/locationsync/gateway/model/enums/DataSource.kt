package com.locationsync.gateway.model.enums

/**
 * External data sources for location snapshots.
 * Maps to database enum: data_source_type
 */
enum class DataSource {
    GOOGLE_MY_BUSINESS,
    POS_SYSTEM,
    ERP_SYSTEM,
    WEBHOOK;

    companion object {
        fun fromString(value: String): DataSource {
            return valueOf(value.uppercase().replace("-", "_"))
        }
    }

    fun displayName(): String = when (this) {
        GOOGLE_MY_BUSINESS -> "Google My Business"
        POS_SYSTEM -> "POS System"
        ERP_SYSTEM -> "ERP System"
        WEBHOOK -> "Webhook"
    }
}