package com.trueprogrammers.youtubearchive.service.mapper

import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.service.VideoArchiver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.floor

@Service
class MetadataParser {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    fun metadataFromString(string: String?): VideoMetadata {
        try {
            val fields = string!!.split("///")
            val size = bytesToMb(fields[2].toLong())
            return VideoMetadata(fields[0], fields[1], size)
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException()
        }
    }
    fun playlistYoutubeIdFromString(string: String?): String {
        try {
            return string!!.split("///")[3]
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException()
        }
    }
    fun playlistTitleFromString(string: String?): String {
        try {
            return string!!.split("///")[4]
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException()
        }
    }

    private fun bytesToMb(size: Long) = floor(size.toDouble() / 1024 / 1024 * 100) / 100
}