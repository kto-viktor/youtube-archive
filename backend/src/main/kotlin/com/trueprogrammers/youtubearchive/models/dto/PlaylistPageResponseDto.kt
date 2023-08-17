package com.trueprogrammers.youtubearchive.models.dto

import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive

data class PlaylistPageResponseDto(
    val content: List<PlaylistArchive>,
    val totalPages: Int
)
