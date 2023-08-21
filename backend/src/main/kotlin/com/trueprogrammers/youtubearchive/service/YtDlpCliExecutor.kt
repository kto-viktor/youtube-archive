package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class YtDlpCliExecutor {
    private final val utilityName = "yt-dlp"
    private final val bestVideoFormatFilter = "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4] / bv*+ba/b"
    private final val infoPrintFormat = "%(id)s///%(title)s///%(filesize,filesize_approx)s"

    fun processGettingVideoMetadata(url: String): BufferedReader {
        val commands = listOf(
            utilityName,
            "--compat-options",
            "no-youtube-unavailable-videos",
            "--no-warnings",
            "--print",
            infoPrintFormat,
            url,
            "-f",
            bestVideoFormatFilter
        )
        val process = processYtDlp(commands)
        return BufferedReader(InputStreamReader(process.inputStream))
    }

    fun processGettingPlaylistMetadata(url: String): BufferedReader {
        val commands = listOf(
            utilityName,
            "--compat-options",
            "no-youtube-unavailable-videos",
            "--no-warnings",
            "--print",
            "$infoPrintFormat///%(playlist_id)s///%(playlist_title)s",
            url,
            "-f",
            bestVideoFormatFilter
        )
        val process = processYtDlp(commands)
        return BufferedReader(InputStreamReader(process.inputStream))
    }

    fun processVideoDownloading(videoMetadata: VideoMetadata): Process {
        val commands = listOf(
            utilityName,
            videoMetadata.youtubeId,
            "-f",
            bestVideoFormatFilter,
            "-o",
            videoMetadata.title
        )
        return processYtDlp(commands)
    }

    private fun processYtDlp(commands: List<String>): Process {
        val processBuilder = ProcessBuilder(commands)
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
    }
}
