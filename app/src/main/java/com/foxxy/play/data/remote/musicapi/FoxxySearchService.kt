package com.foxxy.play.data.remote.musicapi

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface FoxxySearchService {

    @GET("api/search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): JsonObject
}
