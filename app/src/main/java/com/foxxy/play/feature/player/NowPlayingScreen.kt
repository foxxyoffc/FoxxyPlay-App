package com.foxxy.play.feature.player

import java.io.File
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import com.foxxy.play.core.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: FoxxyPlayerViewModel,
    onBack: () -> Unit
) {
    val track by viewModel.currentTrack.collectAsState()
    val currentDownload by viewModel.currentDownload.collectAsState()
    val onlineTrack by viewModel.currentOnlineTrack.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val downloadingIds by viewModel.downloadingTrackIds.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()

    val onlineDownloadKey = onlineTrack?.let { "${it.source}:${it.id}" }
    val isDownloaded = onlineTrack != null && downloads.any {
        it.source == onlineTrack?.source && it.trackId == onlineTrack?.id
    }
    val isDownloading = onlineDownloadKey != null &&
        onlineDownloadKey in downloadingIds

    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val onlineFavoriteIds by viewModel.onlineFavoriteIds.collectAsState()

    val playlists by viewModel.playlists.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffle by viewModel.shuffleEnabled.collectAsState()
    val repeat by viewModel.repeatMode.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingMs.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepHours by remember { mutableStateOf("0") }
    var sleepMinutes by remember { mutableStateOf("30") }

    var dragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val title = onlineTrack?.title
        ?: currentDownload?.title
        ?: track?.title
        ?: "Nothing Playing"

    val artist = onlineTrack?.artist
        ?: currentDownload?.artist
        ?: track?.artist
        ?: "Choose a song from your library"

    val album = onlineTrack?.album ?: track?.album

    val downloadCover = currentDownload?.coverPath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { it.exists() }

    val cover: Any? = onlineTrack?.coverUrl
        ?.takeIf { it.isNotBlank() }
        ?: downloadCover

    val hasTrack = track != null ||
        onlineTrack != null ||
        currentDownload != null
    val maxDuration = duration.coerceAtLeast(1L)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF60451F),
                        Color(0xFF382916),
                        Color(0xFF17130E),
                        FoxxyBackground,
                        FoxxyBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = FoxxyText)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "NOW PLAYING",
                    color = FoxxyGoldLight,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 370.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(25.dp))
                    .background(FoxxySurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = FoxxyGoldLight
                    )
                }
            }

            Spacer(Modifier.height(27.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = FoxxyText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        artist,
                        color = FoxxyGoldLight,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!album.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            album,
                            color = FoxxyMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val isFavorite = if (onlineTrack != null) {
                    "${onlineTrack!!.source}:${onlineTrack!!.id}" in onlineFavoriteIds
                } else {
                    track?.id in favoriteIds
                }

                IconButton(
                    enabled = hasTrack,
                    onClick = {
                        onlineTrack?.let { viewModel.toggleOnlineFavorite(it) }
                            ?: track?.let { viewModel.toggleFavorite(it) }
                    }
                ) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF6685) else FoxxyGoldLight,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { if (hasTrack) showPlaylistDialog = true },
                    enabled = hasTrack,
                    shape = RoundedCornerShape(14.dp),
                    color = FoxxyGold.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        FoxxyGold.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 15.dp,
                            vertical = 11.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FoxxyPlaylistSvg(
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Add to Playlist",
                            color = FoxxyGoldLight,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (onlineTrack != null) {
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = !isDownloaded && !isDownloading,
                        onClick = {
                            onlineTrack?.let { viewModel.downloadOnlineTrack(it) }
                        }
                    ) {
                        when {
                            isDownloading -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = FoxxyGoldLight
                            )
                            isDownloaded -> {
                                Icon(Icons.Rounded.Check, null, tint = FoxxyGoldLight)
                                Spacer(Modifier.width(5.dp))
                                Text("Downloaded", color = FoxxyGoldLight)
                            }
                            else -> {
                                Icon(Icons.Rounded.Download, null, tint = FoxxyGoldLight)
                                Spacer(Modifier.width(5.dp))
                                Text("Download", color = FoxxyGoldLight)
                            }
                        }
                    }
                }
            }

            if (!downloadError.isNullOrBlank()) {
                Text(
                    downloadError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                val sliderValue = if (dragging) {
                    sliderPosition
                } else {
                    position.coerceIn(0L, maxDuration).toFloat()
                }

                val fraction = (
                    sliderValue / maxDuration.toFloat()
                ).coerceIn(0f, 1f)

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        dragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        viewModel.seekTo(sliderPosition.toLong())
                        dragging = false
                    },
                    valueRange = 0f..maxDuration.toFloat(),
                    enabled = hasTrack && duration > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = FoxxyGoldLight,
                        activeTrackColor = FoxxyGold,
                        inactiveTrackColor = FoxxySurfaceVariant
                    ),
                    thumb = {
                        Box(
                            Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(FoxxyGoldLight)
                        )
                    }
                )

                if (dragging) {
                    Text(
                        text = formatTime(sliderPosition.toLong()),
                        modifier = Modifier
                            .width(46.dp)
                            .offset(
                                x = (maxWidth - 46.dp) * fraction,
                                y = (-19).dp
                            ),
                        color = FoxxyGoldLight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(if (dragging) sliderPosition.toLong() else position),
                    color = FoxxyGoldLight,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    formatTime(duration),
                    color = FoxxyMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(23.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = hasTrack,
                    onClick = { viewModel.toggleShuffle() }
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        "Shuffle",
                        tint = if (shuffle) FoxxyGoldLight else FoxxyMuted
                    )
                }

                IconButton(
                    enabled = hasTrack,
                    onClick = { viewModel.previousTrack() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        "Previous",
                        tint = FoxxyText,
                        modifier = Modifier.size(34.dp)
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(70.dp),
                    enabled = hasTrack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = FoxxyGold,
                        contentColor = FoxxyBackground
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        "Play or pause",
                        modifier = Modifier.size(39.dp)
                    )
                }

                IconButton(
                    enabled = hasTrack,
                    onClick = { viewModel.nextTrack() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        "Next",
                        tint = FoxxyText,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    enabled = hasTrack,
                    onClick = { viewModel.cycleRepeatMode() }
                ) {
                    Box {
                        Icon(
                            Icons.Rounded.Repeat,
                            "Repeat",
                            tint = if (repeat != Player.REPEAT_MODE_OFF) {
                                FoxxyGoldLight
                            } else {
                                FoxxyMuted
                            }
                        )
                        if (repeat == Player.REPEAT_MODE_ONE) {
                            Text(
                                "1",
                                fontSize = 9.sp,
                                color = FoxxyGoldLight,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = FoxxySurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, FoxxyGoldDark.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showSleepTimerDialog = true }) {
                        Text(
                            if (sleepTimerRemaining > 0L) {
                                "◷ Sleep Timer • ${formatSleepTimer(sleepTimerRemaining)}"
                            } else {
                                "◷ Sleep Timer"
                            },
                            color = FoxxyGoldLight
                        )
                    }

                    if (sleepTimerRemaining > 0L) {
                        TextButton(onClick = { viewModel.cancelSleepTimer() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    Text(
                        "Choose a duration",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15, 30).forEach { minutes ->
                            TextButton(
                                onClick = {
                                    viewModel.startSleepTimer(0, minutes)
                                    showSleepTimerDialog = false
                                }
                            ) {
                                Text("$minutes min")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(45, 60).forEach { minutes ->
                            TextButton(
                                onClick = {
                                    viewModel.startSleepTimer(0, minutes)
                                    showSleepTimerDialog = false
                                }
                            ) {
                                Text("$minutes min")
                            }
                        }
                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Custom time",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = sleepHours,
                            onValueChange = {
                                sleepHours = it.filter(Char::isDigit).take(3)
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Hours") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sleepMinutes,
                            onValueChange = {
                                sleepMinutes = it.filter(Char::isDigit).take(2)
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minutes") },
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Set hours and minutes. Minimum duration is 1 minute.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hours = sleepHours.toIntOrNull() ?: 0
                        val minutes = sleepMinutes.toIntOrNull() ?: 0

                        if (hours >= 0 && minutes in 0..59 &&
                            (hours > 0 || minutes > 0)
                        ) {
                            viewModel.startSleepTimer(hours, minutes)
                            showSleepTimerDialog = false
                        }
                    },
                    enabled = (
                        (sleepHours.toIntOrNull() ?: 0) > 0 ||
                        (sleepMinutes.toIntOrNull() ?: 0) in 1..59
                    ) && (sleepMinutes.toIntOrNull() ?: 0) in 0..59
                ) {
                    Text("Start Timer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = FoxxySurface,
            titleContentColor = FoxxyText,
            textContentColor = FoxxyMuted,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(43.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(FoxxyGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        FoxxyPlaylistSvg(
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            "Add to Playlist",
                            color = FoxxyText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Simpan lagu favoritmu",
                            color = FoxxyMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "YOUR PLAYLISTS",
                        color = FoxxyGoldLight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(FoxxySurfaceVariant)
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FoxxyPlaylistSvg(
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    "Belum ada playlist",
                                    color = FoxxyText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Buat playlist baru untuk menyimpan lagu ini.",
                                    color = FoxxyMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 190.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playlists.forEach { playlist ->
                                Surface(
                                    onClick = {
                                        viewModel.addCurrentTrackToPlaylist(playlist.id)
                                        showPlaylistDialog = false
                                    },
                                    shape = RoundedCornerShape(13.dp),
                                    color = FoxxySurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        FoxxyGoldDark.copy(alpha = 0.28f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 13.dp,
                                                vertical = 12.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(37.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    FoxxyGold.copy(alpha = 0.13f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            FoxxyPlaylistSvg(
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(11.dp))

                                        Text(
                                            playlist.name,
                                            modifier = Modifier.weight(1f),
                                            color = FoxxyText,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add",
                                            tint = FoxxyGoldLight
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    HorizontalDivider(
                        color = FoxxyGoldDark.copy(alpha = 0.35f)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "CREATE NEW PLAYLIST",
                        color = FoxxyGoldLight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Masukkan nama playlist",
                                color = FoxxyMuted
                            )
                        },
                        leadingIcon = {
                            FoxxyPlaylistSvg(
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FoxxyGold,
                            unfocusedBorderColor = FoxxyGoldDark.copy(alpha = 0.5f),
                            focusedTextColor = FoxxyText,
                            unfocusedTextColor = FoxxyText,
                            cursorColor = FoxxyGoldLight,
                            focusedLeadingIconColor = FoxxyGoldLight,
                            unfocusedLeadingIconColor = FoxxyMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPlaylistName.isNotBlank() && hasTrack,
                    onClick = {
                        viewModel.createPlaylistWithCurrentTrack(newPlaylistName)
                        newPlaylistName = ""
                        showPlaylistDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoxxyGold,
                        contentColor = FoxxyBackground,
                        disabledContainerColor = FoxxySurfaceVariant,
                        disabledContentColor = FoxxyMuted
                    )
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Create & Add",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPlaylistDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = FoxxyMuted
                    )
                }
            }
        )
    }
}

private fun formatSleepTimer(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}


@Composable
private fun FoxxyPlaylistSvg(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gold = Color(0xFFF2C875)
        val stroke = Stroke(
            width = size.minDimension * 0.085f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Garis playlist
        for (i in 0..2) {
            val y = size.height * (0.25f + i * 0.25f)
            drawLine(
                color = gold,
                start = Offset(size.width * 0.12f, y),
                end = Offset(size.width * 0.48f, y),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
        }

        // Simbol musik di sisi kanan
        val x = size.width * 0.70f
        val top = size.height * 0.13f
        val bottom = size.height * 0.68f

        drawLine(
            color = gold,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )

        drawLine(
            color = gold,
            start = Offset(x, top),
            end = Offset(size.width * 0.94f, top + size.height * 0.06f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = gold,
            radius = size.minDimension * 0.105f,
            center = Offset(x - size.width * 0.10f, bottom + size.height * 0.02f)
        )
    }
}
