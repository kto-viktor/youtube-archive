package com.trueprogrammers.youtubearchive.service.mapper

import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import kotlin.math.floor

@Service
class MetadataParser {
    private val log = LoggerFactory.getLogger(MetadataParser::class.java)
    private val youtubeIdIndex = 0
    private val titleIndex = 1
    private val sizeIndex = 2
    private val playlistIdIndex = 3
    private val playlistTitleIndex = 4
    private final val percentGroup = "percent"
    private final val fragmentGroup = "fragment"
    private final val p = Pattern.compile(
        "\\[download\\]\\s+(?<percent>\\d+\\.\\d)%.*ETA(.*\\(frag (?<fragment>\\d+).*|.*)"
    )

    fun metadataFromString(string: String?): VideoMetadata {
        try {
            val fields = string!!.split("///")
            val size = bytesToMb(fields[sizeIndex].toLong())
            return VideoMetadata(fields[youtubeIdIndex], fields[titleIndex], size)
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException(e)
        }
    }
    fun playlistYoutubeIdFromString(string: String?): String {
        try {
            return string!!.split("///")[playlistIdIndex]
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException(e)
        }
    }
    fun playlistTitleFromString(string: String?): String {
        try {
            return string!!.split("///")[playlistTitleIndex]
        } catch (e: Exception) {
            log.error("Error while parsing answer from yt-dlp with $string line")
            throw IllegalArgumentException(e)
        }
    }

    fun bytesToMb(size: Long) = floor(size.toDouble() / 1024 / 1024 * 100) / 100

    fun getYtDlpDownloadProgressFromString(line: String): Int {
        val m = p.matcher(line)
        return if (m.find() && (m.group(fragmentGroup) == null || m.group(fragmentGroup).toInt() != 1)) {
            m.group(percentGroup).toDouble().toInt()
        } else {
            0
        }
    }
}
