package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.dto.PlaylistPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.service.PlaylistService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlaylistController(
    private val playlistService: PlaylistService
) {
    @PostMapping("/playlist")
    fun archivePlaylist(@RequestParam url: String): String {
        return playlistService.archivePlaylist(url)
    }

    @GetMapping("/playlist/archives")
    fun searchPlaylistArchives(
        @RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(value = "size", required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) query: String?
    ): PlaylistPageResponseDto {
        return playlistService.findPlaylistsByQuery(page, size, query)
    }

    @GetMapping("/playlist/archives/{id}")
    fun getPlaylistArchiveById(@PathVariable id: String): PlaylistArchive {
        return playlistService.getPlaylistById(id)
    }
}
