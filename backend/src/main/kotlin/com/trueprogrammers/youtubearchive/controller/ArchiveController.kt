package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class ArchiveController(
    private val videoArchiver: VideoArchiver
) {
    @GetMapping("/video/metadata")
    fun getVideoMetadata(@RequestParam url: String): VideoMetadata {
        return videoArchiver.getVideoMetadata(url)
    }

    @PostMapping("/video")
    fun archiveVideo(@RequestBody metadata: VideoMetadata): UUID {
        return videoArchiver.archiveVideo(metadata)
    }

    @GetMapping("/video/archives")
    fun searchVideoArchives(@RequestParam(required = false) query: String?): List<VideoArchive> {
        return videoArchiver.findVideosByQuery(query)
    }

    @GetMapping("/video/archives/{id}")
    fun getVideoArchiveById(@PathVariable id: UUID): VideoArchive {
        return videoArchiver.findVideoById(id)
    }

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