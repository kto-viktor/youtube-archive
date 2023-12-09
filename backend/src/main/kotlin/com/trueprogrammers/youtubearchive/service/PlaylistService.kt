package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.models.ErrorCodeConstants
import com.trueprogrammers.youtubearchive.models.dto.PlaylistMetadata
import com.trueprogrammers.youtubearchive.models.dto.PlaylistPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.models.exception.NotFoundException
import com.trueprogrammers.youtubearchive.repository.PlaylistArchiveRepository
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import com.trueprogrammers.youtubearchive.service.mapper.MetadataParser
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class PlaylistService(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val playlistArchiveRepository: PlaylistArchiveRepository,
    private val parser: MetadataParser,
    private val ytDlpCliExecutor: YtDlpCliExecutor,
    private val videoArchiver: VideoArchiver
) {
    private val urlValidator = UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES)
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)

    @Transactional
    fun archivePlaylist(youtubeUrl: String): String {
        val playlistMetadata = getPlaylistMetadata(youtubeUrl)
        if (!videoArchiver.checkUploadLimitPerDay(playlistMetadata.videos.sumOf { metadata -> metadata.sizeMb })) {
            throw ExceededUploadS3LimitException(ErrorCodeConstants.exceededLimitUpload)
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
                    videoArchiveRepository.deleteById(it.id)
                } else {
                    playlistArchive.videoArchives.add(it)
                }
            }, {
                playlistArchive.videoArchives.add(videoArchiver.saveArchive(video))
            })
        }
        playlistArchiveRepository.save(playlistArchive)
        return playlistArchive.id!!
    }

    fun findPlaylistsByQuery(page: Int, size: Int, query: String?): PlaylistPageResponseDto {
        val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
        val playlistPage = if (query.isNullOrBlank()) {
            playlistArchiveRepository.findAll(sortedByDate)
        } else {
            playlistArchiveRepository.findByTitleContainingIgnoreCase(query, sortedByDate)
        }
        return PlaylistPageResponseDto(content = playlistPage.content, totalPages = playlistPage.totalPages)
    }

    fun getPlaylistById(id: String): PlaylistArchive {
        return playlistArchiveRepository.findById(id)
            .orElseThrow { NotFoundException(ErrorCodeConstants.playlistNotFound) }
    }

    private fun getPlaylistMetadata(url: String): PlaylistMetadata {
        require(urlValidator.isValid(url)) { ErrorCodeConstants.invalidPlaylistUrl }
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
            throw IllegalArgumentException(ErrorCodeConstants.invalidPlaylistUrl, e)
        }
    }
}
