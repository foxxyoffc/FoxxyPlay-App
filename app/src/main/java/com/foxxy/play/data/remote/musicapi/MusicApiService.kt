package com.foxxy.play.data.remote.musicapi

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class MusicSearchRequest(
    val type: String = "track",
    val track: String,
    val sources: List<String>
)

data class MusicEmbedRequest(
    val url: String
)

data class MusicEmbedResponse(
    val embedUrl: String?,
    val embedCode: String?
)

interface MusicApiService {

    @Headers("Content-Type: application/json")
    @POST("public/search")
    suspend fun searchTracks(
        @Body request: MusicSearchRequest
    ): JsonObject

    @Headers("Content-Type: application/json")
    @POST("public/embed/url")
    suspend fun createEmbed(
        @Body request: MusicEmbedRequest
    ): MusicEmbedResponse
}
