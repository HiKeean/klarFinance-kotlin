package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/**
 * Nested parent fields (province/regencies/district) are ignored on purpose - the app
 * only needs id + name at each cascade level - relying on ignoreUnknownKeys in the
 * shared Json instance.
 */
@Serializable
data class LocationItemDto(
    val id: Long,
    val name: String,
)
