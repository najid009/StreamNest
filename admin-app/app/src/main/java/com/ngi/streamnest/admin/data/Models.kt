package com.ngi.streamnest.admin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelRow(
    val id: String,
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("stream_url") val streamUrl: String,
    val category: String = "general",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class FeaturedRow(
    val id: String,
    val title: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("action_type") val actionType: String,
    @SerialName("action_value") val actionValue: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class MatchRow(
    val id: String,
    val sport: String,
    @SerialName("external_id") val externalId: String? = null,
    val title: String,
    val league: String? = null,
    @SerialName("team_a") val teamA: String? = null,
    @SerialName("team_b") val teamB: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    val status: String = "upcoming",
    val score: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
)

@Serializable
data class ApiSettingRow(
    val provider: String,
    val enabled: Boolean = false,
    @SerialName("api_key") val apiKey: String? = null,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
    @SerialName("last_error") val lastError: String? = null,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: AuthUser,
)

/** লগইন সেশন (SharedPreferences-এ থাকে) */
data class AdminSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val email: String,
)

/** ফর্ম থেকে আসা ইনপুট */
data class ChannelInput(
    val name: String,
    val logoUrl: String?,
    val streamUrl: String,
    val category: String,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class FeaturedInput(
    val title: String,
    val imageUrl: String,
    val actionType: String,
    val actionValue: String,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class CustomEventInput(
    val sport: String,
    val title: String,
    val league: String?,
    val startTime: String?,
    val status: String,
    val score: String?,
    val channelId: String?,
)
