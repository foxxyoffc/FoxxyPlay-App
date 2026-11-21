package com.foxxy.play.feature.home

import com.foxxy.play.core.ui.FoxxyPlayLoadingAnimation
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.foxxy.play.BuildConfig
import com.foxxy.play.R
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import com.foxxy.play.feature.developer.DeveloperHomeAction
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import com.foxxy.play.feature.search.OnlineSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Image
import androidx.compose.foundation.border

@Composable
fun HomeScreen(
    viewModel: FoxxyPlayerViewModel,
    onOpenOnlineTrack: (OnlineMusicTrack) -> Unit,
    onOpenDeveloperPanel: () -> Unit = {}
) {
    val onlineViewModel: OnlineSearchViewModel = viewModel()

    val tracks by viewModel.tracks.collectAsState()
    val recentIds by viewModel.recentTrackIds.collectAsState()

    val discover by onlineViewModel.discoverResults.collectAsState()
    val discoverLoading by onlineViewModel.discoverLoading.collectAsState()
    val discoverError by onlineViewModel.discoverError.collectAsState()

    val context = LocalContext.current
    var updateVersion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onlineViewModel.loadDiscover()
    }

    LaunchedEffect(Unit) {
        val detectedVersion = withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .callTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://foxxyplay-api.vercel.app/api/app/version")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        null
                    } else {
                        val json = JSONObject(response.body?.string().orEmpty())
                        val packageName = json.optString("packageName")
                        val latestCode = json.optInt("versionCode", 0)
                        val latestName = json.optString("versionName")

                        if (
                            packageName == "com.foxxy.play" &&
                            latestCode > BuildConfig.VERSION_CODE &&
                            latestName.isNotBlank()
                        ) latestName else null
                    }
                }
            }.getOrNull()
        }

        updateVersion = detectedVersion
    }

    val recentTracks = remember(recentIds, tracks) {
        recentIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
    }

    val homePicks = remember(discover) { discover.shuffled() }
    val discoverPicks = remember(homePicks) { homePicks.take(8) }

    val discoverAlbumKeys = remember(discoverPicks) {
        discoverPicks.map {
            "${it.album.lowercase()}-${it.artist.lowercase()}"
        }.toSet()
    }

    val albumPicks = remember(homePicks, discoverAlbumKeys) {
        homePicks.drop(8)
            .filter { it.album.isNotBlank() }
            .filter {
                "${it.album.lowercase()}-${it.artist.lowercase()}" !in discoverAlbumKeys
            }
            .distinctBy { "${it.album.lowercase()}-${it.artist.lowercase()}" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            HomeHeader(onOpenDeveloperPanel = onOpenDeveloperPanel)
        }

        if (updateVersion != null) {
            item {
                UpdateAvailableCard(
                    version = updateVersion.orEmpty(),
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://foxxyplay.vercel.app/#download")
                            )
                        )
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle(
                    title = "Pilihan Hari Ini",
                    subtitle = "Temukan musik yang cocok dengan suasana hatimu",
                    icon = Icons.Rounded.AutoAwesome
                )

                val featured = discover.firstOrNull()
                if (featured != null) {
                    FeaturedMusicCard(
                        track = featured,
                        onClick = { onOpenOnlineTrack(featured) }
                    )
                } else {
                    FeaturedEmptyCard(
                        loading = discoverLoading,
                        message = discoverError ?: "Rekomendasi musik akan muncul di sini"
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle(
                    title = "Akses Cepat",
                    subtitle = "Putar kembali musik lokalmu",
                    icon = Icons.Rounded.History
                )

                if (recentTracks.isEmpty()) {
                    EmptyMusicCard(
                        title = "Belum ada aktivitas",
                        description = "Lagu lokal yang kamu putar akan muncul di sini."
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            recentTracks.take(8),
                            key = { "quick-${it.id}" }
                        ) { track ->
                            QuickAccessCard(
                                title = track.title,
                                artist = track.artist,
                                onClick = { viewModel.playTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle(
                    title = "Discover Picks",
                    subtitle = "Pilihan musik online",
                    icon = Icons.Rounded.AutoAwesome
                )

                when {
                    discoverLoading && discover.isEmpty() -> HomeLoading()

                    discoverPicks.isEmpty() -> EmptyMusicCard(
                        title = "Belum ada rekomendasi",
                        description = discoverError ?: "Coba muat ulang beberapa saat lagi."
                    )

                    else -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        items(
                            discoverPicks,
                            key = { "discover-${it.source}:${it.id}" }
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

        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle(
                    title = "Album untuk Dijelajahi",
                    subtitle = "Koleksi pilihan dari rekomendasi musik",
                    icon = Icons.Rounded.Album
                )

                if (albumPicks.isEmpty()) {
                    EmptyMusicCard(
                        title = "Album sedang disiapkan",
                        description = "Album akan muncul saat rekomendasi tersedia."
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        items(
                            albumPicks.take(12),
                            key = { "album-${it.album}:${it.artist}" }
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
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeSectionTitle(
                    title = "Baru Diputar",
                    subtitle = "Riwayat musik lokal kamu",
                    icon = Icons.Rounded.History
                )

                if (recentTracks.isEmpty()) {
                    EmptyMusicCard(
                        title = "Belum ada lagu diputar",
                        description = "Riwayat pemutaran akan tersimpan di sini."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        recentTracks.take(8).forEachIndexed { index, track ->
                            RecentTrackRow(
                                index = index + 1,
                                title = track.title,
                                artist = track.artist,
                                onClick = { viewModel.playTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        item {
            FooterDecoration()
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenDeveloperPanel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF2C875),
                            Color(0xFF9B702B),
                            Color(0xFF3B2B16)
                        )
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color(0xFF090A0B)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.foxxyplay_logo),
                contentDescription = "Logo FoxxyPlay",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FoxxyPlay",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = "YOUR MUSIC. YOUR VIBE.",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }

        DeveloperHomeAction(onClick = onOpenDeveloperPanel)
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(37.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2115)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeaturedMusicCard(
    track: OnlineMusicTrack,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(25.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF49371C), Color(0xFF17130D), Color(0xFF101113))
                )
            )
            .clickable(onClick = onClick)
    ) {
        if (!track.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.album,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(224.dp),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x22000000),
                            Color(0x66000000),
                            Color(0xF5090A0B)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(19.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFF2C875),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "RECOMMENDED FOR YOU",
                    color = Color(0xFFF2C875),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp
                )
            }

            Spacer(Modifier.height(7.dp))

            Text(
                track.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                track.artist,
                color = Color(0xFFD4D0C8),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(39.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9A441)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Putar rekomendasi",
                        tint = Color(0xFF17120A),
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    "Putar sekarang",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FeaturedEmptyCard(
    loading: Boolean,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF49371C), Color(0xFF211B12), Color(0xFF121416))
                )
            )
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color(0xFFF2C875),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFF2C875),
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (loading) "Menyiapkan rekomendasi..." else message,
                color = Color(0xFFE8E1D4),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    artist: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "quick-card-scale"
    )

    Row(
        modifier = Modifier
            .width(220.dp)
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(49.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF63491F), Color(0xFF2B2114))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFFF2C875),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
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
    var menuExpanded by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "music-card-scale"
    )

    Column(
        modifier = Modifier
            .width(158.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                        onLongClick = { menuExpanded = true }
                    )
            ) {
                if (!track.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.album,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF49371C), Color(0xFF151515))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFF2C875),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(9.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9A441)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color(0xFF17120A)
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Putar musik") },
                    leadingIcon = {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            if (showAlbum) track.album else track.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
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
private fun RecentTrackRow(
    index: Int,
    title: String,
    artist: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(30.dp)
        )

        Box(
            modifier = Modifier
                .size(43.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF302516)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFFF2C875)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = "Putar",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyMusicCard(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF302516)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFFF2C875)
            )
        }

        Spacer(Modifier.width(13.dp))

        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    version: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF292116),
                        Color(0xFF171512),
                        Color(0xFF101113)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFFD9A441).copy(alpha = 0.42f),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.foxxy.play.R.drawable.foxxyplay_logo
            ),
            contentDescription = "Logo FoxxyPlay",
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "PEMBARUAN TERSEDIA",
                color = Color(0xFFF2C875),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            Text(
                "FoxxyPlay v$version",
                color = Color(0xFFF3F0E9),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(3.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD9A441))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Update Sekarang",
                    color = Color(0xFF17120A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF17120A),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
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
private fun FooterDecoration() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(Color(0xFF80602C))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "MADE FOR YOUR VIBE",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(Color(0xFF80602C))
        )
    }
}
