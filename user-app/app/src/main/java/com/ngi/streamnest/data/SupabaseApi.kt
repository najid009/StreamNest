package com.ngi.streamnest.data

import com.ngi.streamnest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Supabase PostgREST-এ শুধু read করে। ইউজার অ্যাপে কোনো service_role key নেই,
 * এবং RLS policy শুধু active চ্যানেল/ফিচার/ম্যাচ পড়তে দেয়।
 */
object SupabaseApi {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun channels(): List<Channel> =
        get("channels?select=*&is_active=eq.true&order=sort_order.asc", ListSerializer(Channel.serializer()))

    suspend fun featured(): List<FeaturedItem> =
        get("featured_items?select=*&is_active=eq.true&order=sort_order.asc", ListSerializer(FeaturedItem.serializer()))

    /** গত ১ দিন থেকে সামনের সব ইভেন্ট (পুরোনো ইভেন্ট তালিকা ভরিয়ে দেবে না) */
    suspend fun matches(): List<MatchItem> {
        val fromUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
        return get(
            "matches?select=*&or=(start_time.gte.$fromUtc,start_time.is.null)&order=start_time.asc&limit=100",
            ListSerializer(MatchItem.serializer()),
        )
    }

    private suspend fun <T> get(path: String, strategy: DeserializationStrategy<List<T>>): List<T> =
        withContext(Dispatchers.IO) {
            val base = BuildConfig.SUPABASE_URL.trimEnd('/')
            val key = BuildConfig.SUPABASE_ANON_KEY
            val request = Request.Builder()
                .url("$base/rest/v1/$path")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}")
                json.decodeFromString(strategy, body)
            }
        }
}
