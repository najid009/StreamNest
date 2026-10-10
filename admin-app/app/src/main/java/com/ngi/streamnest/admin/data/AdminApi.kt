package com.ngi.streamnest.admin.data

import com.ngi.streamnest.admin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// buildJsonObject-এর জন্য ছোট helper (কোটলিনের Primitive মান JsonElement-এ মোড়ানো)
private fun JsonObjectBuilder.put(key: String, value: String) {
    put(key, JsonPrimitive(value))
}

private fun JsonObjectBuilder.put(key: String, value: Boolean) {
    put(key, JsonPrimitive(value))
}

private fun JsonObjectBuilder.put(key: String, value: Int) {
    put(key, JsonPrimitive(value))
}

class ApiException(message: String) : Exception(message)

/**
 * Supabase-এর সাথে যোগাযোগ: Auth (email/password) + PostgREST (টেবিল) + Edge Function (sync).
 * সব লেখার অনুরোধে admin-এর access token যায়, তাই RLS অনুযায়ী শুধু admin লিখতে পারে।
 */
class AdminApi(private val store: SessionStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaJson = "application/json; charset=utf-8".toMediaType()

    private val base: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anon: String get() = BuildConfig.SUPABASE_ANON_KEY

    // ---------- Auth ----------

    suspend fun signIn(email: String, password: String): AdminSession = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }.toString()
        val request = Request.Builder()
            .url("$base/auth/v1/token?grant_type=password")
            .header("apikey", anon)
            .post(body.toRequestBody(mediaJson))
            .build()
        val auth = json.decodeFromString(AuthResponse.serializer(), exec(request))
        val session = AdminSession(
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            expiresAt = nowSec() + auth.expiresIn,
            email = auth.user.email ?: email,
        )
        // Login-এর পর admin role যাচাই: admins টেবিলে নিজের row থাকতে হবে
        store.save(session)
        try {
            val rows = rest("GET", "admins?select=user_id")
            if (rows.trim() == "[]") throw ApiException("এই অ্যাকাউন্টের admin অনুমতি নেই")
        } catch (e: Exception) {
            // যাচাই ব্যর্থ (অনুমতি নেই বা নেটওয়ার্ক) হলে টোকেন রাখা যাবে না
            store.clear()
            throw e
        }
        session
    }

    private fun accessToken(): String {
        val session = store.load() ?: throw ApiException("লগইন প্রয়োজন")
        if (session.expiresAt - 60 > nowSec()) return session.accessToken

        val body = buildJsonObject { put("refresh_token", session.refreshToken) }.toString()
        val request = Request.Builder()
            .url("$base/auth/v1/token?grant_type=refresh_token")
            .header("apikey", anon)
            .post(body.toRequestBody(mediaJson))
            .build()
        val text = try {
            exec(request)
        } catch (e: ApiException) {
            store.clear()
            throw ApiException("সেশন শেষ হয়েছে, আবার লগইন করুন")
        }
        val auth = json.decodeFromString(AuthResponse.serializer(), text)
        val updated = session.copy(
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            expiresAt = nowSec() + auth.expiresIn,
        )
        store.save(updated)
        return updated.accessToken
    }

    // ---------- Low level ----------

    suspend fun rest(method: String, path: String, body: String? = null, prefer: String? = null): String =
        withContext(Dispatchers.IO) {
            val token = accessToken()
            val requestBody = (body ?: "").toRequestBody(mediaJson)
            val builder = Request.Builder()
                .url("$base/rest/v1/$path")
                .header("apikey", anon)
                .header("Authorization", "Bearer $token")
            if (prefer != null) builder.header("Prefer", prefer)
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(requestBody)
                "PATCH" -> builder.patch(requestBody)
                "DELETE" -> builder.delete()
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }
            exec(builder.build())
        }

    private fun exec(request: Request): String =
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(readableError(text, response.code))
            text
        }

    private fun readableError(text: String, code: Int): String {
        val message = runCatching {
            val obj = json.parseToJsonElement(text).jsonObject
            listOf("msg", "message", "error_description", "error").firstNotNullOfOrNull { key ->
                obj[key]?.jsonPrimitive?.contentOrNull
            }
        }.getOrNull()
        return message ?: "সার্ভার ত্রুটি (HTTP $code)"
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private inline fun <reified T> decode(text: String): List<T> = json.decodeFromString(text)

    private fun JsonObjectBuilder.nullableString(key: String, value: String?) {
        put(key, if (value == null) JsonNull else JsonPrimitive(value))
    }

    // ---------- Read ----------

    suspend fun channels(): List<ChannelRow> =
        decode(rest("GET", "channels?select=*&order=sort_order.asc"))

    suspend fun featured(): List<FeaturedRow> =
        decode(rest("GET", "featured_items?select=*&order=sort_order.asc"))

    suspend fun matches(): List<MatchRow> =
        decode(rest("GET", "matches?select=*&order=start_time.asc&limit=300"))

    suspend fun apiSettings(): List<ApiSettingRow> =
        decode(rest("GET", "api_settings?select=*"))

    // ---------- Channels ----------

    suspend fun saveChannel(id: String?, input: ChannelInput) {
        val body = buildJsonObject {
            put("name", input.name)
            nullableString("logo_url", input.logoUrl)
            put("stream_url", input.streamUrl)
            put("category", input.category)
            put("is_active", input.isActive)
            put("sort_order", input.sortOrder)
        }.toString()
        if (id == null) {
            rest("POST", "channels", body, "return=minimal")
        } else {
            rest("PATCH", "channels?id=eq.$id", body, "return=minimal")
        }
    }

    suspend fun setChannelActive(id: String, active: Boolean) {
        val body = buildJsonObject { put("is_active", active) }.toString()
        rest("PATCH", "channels?id=eq.$id", body, "return=minimal")
    }

    suspend fun deleteChannel(id: String) {
        rest("DELETE", "channels?id=eq.$id")
    }

    // ---------- Featured ----------

    suspend fun saveFeatured(id: String?, input: FeaturedInput) {
        val body = buildJsonObject {
            put("title", input.title)
            put("image_url", input.imageUrl)
            put("action_type", input.actionType)
            put("action_value", input.actionValue)
            put("is_active", input.isActive)
            put("sort_order", input.sortOrder)
        }.toString()
        if (id == null) {
            rest("POST", "featured_items", body, "return=minimal")
        } else {
            rest("PATCH", "featured_items?id=eq.$id", body, "return=minimal")
        }
    }

    suspend fun setFeaturedActive(id: String, active: Boolean) {
        val body = buildJsonObject { put("is_active", active) }.toString()
        rest("PATCH", "featured_items?id=eq.$id", body, "return=minimal")
    }

    suspend fun deleteFeatured(id: String) {
        rest("DELETE", "featured_items?id=eq.$id")
    }

    // ---------- Events (matches) ----------

    suspend fun updateEvent(id: String, channelId: String?, status: String, score: String?) {
        val body = buildJsonObject {
            nullableString("channel_id", channelId)
            put("status", status)
            nullableString("score", score)
        }.toString()
        rest("PATCH", "matches?id=eq.$id", body, "return=minimal")
    }

    suspend fun createEvent(input: CustomEventInput) {
        val body = buildJsonObject {
            put("sport", input.sport)
            put("title", input.title)
            nullableString("league", input.league)
            nullableString("start_time", input.startTime)
            put("status", input.status)
            nullableString("score", input.score)
            nullableString("channel_id", input.channelId)
        }.toString()
        rest("POST", "matches", body, "return=minimal")
    }

    suspend fun deleteEvent(id: String) {
        rest("DELETE", "matches?id=eq.$id")
    }

    // ---------- API setup ----------

    suspend fun saveApiSetting(provider: String, enabled: Boolean, apiKey: String?) {
        val body = buildJsonObject {
            put("provider", provider)
            put("enabled", enabled)
            // key ফাঁকা থাকলে আগের key-ই থাকবে (payload-এ না পাঠালে আপডেট হয় না)
            if (!apiKey.isNullOrBlank()) put("api_key", apiKey.trim())
        }.toString()
        rest("POST", "api_settings", body, "resolution=merge-duplicates,return=minimal")
    }

    /** Edge Function `sync-sports` চালায় (admin JWT দিয়ে) */
    suspend fun syncNow(): String = withContext(Dispatchers.IO) {
        val token = accessToken()
        val request = Request.Builder()
            .url("$base/functions/v1/sync-sports")
            .header("apikey", anon)
            .header("Authorization", "Bearer $token")
            .post("{}".toRequestBody(mediaJson))
            .build()
        exec(request)
    }
}
