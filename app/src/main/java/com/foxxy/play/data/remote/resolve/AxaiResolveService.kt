package com.foxxy.play.data.remote.resolve

import retrofit2.http.GET
import retrofit2.http.Query

data class AxaiResolveResponse(
    val success: Boolean,
    val platform: String?,
    val title: String?,
    val thumbnail: String?,
    val quality: String?,
    val audioUrl: String?
)

interface AxaiResolveService {
    @GET("api/resolve")
    suspend fun resolve(
        @Query("url") spotifyUrl: String
    ): AxaiResolveResponse
}
