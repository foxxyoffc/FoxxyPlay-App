package com.foxxy.play.feature.settings

import androidx.core.content.FileProvider
import java.io.File
import android.widget.Toast
import android.content.Context
import android.content.ClipData
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxxy.play.BuildConfig
import com.foxxy.play.feature.player.FoxxyPlayerViewModel
import androidx.media3.common.Player

@Composable
fun SettingsScreen(
    playerViewModel: FoxxyPlayerViewModel,
    onOpenStorage: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasMusicPermission by remember {
        mutableStateOf(false)
    }
    var hasNotificationPermission by remember {
        mutableStateOf(false)
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) playerViewModel.scanLocalMusic()
    }

    fun requestScan() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            playerViewModel.scanLocalMusic()
        } else {
            scanLauncher.launch(permission)
        }
    }

    fun refreshMusicPermission() {
        hasMusicPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshMusicPermission()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        refreshMusicPermission()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shuffleEnabled by playerViewModel.shuffleEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val tracks by playerViewModel.tracks.collectAsState()
    val isScanning by playerViewModel.isScanning.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    val repeatLabel = when (repeatMode) {
        Player.REPEAT_MODE_ALL -> "Repeat All"
        Player.REPEAT_MODE_ONE -> "Repeat One"
        else -> "Off"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Customize your FoxxyPlay experience",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "PLAYBACK") {
            SettingsRow(
                title = "Shuffle",
                subtitle = "Play tracks in random order"
            ) {
                Switch(
                    checked = shuffleEnabled,
                    onCheckedChange = {
                        playerViewModel.toggleShuffle()
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            SettingsRow(
                title = "Repeat Mode",
                subtitle = "Current mode: $repeatLabel"
            ) {
                TextButton(
                    onClick = { playerViewModel.cycleRepeatMode() }
                ) {
                    Text(
                        repeatLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "LIBRARY & DATA") {
            SettingsRow(
                title = "Recently Played",
                subtitle = "Clear your recently played history"
            ) {
                TextButton(
                    onClick = { showClearDialog = true }
                ) {
                    Text(
                        "Clear",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "STORAGE") {
            SettingsRow(
                title = "Storage Usage",
                subtitle = "View device storage and FoxxyPlay data"
            ) {
                TextButton(onClick = onOpenStorage) {
                    Text("View")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "PRIVACY & PERMISSIONS") {
            SettingsRow(
                title = "Notification Permission",
                subtitle = if (Build.VERSION.SDK_INT < 33) {
                    "Not required on this Android version"
                } else if (hasNotificationPermission) {
                    "Notification access is allowed"
                } else {
                    "Notification access has not been granted"
                }
            ) {
                Text(
                    when {
                        Build.VERSION.SDK_INT < 33 -> "Not required"
                        hasNotificationPermission -> "Granted"
                        else -> "Not granted"
                    },
                    color = if (Build.VERSION.SDK_INT < 33 || hasNotificationPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            SettingsRow(
                title = "Android App Permissions",
                subtitle = "Review and manage FoxxyPlay permissions in system settings"
            ) {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Manage")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "MUSIC LIBRARY") {
            SettingsRow(
                title = "Music Permission",
                subtitle = if (hasMusicPermission) {
                    "FoxxyPlay can access local audio"
                } else {
                    "Permission is required to scan local music"
                }
            ) {
                Text(
                    if (hasMusicPermission) "Granted" else "Required",
                    color = if (hasMusicPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            SettingsRow(
                title = "Local Music",
                subtitle = "${tracks.size} songs available"
            ) {
                Text(
                    "${tracks.size}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            SettingsRow(
                title = "Room Music Cache",
                subtitle = "Saved song metadata for faster library loading"
            ) {
                Text(
                    if (tracks.isEmpty()) "Empty" else "Ready",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            SettingsRow(
                title = "Rescan Music",
                subtitle = if (isScanning) {
                    "Scanning your music library..."
                } else {
                    "Search for newly added local music"
                }
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(
                        onClick = { requestScan() },
                        enabled = !isScanning
                    ) {
                        Text(if (hasMusicPermission) "Scan" else "Grant Access")
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "APPEARANCE") {
            SettingsRow(
                title = "Silver Dark Theme",
                subtitle = "Your interface uses the default silver color scheme"
            ) {
                Text(
                    "Active",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "SHARE APP") {
            SettingsRow(
                title = "Share FoxxyPlay APK",
                subtitle = "Send the installation file to another device"
            ) {
                TextButton(
                    onClick = {
                        try {
                            val source = File(context.applicationInfo.sourceDir)
                            val shareDir = File(context.cacheDir, "share").apply {
                                mkdirs()
                            }
                            val apkFile = File(shareDir, "FoxxyPlay.apk")

                            source.copyTo(apkFile, overwrite = true)

                            val apkUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                clipData = ClipData.newUri(
                                    context.contentResolver,
                                    "FoxxyPlay.apk",
                                    apkUri
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share FoxxyPlay APK"
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Gagal membagikan APK: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Share")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsSection(title = "ABOUT") {
            SettingsRow(
                title = "FoxxyPlay",
                subtitle = "Version ${BuildConfig.VERSION_NAME}"
            ) {
                Text(
                    "♫",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            SettingsRow(
                title = "About FoxxyPlay",
                subtitle = "Application information and developer contact"
            ) {
                TextButton(onClick = onOpenAbout) {
                    Text("View")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "FoxxyPlay • Made for music lovers",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("Clear Recently Played?")
            },
            text = {
                Text("Your recently played history will be cleared. Your music files and playlists will remain untouched.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerViewModel.clearRecentlyPlayed()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        "Clear History",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))
        trailing()
    }
}
