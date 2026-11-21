package com.foxxy.play.core.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private sealed interface GateState {
    data object Checking : GateState
    data object Allowed : GateState
    data class Blocked(val message: String) : GateState
    data class Failed(val message: String) : GateState
}

private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

private suspend fun checkMaintenance(): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://foxxyplay-api.vercel.app/api/app/status")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }

            val json = JSONObject(
                response.body?.string() ?: throw IOException("Respons kosong")
            )

            if (!json.optBoolean("success", false)) {
                throw IOException("Status server tidak valid")
            }

            json.optBoolean("maintenance", false) to
                json.optString("maintenanceMessage", "")
        }
    }

@Composable
fun AppMaintenanceGate(content: @Composable () -> Unit) {
    var state by remember { mutableStateOf<GateState>(GateState.Checking) }

    LaunchedEffect(Unit) {
        state = GateState.Checking
        state = try {
            val (enabled, message) = checkMaintenance()
            if (enabled) {
                GateState.Blocked(
                    message.ifBlank {
                        "FoxxyPlay sedang dalam pemeliharaan. Silakan coba lagi nanti."
                    }
                )
            } else {
                GateState.Allowed
            }
        } catch (e: Exception) {
            GateState.Failed(e.message ?: "Gagal menghubungi server.")
        }
    }

    when (val current = state) {
        GateState.Allowed -> content()
        GateState.Checking -> GateScreen(
            title = "Memeriksa Status",
            message = "Menghubungkan ke server FoxxyPlay...",
            loading = true
        )
        is GateState.Blocked -> GateScreen(
            title = "Sedang Maintenance",
            message = current.message
        )
        is GateState.Failed -> GateScreen(
            title = "Tidak Dapat Terhubung",
            message = "Status aplikasi belum bisa diverifikasi.\\n${current.message}"
        )
    }
}

@Composable
private fun GateScreen(
    title: String,
    message: String,
    loading: Boolean = false
) {
    val gold = androidx.compose.ui.graphics.Color(0xFFD9A441)
    val goldLight = androidx.compose.ui.graphics.Color(0xFFF2C875)
    val background = androidx.compose.ui.graphics.Color(0xFF090A0B)
    val muted = androidx.compose.ui.graphics.Color(0xFF9B9A96)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color(0xFF1D1A13),
                                androidx.compose.ui.graphics.Color(0xFF121416),
                                androidx.compose.ui.graphics.Color(0xFF101113)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        gold.copy(alpha = 0.35f),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 27.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FOXXYPLAY  •  SYSTEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = goldLight,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(8.dp))

                MoonwalkAnimation()

                Spacer(Modifier.height(8.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFFF3F0E9),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(gold.copy(alpha = 0.25f), goldLight, gold.copy(alpha = 0.25f))
                            )
                        )
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF090A0B).copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        gold.copy(alpha = 0.16f)
                    )
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color(0xFFD5D0C5),
                        textAlign = TextAlign.Center
                    )
                }

                if (loading) {
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(27.dp),
                        color = gold,
                        strokeWidth = 2.5.dp
                    )
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "FOXXYPLAY BY FOXXYOFFC",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.copy(alpha = 0.85f),
                    letterSpacing = 1.2.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "©2026 All Right Reserved",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MoonwalkAnimation() {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        factory = { context ->
            android.webkit.WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = false
                settings.setSupportZoom(false)
                loadUrl("file:///android_asset/foxxy_moonwalk.svg")
            }
        },
        update = {}
    )
}
