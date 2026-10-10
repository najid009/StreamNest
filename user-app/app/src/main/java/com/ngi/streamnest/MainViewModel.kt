package com.ngi.streamnest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngi.streamnest.data.ContentRepository
import com.ngi.streamnest.data.HomeData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val data: HomeData? = null,
)

/** Home, Sports ও Settings একই ডেটা ব্যবহার করে, তাই Activity-scope ViewModel */
class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        // লাইভ স্কোর আপডেট রাখতে প্রতি ৬০ সেকেন্ডে নীরবে refresh
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                refresh(quiet = true)
            }
        }
    }

    fun refresh(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(loading = true) }
            val data = ContentRepository.load()
            _state.value = HomeUiState(loading = false, data = data)
        }
    }
}
