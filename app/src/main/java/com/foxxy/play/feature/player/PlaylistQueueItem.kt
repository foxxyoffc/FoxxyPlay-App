package com.foxxy.play.feature.player

import com.foxxy.play.data.remote.musicapi.OnlineMusicTrack

sealed class PlaylistQueueItem {
    data class Local(
        val track: MusicTrack
    ) : PlaylistQueueItem()

    data class Online(
        val track: OnlineMusicTrack
    ) : PlaylistQueueItem()
}
