package com.trueprogrammers.youtubearchive.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.kms.model.NotFoundException
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.repository.PlaylistArchiveRepository
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

@Service
class VideoArchiver(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val mapper: VideoMetadataMapper,
    @Value("\${app.s3.upload-limit-gb}") private var uploadLimitPerDayGb: Int = 0,
    @Value("\${app.s3.access-key}") private var accessKey: String,
    @Value("\${app.s3.secret-key}") private val secretKey: String,
    @Value("\${app.s3.service-endpoint}") private var serviceEndpoint: String,
    @Value("\${app.s3.signing-region}") private val signingRegion: String,
    private val playlistArchiveRepository: PlaylistArchiveRepository
) {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    private final val credentials =
        AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey))
    private val s3Client = AmazonS3ClientBuilder.standard()
        .withCredentials(credentials)
        .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion))
        .build()
    private val executor = Executors.newFixedThreadPool(100)
    private val urlValidator = UrlValidator()

    fun archiveVideo(metadata: VideoMetadata): UUID {
        if (!checkUploadLimitPerDay(metadata.sizeMb)) {
            throw ExceededUploadS3LimitException("Лимит загрузки превышен, попробуйте завтра")
        }
        val archive = videoArchiveRepository.findByYoutubeUrl(metadata.url)
        if (archive.isPresent) {
            if (archive.get().status === Status.ERROR) {
                videoArchiveRepository.delete(archive.get())
            } else {
                throw AlreadyExistsException("Это видео уже сохранено, попробуйте поискать его в списках видео")
            }
        }
        return saveArchive(metadata).id!!
    }

    fun archivePlaylist(playlistMetadata: PlaylistMetadata): UUID {
        if (!checkUploadLimitPerDay(playlistMetadata.videos.sumOf { metadata -> metadata.sizeMb })) {
            throw ExceededUploadS3LimitException("Лимит загрузки превышен, попробуйте завтра")
        }
        val playlistArchive = PlaylistArchive(playlistMetadata.url, playlistMetadata.title, ArrayList())

        playlistMetadata.videos.forEach {
            val videoArchive = videoArchiveRepository.findByYoutubeUrl(it.url)
            if (videoArchive.isPresent) {
                playlistArchive.videoArchives.add(videoArchive.get())
            } else {
                playlistArchive.videoArchives.add(saveArchive(it))
            }
        }
        playlistArchiveRepository.save(playlistArchive)
        return playlistArchive.id!!
    }

    fun getVideoMetadata(url: String): VideoMetadata {
        require(urlValidator.isValid(url)) { "Неправильная ссылка - нужна ссылка на youtube-видео" }
        val commands = listOf(
            "yt-dlp",
            "--compat-options",
            "no-youtube-unavailable-videos",
            "--no-warnings",
            "--print",
            "%(webpage_url)s///%(title)s///%(filesize,filesize_approx)s",
            url,
            "-f",
            "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4] / bv*+ba/b"
        )
        val process = processYtDlp(commands)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line: String? = reader.readLine()
        log.info("metadata line from ${commands.reduce { acc, s -> "$acc $s" }} for video $line")
        return mapper.metadataFromString(line!!)
    }

    fun getPlaylistMetadata(url: String): PlaylistMetadata {
        require(urlValidator.isValid(url)) { "Неправильная ссылка - нужна ссылка на youtube-видео" }
        val commands = listOf(
                "yt-dlp",
                "--compat-options",
                "no-youtube-unavailable-videos",
                "--no-warnings",
                "--print",
                "%(webpage_url)s///%(title)s///%(filesize,filesize_approx)s///%(playlist_title)s",
                url,
                "-f",
                "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4] / bv*+ba/b"
            )
        val process = processYtDlp(commands)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String? = reader.readLine()
        val playlistTitle = line!!.split("///")[3]
        log.info("processing $playlistTitle playlist")
        val playlistMetadata = PlaylistMetadata(url, playlistTitle, ArrayList())
        do {
            log.info("metadata line from ${commands.reduce { acc, s -> "$acc $s" }} for video $line")
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
            videoArchiveRepository.getSumInDate(startDate, endDate)
        } catch (e: EmptyResultDataAccessException) {
            0.0
        }
        val gbSizeWithVideo: Double = (sizeOfUploadedVideos + sizeMb) / 1024

        return if (uploadLimitPerDayGb == 0) true else gbSizeWithVideo <= uploadLimitPerDayGb
    }

    private fun processYtDlp(commands: List<String>): Process {
        val processBuilder = ProcessBuilder(commands)
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
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
                val process = processYtDlp(
                    listOf(
                        "yt-dlp",
                        metadata.url,
                        "-f",
                        "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4] / bv*+ba/b",
                        "-o",
                        metadata.title
                    )
                )
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
                    val downloadUrl = uploadToS3(metadata)
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

    private fun uploadToS3(metadata: VideoMetadata): String? {
        val filename = metadata.title + ".mp4"
        val file = File(filename)
        try {
            val s3Metadata = ObjectMetadata()
            s3Metadata.contentLength = file.length()
            s3Metadata.contentType = "video/mp4"
            s3Client.putObject("youtube-archive", filename, file.inputStream(), s3Metadata)
            val downloadUrl = s3Client.getUrl("youtube-archive", filename).toExternalForm()
            log.info("successfully uploaded video $metadata")
            return downloadUrl
        } finally {
            file.delete()
        }
    }
}