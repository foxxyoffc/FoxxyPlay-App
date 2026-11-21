from pathlib import Path
import shutil

home = Path("app/src/main/java/com/foxxy/play/feature/home/HomeScreen.kt")
nav = Path("app/src/main/java/com/foxxy/play/core/navigation/FoxxyNavigation.kt")

for p in (home, nav):
    if not p.exists():
        raise FileNotFoundError(p)
    shutil.copy2(p, str(p) + ".before-home-discover")

home.write_text(r'''package com.foxxy.play.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import com.foxxy.play.feature.search.OnlineSearchViewModel

@Composable
fun HomeScreen(
    viewModel: FoxxyPlayerViewModel,
    onOpenOnlineTrack: (OnlineMusicTrack) -> Unit
) {
    val onlineViewModel: OnlineSearchViewModel = viewModel()

    val tracks by viewModel.tracks.collectAsState()
    val recentIds by viewModel.recentTrackIds.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()

    val discover by onlineViewModel.discoverResults.collectAsState()
    val discoverLoading by onlineViewModel.discoverLoading.collectAsState()
    val discoverError by onlineViewModel.discoverError.collectAsState()

    LaunchedEffect(Unit) {
        onlineViewModel.loadDiscover()
    }

    val recentTracks = remember(recentIds, tracks) {
        recentIds.mapNotNull { id ->
            tracks.firstOrNull { it.id == id }
        }
    }

    val albumPicks = remember(discover) {
        discover
            .filter { it.album.isNotBlank() }
            .distinctBy { "${it.album.lowercase()}-${it.artist.lowercase()}" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 22.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "FoxxyPlay",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Your Music. Your Vibe.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Icon(
                        Icons.Rounded.Headphones,
                        contentDescription = "FoxxyPlay",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSectionTitle("Akses Cepat")

                if (recentTracks.isEmpty()) {
                    Text(
                        "Lagu lokal yang baru kamu putar akan muncul di sini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    recentTracks.take(6).chunked(2).forEach { rowTracks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.playTrack(track) }
                                        .padding(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        track.title,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            if (rowTracks.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSectionTitle(
                    title = "Discover Picks",
                    subtitle = "Pilihan musik dari Spotify"
                )

                when {
                    discoverLoading && discover.isEmpty() -> {
                        HomeLoading()
                    }

                    discover.isEmpty() -> {
                        Text(
                            discoverError ?: "Rekomendasi belum tersedia.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                discover.take(12),
                                key = { "${it.source}:${it.id}" }
                            ) { track ->
                                HomeOnlineCard(
                                    track = track,
                                    onClick = { onOpenOnlineTrack(track) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSectionTitle(
                    title = "Album untuk Dijelajahi",
                    subtitle = "Dikelompokkan dari lagu rekomendasi"
                )

                if (albumPicks.isEmpty()) {
                    Text(
                        "Album akan muncul saat rekomendasi tersedia.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            albumPicks.take(12),
                            key = { "${it.album}:${it.artist}" }
                        ) { track ->
                            HomeOnlineCard(
                                track = track,
                                showAlbum = true,
                                onClick = { onOpenOnlineTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSectionTitle(
                    title = "Baru Diputar",
                    subtitle = "Riwayat musik lokal kamu"
                )

                if (recentTracks.isEmpty()) {
                    Text(
                        "Lagu yang kamu putar akan muncul di sini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentTracks.take(8).forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.playTrack(track) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
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
                                    track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeOnlineCard(
    track: OnlineMusicTrack,
    showAlbum: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(158.dp)
            .clickable(onClick = onClick)
    ) {
        if (!track.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.album,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Text(
            if (showAlbum) track.album else track.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            track.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HomeLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
''', encoding="utf-8")

text = nav.read_text(encoding="utf-8")
old = '''            composable(Screen.Home.route) {
                HomeScreen()
            }'''
new = '''            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = playerViewModel,
                    onOpenOnlineTrack = { track ->
                        playerViewModel.playOnlineTrack(track)
                        navController.navigate("now_playing") {
                            launchSingleTop = true
                        }
                    }
                )
            }'''

if old not in text:
    raise SystemExit("Pemanggilan HomeScreen lama tidak ditemukan. Navigation tidak diubah.")

nav.write_text(text.replace(old, new, 1), encoding="utf-8")

print("Home Discover berhasil dipasang.")
print("Backup:")
print(f"- {home}.before-home-discover")
print(f"- {nav}.before-home-discover")
