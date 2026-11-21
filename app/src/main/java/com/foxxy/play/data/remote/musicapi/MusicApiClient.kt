package com.foxxy.play.data.remote.musicapi

import com.foxxy.play.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MusicApiClient {

    private const val BASE_URL = "https://api.musicapi.com/"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder()
            .header(
                "Authorization",
                "Token ${BuildConfig.MUSICAPI_CLIENT_ID}"
            )
            .header("Accept", "application/json")
            .build()

        chain.proceed(request)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApiService::class.java)
    }
}
