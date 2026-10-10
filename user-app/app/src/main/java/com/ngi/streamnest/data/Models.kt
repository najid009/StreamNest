package com.ngi.streamnest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase `channels` টেবিলের সারি */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("stream_url") val streamUrl: String,
    val category: String = "general",
    @SerialName("sort_order") val sortOrder: Int = 0,
)

/**
 * Supabase `featured_items` টেবিলের সারি (Home-এর ব্যানার/বিজ্ঞাপন)
 * actionType: "url" -> বাইরের লিংক (ad redirect), "channel" -> চ্যানেল চালু, "match" -> ইভেন্ট চালু
 */
@Serializable
data class FeaturedItem(
    val id: String,
    val title: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("action_type") val actionType: String,
    @SerialName("action_value") val actionValue: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

/** Supabase `matches` টেবিলের সারি (football / cricket ইভেন্ট) */
@Serializable
data class MatchItem(
    val id: String,
    val sport: String,
    val title: String,
    val league: String? = null,
    @SerialName("team_a") val teamA: String? = null,
    @SerialName("team_b") val teamB: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    val status: String = "upcoming",
    val score: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
)
