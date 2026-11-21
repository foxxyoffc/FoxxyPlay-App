package com.foxxy.play.feature.player

import com.foxxy.play.core.ui.FoxxyPlayLoadingAnimation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack

@Composable
fun OnlinePlayerScreen(
    track: OnlineMusicTrack,
    viewModel: FoxxyPlayerViewModel,
    onBack: () -> Unit
) {
    val loading by viewModel.isResolvingOnline.collectAsState()
    val error by viewModel.onlinePlaybackError.collectAsState()

    LaunchedEffect(track.id) {
        viewModel.playOnlineTrack(track)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "ONLINE PLAYER",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.weight(1f))

        if (!track.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = "Album cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            track.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            track.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        when {
            loading -> {
                FoxxyPlayLoadingAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
                Spacer(Modifier.height(12.dp))
                Text("Menyiapkan audio...")
            }

            error != null -> {
                Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.playOnlineTrack(track) }) {
                    Text("Coba Lagi")
                }
            }

            else -> {
                Text(
                    "Lagu dikirim ke pemutar FoxxyPlay.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { viewModel.togglePlayPause() }) {
                    Text("Play / Pause")
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}
