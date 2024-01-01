package com.trueprogrammers.youtubearchive.controller

import com.trueprogrammers.youtubearchive.models.dto.VideoPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.service.VideoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class VideoController(
    private val videoService: VideoService
) {
    @PostMapping("/video")
    fun archiveVideo(@RequestParam url: String): String {
        return videoService.archiveVideo(url)
    }

    @GetMapping("/video/archives")
    fun searchVideoArchives(
        @RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(value = "size", required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) query: String?
    ): VideoPageResponseDto {
        return videoService.findByQuery(page, size, query)
    }

    @GetMapping("/video/archives/{id}")
    fun getVideoArchiveById(@PathVariable id: String): VideoArchive {
        return videoService.getById(id)
    }
}
