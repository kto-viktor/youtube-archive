package com.trueprogrammers.youtubearchive.service

import com.amazonaws.services.kms.model.NotFoundException
import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.dto.PlaylistPageResponseDto
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.dto.VideoPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.enums.ErrorEnum
import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.repository.PlaylistArchiveRepository
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import com.trueprogrammers.youtubearchive.service.mapper.MetadataParser
import org.apache.commons.validator.routines.UrlValidator
import org.apache.commons.validator.routines.UrlValidator.ALLOW_ALL_SCHEMES
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

@Service
class VideoArchiver(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val playlistArchiveRepository: PlaylistArchiveRepository,
    private val parser: MetadataParser,
    private val s3StorageConnector: S3StorageConnector,
    private val ytDlpCliExecutor: YtDlpCliExecutor,
    private val props: AppProperties
) {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    private val executor = Executors.newFixedThreadPool(100)
    private val urlValidator = UrlValidator(ALLOW_ALL_SCHEMES)

    fun archiveVideo(youtubeUrl: String): String {
        val metadata = getVideoMetadata(youtubeUrl)
        if (!checkUploadLimitPerDay(metadata.sizeMb)) {
            throw ExceededUploadS3LimitException(ErrorEnum.EXCEEDED_LIMIT_UPLOAD.getMessage())
        }
        val archive = videoArchiveRepository.findById(metadata.youtubeId)
        archive.ifPresent {
            if (it.status === Status.ERROR) {
                videoArchiveRepository.delete(it)
            } else {
                throw AlreadyExistsException("Это видео уже сохранено, попробуйте поискать ${it.title} в списках видео")
            }
        }
        return saveArchive(metadata).id!!
    }

    fun archivePlaylist(youtubeUrl: String): String {
        val playlistMetadata = getPlaylistMetadata(youtubeUrl)
        if (!checkUploadLimitPerDay(playlistMetadata.videos.sumOf { metadata -> metadata.sizeMb })) {
            throw ExceededUploadS3LimitException(ErrorEnum.EXCEEDED_LIMIT_UPLOAD.getMessage())
        }
        val playlistArchive = PlaylistArchive(
            id = playlistMetadata.youtubeId,
            title = playlistMetadata.title,
            videoArchives = ArrayList()
        )

        playlistMetadata.videos.forEach { video ->
            val videoArchive = videoArchiveRepository.findById(video.youtubeId)
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
        require(urlValidator.isValid(url)) { ErrorEnum.INVALID_VIDEO_URL.getMessage() }
        try {
            val reader = ytDlpCliExecutor.processGettingVideoMetadata(url)
            val line: String? = reader.readLine()
            log.info("metadata line for video $line")
            return parser.metadataFromString(line)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(ErrorEnum.INVALID_VIDEO_URL.getMessage())
        }
    }

    fun getPlaylistMetadata(url: String): PlaylistMetadata {
        require(urlValidator.isValid(url)) { ErrorEnum.INVALID_PLAYLIST_URL.getMessage() }
        try {
            val reader = ytDlpCliExecutor.processGettingPlaylistMetadata(url)
            var line: String? = reader.readLine()

            val playlistTitle = parser.playlistTitleFromString(line)
            val playlistYoutubeId = parser.playlistYoutubeIdFromString(line)
            log.info("processing $playlistTitle playlist")
            val playlistMetadata = PlaylistMetadata(playlistYoutubeId, playlistTitle, ArrayList())

            do {
                log.info("metadata line for video $line")
                playlistMetadata.videos.add(parser.metadataFromString(line))
            } while (reader.readLine().also { line = it } != null)

            return playlistMetadata
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(ErrorEnum.INVALID_PLAYLIST_URL.getMessage())
        }
    }

    fun findVideosByQuery(page: Int, size: Int, query: String?): VideoPageResponseDto {
        val videoPage = if (query.isNullOrBlank()) {
            val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
            videoArchiveRepository.findAll(sortedByDate)
        } else {
            val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
            videoArchiveRepository.findByTitleContainingIgnoreCase(query, sortedByDate)
        }
        return VideoPageResponseDto(content = videoPage.content, totalPages = videoPage.totalPages)
    }

    fun getVideoById(id: String): VideoArchive {
        return videoArchiveRepository.findById(id)
            .orElseThrow { NotFoundException(ErrorEnum.VIDEO_NOT_FOUND.getMessage()) }
    }

    fun findPlaylistsByQuery(page: Int, size: Int, query: String?): PlaylistPageResponseDto {
        val playlistPage = if (query.isNullOrBlank()) {
            val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
            playlistArchiveRepository.findAll(sortedByDate)
        } else {
            val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
            playlistArchiveRepository.findByTitleContainingIgnoreCase(query, sortedByDate)
        }
        return PlaylistPageResponseDto(content = playlistPage.content, totalPages = playlistPage.totalPages)
    }

    fun getPlaylistById(id: String): PlaylistArchive {
        return playlistArchiveRepository.findById(id)
            .orElseThrow { NotFoundException(ErrorEnum.PLAYLIST_NOT_FOUND.getMessage()) }
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
            VideoArchive(id = metadata.youtubeId, title = metadata.title, sizeMb = metadata.sizeMb)
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