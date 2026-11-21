package com.foxxy.play.feature.storage

import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StorageInfo(
    val total: Long,
    val available: Long,
    val database: Long,
    val cache: Long,
    val appData: Long,
    val files: Long,
    val preferences: Long
)

@Composable
fun StorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val info = remember(context, refreshKey) {
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        val database = context.getDatabasePath("foxxyplay_library.db")

        val databaseSize = listOf(
            database,
            File(database.path + "-wal"),
            File(database.path + "-shm")
        ).sumOf { if (it.exists()) it.length() else 0L }

        val cacheSize = directorySize(context.cacheDir)

        StorageInfo(
            total = stat.totalBytes,
            available = stat.availableBytes,
            database = databaseSize,
            cache = cacheSize,
            appData = directorySize(context.dataDir),
            files = directorySize(context.filesDir),
            preferences = directorySize(File(context.applicationInfo.dataDir, "shared_prefs"))
        )
    }

    val used = (info.total - info.available).coerceAtLeast(0L)
    val usage = if (info.total > 0) {
        (used.toFloat() / info.total).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                "Storage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { refreshKey++ }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "INTERNAL STORAGE",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    formatBytes(info.available),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Available of ${formatBytes(info.total)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(18.dp))

                LinearProgressIndicator(
                    progress = usage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(usage * 100).toInt()}% used",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${formatBytes(used)} used",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "FOXXYPLAY STORAGE",
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
            Column(Modifier.padding(horizontal = 16.dp)) {
                StorageRow(
                    "Application Data",
                    "Total app data directory",
                    formatBytes(info.appData)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )

                StorageRow(
                    "Room Database",
                    "Library, playlists and favorites",
                    formatBytes(info.database)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )

                StorageRow(
                    "App Cache",
                    "Temporary application files",
                    formatBytes(info.cache)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )

                StorageRow(
                    "App Files",
                    "Files stored by FoxxyPlay",
                    formatBytes(info.files)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )

                StorageRow(
                    "Preferences",
                    "Playback and application settings",
                    formatBytes(info.preferences)
                )
            }
        }

        Spacer(Modifier.height(16.dp))


        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "STORAGE MANAGEMENT",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Remove temporary files stored in the application cache.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Clear App Cache", fontWeight = FontWeight.SemiBold)
                        Text(
                            formatBytes(info.cache),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        enabled = info.cache > 0L && !isClearingCache
                    ) {
                        Text(if (isClearingCache) "Clearing..." else "Clear")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Storage figures are refreshed when this page opens or when you tap refresh. App data is an approximate directory size.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(28.dp))
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear temporary cache?") },
            text = {
                Text(
                    "This will remove temporary files from FoxxyPlay. " +
                    "Your database, playlists, favorites and music files will remain untouched."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        if (!isClearingCache) {
                            scope.launch {
                                isClearingCache = true

                                val result = withContext(Dispatchers.IO) {
                                    val before = directorySize(context.cacheDir)

                                    val success = context.cacheDir.listFiles()
                                        ?.all { file ->
                                            if (file.isDirectory) {
                                                file.deleteRecursively() || !file.exists()
                                            } else {
                                                file.delete() || !file.exists()
                                            }
                                        } ?: true

                                    val after = directorySize(context.cacheDir)

                                    Triple(before, after, success)
                                }

                                refreshKey++
                                isClearingCache = false

                                val freed = (result.first - result.second).coerceAtLeast(0L)

                                Toast.makeText(
                                    context,
                                    if (result.third) {
                                        "Cache cleared: ${formatBytes(freed)} freed"
                                    } else {
                                        "Some cache files could not be removed"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

}

@Composable
private fun StorageRow(
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun directorySize(directory: File): Long {
    if (!directory.exists()) return 0L
    return if (directory.isFile) {
        directory.length()
    } else {
        directory.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1

    do {
        value /= 1024.0
        index++
    } while (value >= 1024.0 && index < units.lastIndex)

    return String.format(Locale.US, "%.2f %s", value, units[index])
}
