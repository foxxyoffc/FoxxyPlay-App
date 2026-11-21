package com.foxxy.play.feature.player

import java.io.File

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.foxxy.play.core.ui.theme.*

@Composable
fun FoxxyMiniPlayer(
    viewModel: FoxxyPlayerViewModel,
    onOpen: () -> Unit
) {
    val track by viewModel.currentTrack.collectAsState()
    val onlineTrack by viewModel.currentOnlineTrack.collectAsState()
    val download by viewModel.currentDownload.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val title = onlineTrack?.title ?: download?.title ?: track?.title
    val artist = onlineTrack?.artist ?: download?.artist ?: track?.artist

    val localCover = download?.coverPath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { it.exists() }

    val cover: Any? = onlineTrack?.coverUrl
        ?.takeIf { it.isNotBlank() }
        ?: localCover

    if (title != null) {
        val progress = if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(18.dp),
            color = FoxxySurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                FoxxyGold.copy(alpha = 0.28f)
            ),
            shadowElevation = 8.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen() }
                        .padding(start = 10.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(FoxxyGoldDark, FoxxySurfaceVariant)
                                )
                            ),
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
                                tint = FoxxyGoldLight,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FoxxyText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            artist.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FoxxyMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = FoxxyGold,
                        onClick = { viewModel.togglePlayPause() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) {
                                    Icons.Rounded.Pause
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = FoxxyBackground,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(FoxxySurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(FoxxyGoldDark, FoxxyGoldLight)
                                )
                            )
                    )
                }
            }
        }
    }
}
