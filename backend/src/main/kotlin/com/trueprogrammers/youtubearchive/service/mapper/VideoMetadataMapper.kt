package com.trueprogrammers.youtubearchive.service.mapper

import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.floor

@Service
class VideoMetadataMapper {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    fun metadataFromString(string: String): VideoMetadata {
        val fields = string.split("///")
        val size = try {
            bytesToMb(fields[2].toLong())
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException("Неправильная ссылка - нужна ссылка на youtube-видео")
        }
        return VideoMetadata(fields[0], fields[1], size)
    }

    private fun bytesToMb(size: Long) = floor(size.toDouble() / 1024 / 1024 * 100) / 100
}