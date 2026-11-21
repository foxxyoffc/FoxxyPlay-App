from pathlib import Path
import shutil

root = Path("app/src/main/java/com/foxxy/play/feature")

def backup(path):
    p = Path(path)
    if not p.exists():
        raise FileNotFoundError(f"File tidak ditemukan: {p}")
    shutil.copy2(p, str(p) + ".before-discover")
    return p

# =========================================================
# ONLINE SEARCH VIEWMODEL
# =========================================================

vm_path = root / "search/OnlineSearchViewModel.kt"
backup(vm_path)

vm_path.write_text(r'''package com.foxxy.play.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxxy.play.data.remote.musicapi.MusicApiRepository
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnlineSearchViewModel : ViewModel() {
    private val repository = MusicApiRepository()

    private val _results = MutableStateFlow<List<OnlineMusicTrack>>(emptyList())
    val results: StateFlow<List<OnlineMusicTrack>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searched = MutableStateFlow(false)
    val searched: StateFlow<Boolean> = _searched.asStateFlow()

    private val _discoverResults =
        MutableStateFlow<List<OnlineMusicTrack>>(emptyList())
    val discoverResults: StateFlow<List<OnlineMusicTrack>> =
        _discoverResults.asStateFlow()

    private val _discoverLoading = MutableStateFlow(false)
    val discoverLoading: StateFlow<Boolean> = _discoverLoading.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError: StateFlow<String?> = _discoverError.asStateFlow()

    fun loadDiscover(forceRefresh: Boolean = false) {
        if (_discoverLoading.value) return
        if (!forceRefresh && _discoverResults.value.isNotEmpty()) return

        viewModelScope.launch {
            _discoverLoading.value = true
            _discoverError.value = null

            try {
                // Kata kunci dipilih secara acak agar rekomendasi bervariasi.
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
                        batches.firstNotNullOfOrNull {
                            it.exceptionOrNull()?.message
                        } ?: "Rekomendasi belum tersedia. Coba refresh."
                }
            } catch (e: Exception) {
                _discoverError.value =
                    e.message ?: "Gagal memuat rekomendasi."
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

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _searched.value = true

            try {
                repository.search(
                    query = query,
                    sources = listOf("spotify")
                ).onSuccess {
                    _results.value = it
                    if (it.isEmpty()) {
                        _error.value = "Tidak ada lagu yang ditemukan."
                    }
                }.onFailure {
                    _results.value = emptyList()
                    _error.value = it.message ?: "Pencarian gagal."
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
''', encoding="utf-8")

# =========================================================
# SEARCH SCREEN
# =========================================================

screen_path = root / "search/SearchScreen.kt"
backup(screen_path)

screen_path.write_text(r'''package com.foxxy.play.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import com.foxxy.play.feature.player.MusicTrack

@Composable
fun SearchScreen(
    viewModel: FoxxyPlayerViewModel,
    onOpenOnlineTrack: (OnlineMusicTrack) -> Unit
) {
    val onlineViewModel: OnlineSearchViewModel = viewModel()

    val tracks by viewModel.tracks.collectAsState()
    val recentIds by viewModel.recentTrackIds.collectAsState()

    val onlineResults by onlineViewModel.results.collectAsState()
    val onlineLoading by onlineViewModel.loading.collectAsState()
    val onlineError by onlineViewModel.error.collectAsState()
    val onlineSearched by onlineViewModel.searched.collectAsState()

    val discoverResults by onlineViewModel.discoverResults.collectAsState()
    val discoverLoading by onlineViewModel.discoverLoading.collectAsState()
    val discoverError by onlineViewModel.discoverError.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        onlineViewModel.loadDiscover()
    }

    val filtered = remember(query, tracks) {
        if (query.isBlank()) emptyList()
        else tracks.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }
    }

    val recentTracks = remember(recentIds, tracks) {
        recentIds.mapNotNull { id ->
            tracks.firstOrNull { it.id == id }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Songs, artists, albums...")
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, null)
            },
            trailingIcon = {
                if (selectedTab == 1) {
                    TextButton(
                        enabled = query.isNotBlank() && !onlineLoading,
                        onClick = {
                            onlineViewModel.search(query)
                        }
                    ) {
                        Text("Search")
                    }
                }
            },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        val tabs = listOf("Local", "Online", "Recently Played")

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                when {
                    query.isBlank() -> SearchEmpty(
                        "Search your local music library."
                    )

                    filtered.isEmpty() -> SearchEmpty(
                        "No matching local songs found."
                    )

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { track ->
                            SearchTrackItem(track) {
                                viewModel.playTrack(track)
                            }
                        }
                    }
                }
            }

            1 -> {
                if (onlineSearched) {
                    when {
                        onlineLoading -> LoadingContent()

                        onlineError != null -> SearchEmpty(
                            onlineError ?: "Search failed."
                        )

                        onlineResults.isEmpty() -> SearchEmpty(
                            "No online tracks found."
                        )

                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                onlineResults,
                                key = { "${it.source}:${it.id}" }
                            ) { track ->
                                OnlineTrackItem(track) {
                                    onOpenOnlineTrack(track)
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Discover Picks",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "Pilihan musik untuk kamu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                enabled = !discoverLoading,
                                onClick = {
                                    onlineViewModel.loadDiscover(forceRefresh = true)
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Refresh recommendations"
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh")
                            }
                        }

                        when {
                            discoverLoading && discoverResults.isEmpty() ->
                                LoadingContent()

                            discoverResults.isEmpty() ->
                                SearchEmpty(
                                    discoverError
                                        ?: "Rekomendasi belum tersedia."
                                )

                            else -> {
                                if (discoverLoading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(
                                        discoverResults,
                                        key = { "${it.source}:${it.id}" }
                                    ) { track ->
                                        OnlineTrackItem(track) {
                                            onOpenOnlineTrack(track)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${recentTracks.size} recently played",
                        style = MaterialTheme.typography.titleSmall
                    )

                    TextButton(
                        onClick = { viewModel.clearRecentlyPlayed() },
                        enabled = recentTracks.isNotEmpty()
                    ) {
                        Text("Clear")
                    }
                }

                if (recentTracks.isEmpty()) {
                    SearchEmpty("Songs you play will appear here.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(recentTracks, key = { it.id }) { track ->
                            SearchTrackItem(track) {
                                viewModel.playTrack(track)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SearchTrackItem(
    track: MusicTrack,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "${track.artist} • ${track.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OnlineTrackItem(
    track: OnlineMusicTrack,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!track.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                modifier = Modifier.size(52.dp)
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                track.album.ifBlank { track.source },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = "Track details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchEmpty(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
''', encoding="utf-8")

print("Discover Picks berhasil dipasang.")
print("Backup:")
print(f"- {vm_path}.before-discover")
print(f"- {screen_path}.before-discover")
