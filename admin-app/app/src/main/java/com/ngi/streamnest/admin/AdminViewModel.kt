package com.ngi.streamnest.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngi.streamnest.admin.data.AdminApi
import com.ngi.streamnest.admin.data.AdminSession
import com.ngi.streamnest.admin.data.ApiException
import com.ngi.streamnest.admin.data.ApiSettingRow
import com.ngi.streamnest.admin.data.ChannelInput
import com.ngi.streamnest.admin.data.ChannelRow
import com.ngi.streamnest.admin.data.CustomEventInput
import com.ngi.streamnest.admin.data.FeaturedInput
import com.ngi.streamnest.admin.data.FeaturedRow
import com.ngi.streamnest.admin.data.MatchRow
import com.ngi.streamnest.admin.data.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Admin-এর সব ডেটা ও কাজ এখানে। প্রতিটি লেখার কাজ শেষে সংশ্লিষ্ট লিস্ট আবার লোড হয়।
 */
class AdminViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    private val api = AdminApi(store)

    private val _session = MutableStateFlow(store.load())
    val session: StateFlow<AdminSession?> = _session.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelRow>>(emptyList())
    val channels: StateFlow<List<ChannelRow>> = _channels.asStateFlow()

    private val _featured = MutableStateFlow<List<FeaturedRow>>(emptyList())
    val featured: StateFlow<List<FeaturedRow>> = _featured.asStateFlow()

    private val _matches = MutableStateFlow<List<MatchRow>>(emptyList())
    val matches: StateFlow<List<MatchRow>> = _matches.asStateFlow()

    private val _apiSettings = MutableStateFlow<Map<String, ApiSettingRow>>(emptyMap())
    val apiSettings: StateFlow<Map<String, ApiSettingRow>> = _apiSettings.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        if (_session.value != null) {
            launchTask { loadAll() }
        }
    }

    // ---------- Core helpers ----------

    private fun launchTask(onDone: (() -> Unit)? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
                onDone?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (store.load() == null) _session.value = null
                _messages.emit(e.message ?: "কিছু একটা ভুল হয়েছে")
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun loadAll() {
        _channels.value = api.channels()
        _featured.value = api.featured()
        _matches.value = api.matches()
        _apiSettings.value = api.apiSettings().associateBy { it.provider }
    }

    fun refreshAll(onDone: (() -> Unit)? = null) = launchTask(onDone) { loadAll() }

    // ---------- Auth ----------

    fun signIn(email: String, password: String, onDone: () -> Unit = {}) {
        launchTask(onDone) {
            _session.value = api.signIn(email, password)
            loadAll()
        }
    }

    fun signOut() {
        store.clear()
        _session.value = null
        _channels.value = emptyList()
        _featured.value = emptyList()
        _matches.value = emptyList()
        _apiSettings.value = emptyMap()
    }

    // ---------- Channels ----------

    fun saveChannel(id: String?, input: ChannelInput, onDone: () -> Unit) = launchTask(onDone) {
        api.saveChannel(id, input)
        _channels.value = api.channels()
        _messages.emit("চ্যানেল সংরক্ষিত হয়েছে")
    }

    fun toggleChannel(channel: ChannelRow) = launchTask {
        api.setChannelActive(channel.id, !channel.isActive)
        _channels.value = api.channels()
    }

    fun deleteChannel(id: String, onDone: () -> Unit) = launchTask(onDone) {
        api.deleteChannel(id)
        _channels.value = api.channels()
        _messages.emit("চ্যানেল মুছে ফেলা হয়েছে")
    }

    // ---------- Featured ----------

    fun saveFeatured(id: String?, input: FeaturedInput, onDone: () -> Unit) = launchTask(onDone) {
        api.saveFeatured(id, input)
        _featured.value = api.featured()
        _messages.emit("ফিচার্ড আইটেম সংরক্ষিত হয়েছে")
    }

    fun toggleFeatured(item: FeaturedRow) = launchTask {
        api.setFeaturedActive(item.id, !item.isActive)
        _featured.value = api.featured()
    }

    fun deleteFeatured(id: String, onDone: () -> Unit) = launchTask(onDone) {
        api.deleteFeatured(id)
        _featured.value = api.featured()
        _messages.emit("ফিচার্ড আইটেম মুছে ফেলা হয়েছে")
    }

    // ---------- Events ----------

    fun updateEvent(
        match: MatchRow,
        channelId: String?,
        status: String,
        score: String?,
        onDone: () -> Unit,
    ) = launchTask(onDone) {
        api.updateEvent(match.id, channelId, status, score)
        _matches.value = api.matches()
        _messages.emit("ইভেন্ট আপডেট হয়েছে")
    }

    fun createEvent(input: CustomEventInput, onDone: () -> Unit) = launchTask(onDone) {
        api.createEvent(input)
        _matches.value = api.matches()
        _messages.emit("ইভেন্ট যোগ হয়েছে")
    }

    fun deleteEvent(id: String, onDone: () -> Unit) = launchTask(onDone) {
        api.deleteEvent(id)
        _matches.value = api.matches()
        _messages.emit("ইভেন্ট মুছে ফেলা হয়েছে")
    }

    // ---------- API setup & sync ----------

    fun saveApiSettings(
        cricketEnabled: Boolean,
        cricketKey: String,
        footballEnabled: Boolean,
        footballKey: String,
    ) = launchTask {
        api.saveApiSetting("cricket", cricketEnabled, cricketKey)
        api.saveApiSetting("football", footballEnabled, footballKey)
        _apiSettings.value = api.apiSettings().associateBy { it.provider }
        _messages.emit("API সেটিংস সংরক্ষিত হয়েছে")
    }

    fun syncNow() = launchTask {
        val result = api.syncNow()
        _apiSettings.value = api.apiSettings().associateBy { it.provider }
        _matches.value = api.matches()
        _messages.emit("সিঙ্ক সম্পন্ন: ${result.take(160)}")
    }
}
