package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.dto.PlaylistPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class PlaylistController(
    private val videoArchiver: VideoArchiver
) {
    @PostMapping("/playlist")
    fun archivePlaylist(@RequestParam url: String): String {
        return videoArchiver.archivePlaylist(url)
    }

    @GetMapping("/playlist/archives")
    fun searchPlaylistArchives(
        @RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(value = "size", required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) query: String?
    ): PlaylistPageResponseDto {
        return videoArchiver.findPlaylistsByQuery(page, size, query)
    }

    @GetMapping("/playlist/archives/{id}")
    fun getPlaylistArchiveById(@PathVariable id: String): PlaylistArchive {
        return videoArchiver.getPlaylistById(id)
    }
}