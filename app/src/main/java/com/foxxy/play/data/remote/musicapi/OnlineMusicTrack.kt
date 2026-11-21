package com.foxxy.play.data.remote.musicapi

data class OnlineMusicTrack(
    val id: String,
    val source: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val previewUrl: String?,
    val isrc: String?,
    val duration: Long?,
    val externalUrl: String?
)
