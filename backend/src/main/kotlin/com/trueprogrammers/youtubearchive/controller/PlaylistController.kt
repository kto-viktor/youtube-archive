package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class PlaylistController(
    private val videoArchiver: VideoArchiver
) {
    @PostMapping("/playlist")
    fun archivePlaylist(@RequestBody youtubeUrl: String): String {
        return videoArchiver.archivePlaylist(youtubeUrl)
    }

    @GetMapping("/playlist/archives")
    fun searchPlaylistArchives(@RequestParam(required = false) query: String?): List<PlaylistArchive> {
        return videoArchiver.findPlaylistsByQuery(query)
    }

    @GetMapping("/playlist/archives/{id}")
    fun getPlaylistArchiveById(@PathVariable id: String): PlaylistArchive {
        return videoArchiver.findPlaylistById(id)
    }
}