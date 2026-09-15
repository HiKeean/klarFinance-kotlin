package com.klarfinance.app.core.network

import kotlinx.serialization.Serializable

/** Mirrors the backend's ApiResponse<T> envelope (same shape every endpoint returns). */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val statusCode: Int? = null,
    val message: String? = null,
    val data: T? = null,
)
