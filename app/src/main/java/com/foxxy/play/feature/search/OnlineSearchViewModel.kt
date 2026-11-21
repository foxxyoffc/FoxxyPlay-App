package com.foxxy.play.feature.search

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foxxy.play.core.network.NetworkStatus
import com.foxxy.play.data.remote.musicapi.MusicApiRepository
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnlineSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicApiRepository()
    private val connectivityManager =
        application.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(NetworkStatus.isOnline(application))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var lastQuery = ""

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshConnectionState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            refreshConnectionState()
        }

        override fun onLost(network: Network) {
            refreshConnectionState()
        }
    }

    private val _results = MutableStateFlow<List<OnlineMusicTrack>>(emptyList())
    val results: StateFlow<List<OnlineMusicTrack>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searched = MutableStateFlow(false)
    val searched: StateFlow<Boolean> = _searched.asStateFlow()

    private val _discoverResults = MutableStateFlow<List<OnlineMusicTrack>>(emptyList())
    val discoverResults: StateFlow<List<OnlineMusicTrack>> =
        _discoverResults.asStateFlow()

    private val _discoverLoading = MutableStateFlow(false)
    val discoverLoading: StateFlow<Boolean> = _discoverLoading.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError: StateFlow<String?> = _discoverError.asStateFlow()

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }

        if (!_isOnline.value) {
            _discoverError.value = NetworkStatus.OFFLINE_MESSAGE
        }
    }

    private fun refreshConnectionState() {
        val online = NetworkStatus.isOnline(getApplication())
        val wasOnline = _isOnline.value
        _isOnline.value = online

        if (!online) {
            _discoverError.value = NetworkStatus.OFFLINE_MESSAGE
            if (_searched.value) {
                _error.value = NetworkStatus.OFFLINE_MESSAGE
            }
            return
        }

        if (!wasOnline) {
            viewModelScope.launch {
                delay(1200)

                if (!NetworkStatus.isOnline(getApplication())) return@launch

                if (_discoverResults.value.isEmpty() ||
                    _discoverError.value == NetworkStatus.OFFLINE_MESSAGE
                ) {
                    loadDiscover(forceRefresh = true)
                }

                if (lastQuery.isNotBlank() &&
                    _searched.value &&
                    (_results.value.isEmpty() ||
                        _error.value == NetworkStatus.OFFLINE_MESSAGE)
                ) {
                    search(lastQuery)
                }
            }
        }
    }

    fun loadDiscover(forceRefresh: Boolean = false) {
        if (_discoverLoading.value) return

        if (!NetworkStatus.isOnline(getApplication())) {
            _discoverError.value = NetworkStatus.OFFLINE_MESSAGE
            return
        }

        if (!forceRefresh && _discoverResults.value.isNotEmpty()) return

        viewModelScope.launch {
            _discoverLoading.value = true
            _discoverError.value = null

            try {
                val queries = listOf(
                    "pop hits",
                    "viral songs",
                    "global hits",
                    "indonesia hits",
                    "k-pop hits",
                    "chill hits",
                    "new music",
                    "top songs"
                ).shuffled().take(3)

                val batches = queries.map { query ->
                    repository.search(
                        query = query,
                        sources = listOf("spotify")
                    )
                }

                val combined = batches
                    .flatMap { it.getOrDefault(emptyList()) }
                    .distinctBy { it.id }
                    .shuffled()

                _discoverResults.value = combined

                if (combined.isEmpty()) {
                    _discoverError.value =
                        if (!NetworkStatus.isOnline(getApplication())) {
                            NetworkStatus.OFFLINE_MESSAGE
                        } else {
                            batches.firstNotNullOfOrNull {
                                it.exceptionOrNull()?.message
                            } ?: "Rekomendasi belum tersedia. Coba refresh."
                        }
                }
            } catch (error: Exception) {
                _discoverError.value =
                    if (!NetworkStatus.isOnline(getApplication())) {
                        NetworkStatus.OFFLINE_MESSAGE
                    } else {
                        error.localizedMessage ?: "Gagal memuat rekomendasi."
                    }
            } finally {
                _discoverLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            _error.value = "Masukkan judul atau nama artis."
            return
        }

        lastQuery = query
        _searched.value = true

        if (!NetworkStatus.isOnline(getApplication())) {
            _results.value = emptyList()
            _error.value = NetworkStatus.OFFLINE_MESSAGE
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                repository.search(
                    query = query,
                    sources = listOf("spotify")
                ).onSuccess {
                    _results.value = it
                    _error.value =
                        if (it.isEmpty()) "Tidak ada lagu yang ditemukan." else null
                }.onFailure {
                    _results.value = emptyList()
                    _error.value =
                        if (!NetworkStatus.isOnline(getApplication())) {
                            NetworkStatus.OFFLINE_MESSAGE
                        } else {
                            it.localizedMessage ?: "Pencarian gagal."
                        }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    override fun onCleared() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        super.onCleared()
    }
}
