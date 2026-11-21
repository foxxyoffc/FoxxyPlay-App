from pathlib import Path

p = Path("app/src/main/java/com/foxxy/play/feature/player/NowPlayingScreen.kt")
s = p.read_text()

start = s.find("    Column(\n        modifier = Modifier\n            .fillMaxSize()")

end = s.find("    if (showSleepTimerDialog) {", start)

if start < 0 or end < 0:
    raise SystemExit("ERROR: Struktur file tidak cocok. File asli tetap aman.")

ui = r'''    Box(
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
                TextButton(
                    enabled = hasTrack,
                    onClick = { showPlaylistDialog = true }
                ) {
                    Text("＋ Playlist", color = FoxxyGoldLight)
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

'''

s = s[:start] + ui + s[end:]

imports = [
    "import androidx.compose.foundation.shape.CircleShape",
    "import androidx.compose.ui.graphics.Brush",
    "import androidx.compose.ui.text.font.FontWeight",
    "import androidx.compose.ui.text.style.TextAlign",
    "import androidx.compose.foundation.BorderStroke",
    "import com.foxxy.play.core.ui.theme.*"
]

for item in imports:
    if item not in s:
        s = s.replace("import java.io.File", "import java.io.File\n" + item, 1)

p.write_text(s)
print("Now Playing redesign berhasil diterapkan.")
print("Dialog dan fungsi pemutar lama tetap dipertahankan.")
