package com.trueprogrammers.youtubearchive.models.dto

data class PlaylistMetadata(
    val url: String,
    val title: String,
    val videos: MutableList<VideoMetadata>
) {
}