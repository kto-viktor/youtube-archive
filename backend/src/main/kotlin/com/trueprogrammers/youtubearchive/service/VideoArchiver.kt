package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.exception.VideoDownloadException
import com.trueprogrammers.youtubearchive.models.exception.VideoUploadException
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import com.trueprogrammers.youtubearchive.service.mapper.MetadataParser
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService

@Service
class VideoArchiver(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val parser: MetadataParser,
    private val videoUploader: VideoUploader,
    private val ytDlpCliExecutor: YtDlpCliExecutor,
    private val props: AppProperties,
    private val executor: ExecutorService
) {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    private val generalProgressOfDownloadedVideo = 50

    fun checkUploadLimitPerDay(sizeMb: Double): Boolean {
        val startDate: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val endDate: LocalDateTime = startDate.plusDays(1)
        val sizeOfUploadedVideos: Double = try {
            videoArchiveRepository.getTotalSizeInDates(startDate, endDate)
        } catch (e: EmptyResultDataAccessException) {
            log.warn("Today no downloads, $e exception produced")
            0.0
        }
        val gbSizeWithVideo: Double = (sizeOfUploadedVideos + sizeMb) / 1024

        return (props.s3.uploadDailyLimitGb == 0) || gbSizeWithVideo <= props.s3.uploadDailyLimitGb
    }

    fun saveArchive(metadata: VideoMetadata): VideoArchive {
        log.info("submitten for archive {}", metadata)
        val videoArchive = videoArchiveRepository.save(
            VideoArchive(id = metadata.youtubeId, title = metadata.title, sizeMb = metadata.sizeMb)
        )
        startAsyncArchiving(videoArchive, metadata)
        return videoArchive
    }

    private fun startAsyncArchiving(videoArchive: VideoArchive, metadata: VideoMetadata) {
        executor.submit {
            try {
                val exitCode = processAndLogDownload(ytDlpCliExecutor.processVideoDownloading(metadata), videoArchive)

                if (exitCode != 0) {
                    throw VideoDownloadException("Exit code non-zero: $exitCode")
                } else {
                    log.info("successfully downloaded video $metadata")
                    val file = File(videoArchive.title + ".mp4")
                    videoArchive.progress = generalProgressOfDownloadedVideo
                    videoArchive.sizeMb = parser.bytesToMb(file.length())
                    videoArchive.downloadUrl = videoUploader.uploadVideo(file, videoArchive)
                    videoArchive.status = Status.DOWNLOADED
                    videoArchive.progress = 100
                }
            } catch (e: VideoDownloadException) {
                log.error("Error while executing yt-dlp for video $metadata", e)
                videoArchive.status = Status.ERROR
            } catch (e: VideoUploadException) {
                log.error("Error while uploading video $metadata", e)
                videoArchive.status = Status.ERROR
            } finally {
                videoArchiveRepository.save(videoArchive)
            }
        }
    }

    private fun processAndLogDownload(process: Process, videoArchive: VideoArchive): Int {
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            val downloadTracker = Runnable {
                try {
                    while (reader.readLine().also { line = it } != null) {
                        log.debug("[id ${videoArchive.id}]: $line")
                        val currProgress = parser.getYtDlpDownloadProgressFromString(line!!) / 2
                        log.debug("curr parsed progress = $currProgress, video archive id = ${videoArchive.id}")
                        videoArchiveRepository.updateProgressById(videoArchive.id, currProgress)
                        Thread.sleep(props.upload.updateProgressPeriodMillis)
                    }
                } catch (e: IOException) {
                    log.debug("yt-dlp download stream closed, $e")
                }
            }
            executor.execute(downloadTracker)

            while (reader.readLine().also { line = it } != null) {
                log.debug("[id ${videoArchive.id}]: $line")
            }

            return process.waitFor()
        }
    }
}
