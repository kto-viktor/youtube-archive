package com.trueprogrammers.youtubearchive.models.dto

data class PlaylistMetadata(
    val youtubeId: String,
    val title: String,
    val videos: MutableList<VideoMetadata>
)
