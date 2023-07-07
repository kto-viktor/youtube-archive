package com.trueprogrammers.youtubearchive.service

import com.amazonaws.services.kms.model.NotFoundException
import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.repository.PlaylistArchiveRepository
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import com.trueprogrammers.youtubearchive.service.mapper.VideoMetadataMapper
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

@Service
class VideoArchiver(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val playlistArchiveRepository: PlaylistArchiveRepository,
    private val mapper: VideoMetadataMapper,
    private val s3StorageConnector: S3StorageConnector,
    private val ytDlpCliExecutor: YtDlpCliExecutor,
    private val props: AppProperties
) {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    private val executor = Executors.newFixedThreadPool(100)
    private val urlValidator = UrlValidator()

    fun archiveVideo(metadata: VideoMetadata): UUID {
        if (!checkUploadLimitPerDay(metadata.sizeMb)) {
            throw ExceededUploadS3LimitException("Лимит загрузки превышен, попробуйте завтра")
        }
        val archive = videoArchiveRepository.findByYoutubeUrl(metadata.url)
        archive.ifPresent {
            if (it.status === Status.ERROR) {
                videoArchiveRepository.delete(it)
            } else {
                throw AlreadyExistsException("Это видео уже сохранено, попробуйте поискать ${it.title} в списках видео")
            }
        }
        return saveArchive(metadata).id!!
    }

    fun archivePlaylist(playlistMetadata: PlaylistMetadata): UUID {
        if (!checkUploadLimitPerDay(playlistMetadata.videos.sumOf { metadata -> metadata.sizeMb })) {
            throw ExceededUploadS3LimitException("Лимит загрузки превышен, попробуйте завтра")
        }
        val playlistArchive = PlaylistArchive(playlistMetadata.url, playlistMetadata.title, ArrayList())

        playlistMetadata.videos.forEach { video ->
            val videoArchive = videoArchiveRepository.findByYoutubeUrl(video.url)
            videoArchive.ifPresentOrElse({
                if (it.status === Status.ERROR) {
                    videoArchiveRepository.delete(it)
                } else {
                    playlistArchive.videoArchives.add(it)
                }
            }, {
                playlistArchive.videoArchives.add(saveArchive(video))
            })
        }
        playlistArchiveRepository.save(playlistArchive)
        return playlistArchive.id!!
    }

    fun getVideoMetadata(url: String): VideoMetadata {
        require(urlValidator.isValid(url)) { "Неправильная ссылка - нужна ссылка на youtube-видео" }

        val reader = ytDlpCliExecutor.processGettingVideoMetadata(url)
        val line: String? = reader.readLine()
        log.info("metadata line for video $line")
        return mapper.metadataFromString(line!!)
    }

    fun getPlaylistMetadata(url: String): PlaylistMetadata {
        require(urlValidator.isValid(url)) { "Неправильная ссылка - нужна ссылка на youtube-плейлист" }

        val reader = ytDlpCliExecutor.processGettingPlaylistMetadata(url)
        var line: String? = reader.readLine()
        val playlistTitle = line!!.split("///")[3]
        log.info("processing $playlistTitle playlist")
        val playlistMetadata = PlaylistMetadata(url, playlistTitle, ArrayList())

        do {
            log.info("metadata line for video $line")
            playlistMetadata.videos.add(mapper.metadataFromString(line!!))
        } while (reader.readLine().also { line = it } != null)

        return playlistMetadata
    }

    fun findVideosByQuery(query: String?): List<VideoArchive> {
        return if (query.isNullOrBlank()) {
            videoArchiveRepository.findAll().sortedByDescending { it.createdDate }
        } else {
            videoArchiveRepository.findAll().filter { it.title.contains(query, true) }
        }
    }

    fun findVideoById(id: UUID): VideoArchive {
        return videoArchiveRepository.findById(id).orElseThrow { NotFoundException("not found") }
    }

    fun findPlaylistsByQuery(query: String?): List<PlaylistArchive> {
        return if (query.isNullOrBlank()) {
            playlistArchiveRepository.findAll().sortedByDescending { it.videoArchives[0].createdDate }
        } else {
            playlistArchiveRepository.findAll().filter { it.title.contains(query, true) }
        }
    }

    fun findPlaylistById(id: UUID): PlaylistArchive {
        return playlistArchiveRepository.findById(id).orElseThrow { NotFoundException("not found") }
    }

    private fun checkUploadLimitPerDay(sizeMb: Double): Boolean {
        val startDate: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val endDate: LocalDateTime = startDate.plusDays(1)
        val sizeOfUploadedVideos: Double = try {
            videoArchiveRepository.getTotalSizeInDates(startDate, endDate)
        } catch (e: EmptyResultDataAccessException) {
            0.0
        }
        val gbSizeWithVideo: Double = (sizeOfUploadedVideos + sizeMb) / 1024

        return (props.s3.uploadDailyLimitGb == 0) || gbSizeWithVideo <= props.s3.uploadDailyLimitGb
    }

    private fun saveArchive(metadata: VideoMetadata): VideoArchive {
        log.info("submitten for archive {}", metadata)
        val videoArchive = videoArchiveRepository.save(
            VideoArchive(title = metadata.title, youtubeUrl = metadata.url, sizeMb = metadata.sizeMb)
        )
        startAsyncArchiving(videoArchive, metadata)
        return videoArchive
    }

    private fun startAsyncArchiving(videoArchive: VideoArchive, metadata: VideoMetadata) {
        executor.submit {
            var reader: BufferedReader? = null
            try {
                val process = ytDlpCliExecutor.processVideoDownloading(metadata)
                reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    log.debug("[id ${videoArchive.id}]: $line")
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw Exception("Exit code non-zero: $exitCode")
                } else {
                    log.info("successfully downloaded video $metadata")
                    videoArchive.progress = 50
                    videoArchiveRepository.save(videoArchive)
                    val downloadUrl = s3StorageConnector.uploadToS3(metadata)
                    videoArchive.downloadUrl = downloadUrl
                    videoArchive.status = Status.DOWNLOADED
                    videoArchive.progress = 100
                    videoArchiveRepository.save(videoArchive)
                }
            } catch (e: Exception) {
                log.error("Error when executing yt-dlp for video $metadata", e)
                videoArchive.status = Status.ERROR
                videoArchiveRepository.save(videoArchive)
            } finally {
                reader?.close()
            }
        }
    }
}