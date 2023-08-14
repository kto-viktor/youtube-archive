package com.trueprogrammers.youtubearchive.models.dto

import com.trueprogrammers.youtubearchive.models.entity.VideoArchive

data class VideoPageResponseDto(
    val content: List<VideoArchive>,
    val totalPages: Int
) {
}
