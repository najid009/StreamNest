package com.ngi.streamnest.admin.data

import android.content.Context

/** লগইন টোকেন সংরক্ষণ। শুধু এই অ্যাপের private storage-এ থাকে। */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)

    fun load(): AdminSession? {
        val access = prefs.getString("access", null) ?: return null
        val refresh = prefs.getString("refresh", null) ?: return null
        return AdminSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAt = prefs.getLong("expires_at", 0L),
            email = prefs.getString("email", "").orEmpty(),
        )
    }

    fun save(session: AdminSession) {
        prefs.edit()
            .putString("access", session.accessToken)
            .putString("refresh", session.refreshToken)
            .putLong("expires_at", session.expiresAt)
            .putString("email", session.email)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
