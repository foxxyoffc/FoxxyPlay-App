package com.foxxy.play.feature.search

import com.foxxy.play.core.ui.FoxxyPlayLoadingAnimation
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search music"
                )
            },
            trailingIcon = {
                if (selectedTab == 1) {
                    FilledIconButton(
                        onClick = { onlineViewModel.search(query) },
                        enabled = query.isNotBlank() && !onlineLoading,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (onlineLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "Search online"
                            )
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(14.dp))

        val tabs = listOf("Local", "Online", "Recently Played")

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
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
                                OnlineTrackItem(
                                    track = track,
                                    onClick = { onOpenOnlineTrack(track) },
                                )
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

                            FilledTonalButton(
                                enabled = !discoverLoading,
                                onClick = {
                                    onlineViewModel.loadDiscover(forceRefresh = true)
                                },
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
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
                                        OnlineTrackItem(
                                    track = track,
                                    onClick = { onOpenOnlineTrack(track) },
                                )
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
            .padding(top = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        FoxxyPlayLoadingAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}

@Composable
private fun SearchTrackItem(
    track: MusicTrack,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    track.album,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = "Play track",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun OnlineTrackItem(
    track: OnlineMusicTrack,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchEmpty(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
