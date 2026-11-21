package com.foxxy.play.data.download

import android.content.Context
import com.foxxy.play.data.local.DownloadEntity
import com.foxxy.play.data.local.LibraryDao
import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class FoxxyDownloadManager(
    context: Context,
    private val dao: LibraryDao
) {
    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val downloadDirectory =
        File(appContext.filesDir, "downloads").apply {
            mkdirs()
        }

    suspend fun download(
        track: OnlineMusicTrack,
        audioUrl: String
    ): DownloadEntity = withContext(Dispatchers.IO) {

        require(audioUrl.startsWith("https://")) {
            "URL audio tidak aman."
        }

        val existing = dao.getDownload(track.source, track.id)

        if (existing != null && File(existing.audioPath).exists()) {
            return@withContext existing
        }

        val fileKey = hash("${track.source}:${track.id}")
        val audioFile = File(downloadDirectory, "$fileKey.mp3")
        val tempFile = File(downloadDirectory, "$fileKey.mp3.part")

        val request = Request.Builder()
            .url(audioUrl)
            .header("User-Agent", "FoxxyPlay Android")
            .build()

        try {
            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Download gagal: HTTP ${response.code}"
                    )
                }

                val body = response.body
                    ?: throw IllegalStateException("Respons audio kosong.")

                BufferedInputStream(body.byteStream()).use { input ->
                    val first = input.read()
                    val second = input.read()
                    val third = input.read()

                    val isId3 =
                        first == 0x49 &&
                        second == 0x44 &&
                        third == 0x33

                    val isMpegFrame =
                        first == 0xFF &&
                        second >= 0 &&
                        (second and 0xE0) == 0xE0

                    if (!isId3 && !isMpegFrame) {
                        throw IllegalStateException(
                            "Data yang diterima bukan file MP3 yang valid."
                        )
                    }

                    FileOutputStream(tempFile).use { output ->
                        output.write(first)
                        output.write(second)
                        output.write(third)

                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                        }

                        output.flush()
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 3L) {
                throw IllegalStateException("File MP3 kosong.")
            }

            if (audioFile.exists()) audioFile.delete()

            if (!tempFile.renameTo(audioFile)) {
                throw IllegalStateException("Gagal menyimpan file MP3.")
            }

            val coverFile = track.coverUrl
                ?.let { saveCover(it, fileKey) }

            val entity = DownloadEntity(
                source = track.source,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                audioPath = audioFile.absolutePath,
                coverPath = coverFile?.absolutePath,
                duration = track.duration,
                fileSize = audioFile.length()
            )

            dao.addDownload(entity)

            entity
        } catch (error: Exception) {
            tempFile.delete()
            throw error
        }
    }

    private fun saveCover(url: String, fileKey: String): File? {
        return try {
            if (!url.startsWith("https://")) return null

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val body = response.body ?: return null
                val contentType = body.contentType()?.type

                if (contentType != "image") return null

                val file = File(downloadDirectory, "$fileKey.jpg")

                FileOutputStream(file).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }

                file.takeIf { it.length() > 0L }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hash(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
