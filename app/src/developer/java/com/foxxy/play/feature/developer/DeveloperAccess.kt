package com.foxxy.play.feature.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.foxxy.play.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val API_BASE = "https://foxxyplay-api.vercel.app"

private val developerHttp = OkHttpClient.Builder()
    .build()

private data class MaintenanceStatus(
    val enabled: Boolean,
    val message: String
)

private suspend fun apiRequest(
    path: String,
    method: String = "GET",
    body: JSONObject? = null,
    token: String? = null
): JSONObject = withContext(Dispatchers.IO) {
    val requestBuilder = Request.Builder()
        .url("$API_BASE$path")
        .header("Accept", "application/json")

    if (token != null) {
        requestBuilder.header("Authorization", "Bearer $token")
    }

    when (method) {
        "POST" -> {
            val jsonBody = (body ?: JSONObject()).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            requestBuilder.post(jsonBody)
        }
        "DELETE" -> {
            val jsonBody = (body ?: JSONObject()).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            requestBuilder.delete(jsonBody)
        }
        else -> requestBuilder.get()
    }

    developerHttp.newCall(requestBuilder.build()).execute().use { response ->
        val text = response.body?.string().orEmpty()
        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            JSONObject().put("message", "Respons server tidak valid")
        }

        if (!response.isSuccessful) {
            throw IOException(
                json.optString("message", "HTTP ${response.code}")
            )
        }

        json
    }
}

@Composable
fun DeveloperHomeAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.developer_workstation),
            contentDescription = "Developer Panel",
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DeveloperPanelScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var token by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    var maintenance by remember { mutableStateOf(false) }
    var maintenanceMessage by remember { mutableStateOf("") }

    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }

    var blockedIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmUnblockIp by remember { mutableStateOf<String?>(null) }

    fun loadStatus(session: String) {
        scope.launch {
            loading = true
            error = ""
            try {
                val result = apiRequest(
                    path = "/api/app/status",
                    token = session
                )
                maintenance = result.optBoolean("maintenance")
                maintenanceMessage = result.optString("maintenanceMessage")
                info = "Status berhasil dimuat dari server."
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil status."
            } finally {
                loading = false
            }
        }
    }

    fun loadBlockedIps(session: String) {
        scope.launch {
            loading = true
            error = ""
            try {
                val result = apiRequest(
                    path = "/api/developer/locks",
                    token = session
                )
                val array = result.optJSONArray("blockedIps")
                val items = mutableListOf<String>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val ip = array.optString(i)
                        if (ip.isNotBlank()) items.add(ip)
                    }
                }
                blockedIps = items
                info = "Daftar blokir berhasil diperbarui."
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil daftar blokir."
            } finally {
                loading = false
            }
        }
    }

    fun unblockIp(ip: String) {
        val session = token ?: return
        scope.launch {
            loading = true
            error = ""
            info = ""
            try {
                val result = apiRequest(
                    path = "/api/developer/locks",
                    method = "DELETE",
                    token = session,
                    body = JSONObject().put("ip", ip)
                )
                blockedIps = blockedIps.filterNot { it == ip }
                confirmUnblockIp = null
                info = result.optString("message", "Blokir IP berhasil dibuka.")
            } catch (e: Exception) {
                error = e.message ?: "Gagal membuka blokir IP."
            } finally {
                loading = false
            }
        }
    }


    fun login() {
        if (password.isBlank()) {
            error = "Masukkan password admin."
            return
        }

        scope.launch {
            loading = true
            error = ""
            info = ""

            try {
                val result = apiRequest(
                    path = "/api/developer/login",
                    method = "POST",
                    body = JSONObject().put("password", password)
                )

                val newToken = result.optString("token")
                if (newToken.isBlank()) {
                    throw IOException("Token login tidak diterima.")
                }

                token = newToken
                password = ""
                info = "Login berhasil."
                loadStatus(newToken)
                loadBlockedIps(newToken)
            } catch (e: Exception) {
                error = e.message ?: "Login gagal."
            } finally {
                loading = false
            }
        }
    }

    fun saveMaintenance() {
        val session = token ?: return

        scope.launch {
            loading = true
            error = ""
            info = ""

            try {
                val result = apiRequest(
                    path = "/api/app/status",
                    method = "POST",
                    token = session,
                    body = JSONObject()
                        .put("maintenance", maintenance)
                        .put("maintenanceMessage", maintenanceMessage)
                )

                maintenance = result.optBoolean("maintenance")
                maintenanceMessage = result.optString("maintenanceMessage")
                info = result.optString(
                    "message",
                    "Pengaturan berhasil disimpan."
                )
            } catch (e: Exception) {
                error = e.message ?: "Gagal menyimpan pengaturan."
            } finally {
                loading = false
            }
        }
    }

    fun sendAnnouncement() {
        val session = token ?: return
        val title = announcementTitle.trim()
        val message = announcementMessage.trim()

        if (title.isBlank()) {
            error = "Masukkan judul pengumuman."
            return
        }

        if (message.isBlank()) {
            error = "Masukkan isi pengumuman."
            return
        }

        scope.launch {
            loading = true
            error = ""
            info = ""

            try {
                val result = apiRequest(
                    path = "/api/developer/announcements",
                    method = "POST",
                    token = session,
                    body = JSONObject()
                        .put("title", title)
                        .put("message", message)
                )

                val success = result.optInt("successCount")
                val failed = result.optInt("failureCount")
                val total = result.optInt("totalDevices")

                info = "Pengumuman diproses: $success berhasil, $failed gagal dari $total perangkat."
                announcementTitle = ""
                announcementMessage = ""
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengirim pengumuman."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Kembali")
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "Developer Panel",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            "FOXXYPLAY / CONTROL CENTER",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )

        if (token == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Autentikasi Developer",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Masuk menggunakan password admin untuk mengakses kontrol aplikasi."
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password admin") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = { login() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text(if (loading) "Memproses..." else "Login Developer")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Statistik pengguna",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Penghitungan download dan perangkat terdaftar belum diaktifkan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Data statistik akan ditampilkan setelah sistem pencatatan tersedia."
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Pengumuman",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Kirim notifikasi langsung ke perangkat Public FoxxyPlay yang sudah terdaftar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = announcementTitle,
                        onValueChange = {
                            announcementTitle = it.take(80)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Judul pengumuman") },
                        supportingText = {
                            Text("${announcementTitle.length}/80 karakter")
                        },
                        singleLine = true,
                        enabled = !loading
                    )

                    OutlinedTextField(
                        value = announcementMessage,
                        onValueChange = {
                            announcementMessage = it.take(500)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Isi pesan") },
                        supportingText = {
                            Text("${announcementMessage.length}/500 karakter")
                        },
                        minLines = 3,
                        maxLines = 5,
                        enabled = !loading
                    )

                    Button(
                        onClick = { sendAnnouncement() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading &&
                            announcementTitle.isNotBlank() &&
                            announcementMessage.isNotBlank()
                    ) {
                        Text(
                            if (loading) "Mengirim..." 
                            else "Kirim ke Semua Pengguna"
                        )
                    }

                    Text(
                        "Notifikasi hanya dikirim ke perangkat Public APK yang token FCM-nya sudah terdaftar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Keamanan Login",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Batas percobaan login: 3 kali. IP yang terkunci harus dibuka secara manual.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Total IP terblokir: ${blockedIps.size}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = {
                            token?.let { loadBlockedIps(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text("Refresh Daftar Blokir")
                    }

                    if (blockedIps.isEmpty()) {
                        Text(
                            "Belum ada IP yang diblokir.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        blockedIps.forEach { ip ->
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    ip,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = { confirmUnblockIp = ip },
                                    enabled = !loading
                                ) {
                                    Text("Buka Blokir")
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Maintenance",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (maintenance) "Maintenance aktif"
                                else "Aplikasi normal"
                            )
                            Text(
                                if (maintenance) "Akses aplikasi publik perlu dibatasi."
                                else "Aplikasi berjalan normal.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = maintenance,
                            onCheckedChange = { maintenance = it },
                            enabled = !loading
                        )
                    }

                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = {
                            if (it.length <= 500) maintenanceMessage = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pesan maintenance") },
                        placeholder = {
                            Text("Contoh: FoxxyPlay sedang diperbarui.")
                        },
                        minLines = 3,
                        maxLines = 5
                    )

                    Text(
                        "${maintenanceMessage.length}/500 karakter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { saveMaintenance() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text("Simpan Pengaturan")
                    }

                    OutlinedButton(
                        onClick = {
                            token?.let { loadStatus(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text("Muat Ulang Status")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    token = null
                    password = ""
                    error = ""
                    info = "Sesi lokal telah diakhiri."
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text("Logout Developer")
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (info.isNotBlank()) {
            Text(
                info,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        confirmUnblockIp?.let { ip ->
            AlertDialog(
                onDismissRequest = {
                    if (!loading) confirmUnblockIp = null
                },
                title = { Text("Buka blokir IP?") },
                text = {
                    Text("Akses Developer untuk alamat IP $ip akan diaktifkan kembali.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { unblockIp(ip) },
                        enabled = !loading
                    ) {
                        Text("Ya, Buka Blokir")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { confirmUnblockIp = null },
                        enabled = !loading
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        Text(
            "FoxxyPlay Developer • Control Center",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
