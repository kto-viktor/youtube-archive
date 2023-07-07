package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class PlaylistController(
    private val videoArchiver: VideoArchiver
) {
    @GetMapping("/playlist/metadata")
    fun getPlaylistMetadata(@RequestParam url: String): PlaylistMetadata {
        return videoArchiver.getPlaylistMetadata(url)
    }

    @PostMapping("/playlist")
    fun archivePlaylist(@RequestBody playlistMetadata: PlaylistMetadata): UUID {
        return videoArchiver.archivePlaylist(playlistMetadata)
    }

    @GetMapping("/playlist/archives")
    fun searchPlaylistArchives(@RequestParam(required = false) query: String?): List<PlaylistArchive> {
        return videoArchiver.findPlaylistsByQuery(query)
    }

    @GetMapping("/playlist/archives/{id}")
    fun getPlaylistArchiveById(@PathVariable id: UUID): PlaylistArchive {
        return videoArchiver.findPlaylistById(id)
    }
}