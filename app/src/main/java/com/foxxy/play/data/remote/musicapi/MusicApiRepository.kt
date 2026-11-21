package com.foxxy.play.data.remote.musicapi

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicApiRepository(
    private val api: MusicApiService = MusicApiClient.service,
    private val searchApi: FoxxySearchService = FoxxySearchClient.service
) {

    suspend fun search(
        query: String,
        sources: List<String>
    ): Result<List<OnlineMusicTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            require(query.isNotBlank()) {
                "Masukkan judul atau nama artis."
            }

            require(sources.isNotEmpty()) {
                "Pilih minimal satu sumber musik."
            }

            // FoxxyPlay Search API saat ini khusus Spotify.
            require("spotify" in sources) {
                "Pilih sumber musik online untuk mencari lagu."
            }

            val response = searchApi.searchTracks(
                query = query.trim(),
                limit = 20
            )

            if (response.get("success")?.asBoolean != true) {
                throw IllegalStateException(
                    "Pencarian musik online gagal."
                )
            }

            val tracks = response.getAsJsonArray("tracks")
                ?: throw IllegalStateException(
                    "Respons API tidak memiliki daftar lagu."
                )

            buildList {
                tracks.forEach { element ->
                    if (!element.isJsonObject) return@forEach

                    val data = element.asJsonObject

                    val id = data.string("id")
                        ?: return@forEach

                    val title = data.string("title")
                        ?: return@forEach

                    val artists = data.getAsJsonArray("artist")
                        ?.mapNotNull {
                            if (it.isJsonPrimitive) it.asString else null
                        }
                        .orEmpty()

                    val spotifyUrl = data.string("spotifyUrl")
                        ?: "https://open.spotify.com/track/$id"

                    add(
                        OnlineMusicTrack(
                            id = id,
                            source = "spotify",
                            title = title,
                            artist = artists.joinToString(", ")
                                .ifBlank { "Unknown Artist" },
                            album = data.string("album") ?: "",
                            coverUrl = data.string("coverUrl"),
                            previewUrl = null,
                            isrc = null,
                            duration = data.long("duration"),
                            externalUrl = spotifyUrl
                        )
                    )
                }
            }.distinctBy { it.id }
        }
    }

    // Tetap dipertahankan untuk kompatibilitas dengan kode lama.
    suspend fun createEmbed(url: String): Result<MusicEmbedResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(url.startsWith("https://")) {
                    "URL provider tidak valid."
                }

                api.createEmbed(MusicEmbedRequest(url))
            }
        }

    private fun JsonObject.string(key: String): String? {
        val value = get(key) ?: return null
        if (!value.isJsonPrimitive || value.isJsonNull) return null

        return runCatching { value.asString }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.long(key: String): Long? {
        val value = get(key) ?: return null
        return runCatching { value.asLong }.getOrNull()
    }
}
