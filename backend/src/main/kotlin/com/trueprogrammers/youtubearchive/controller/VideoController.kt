package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class VideoController(
    private val videoArchiver: VideoArchiver
) {
    @PostMapping("/video")
    fun archiveVideo(@RequestBody youtubeUrl: String): String {
        return videoArchiver.archiveVideo(youtubeUrl)
    }

    @GetMapping("/video/archives")
    fun searchVideoArchives(@RequestParam(required = false) query: String?): List<VideoArchive> {
        return videoArchiver.findVideosByQuery(query)
    }

    @GetMapping("/video/archives/{id}")
    fun getVideoArchiveById(@PathVariable id: String): VideoArchive {
        return videoArchiver.findVideoById(id)
    }
}