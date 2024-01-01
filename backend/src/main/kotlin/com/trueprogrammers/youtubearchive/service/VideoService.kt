package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.models.ErrorCodeConstants
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import com.trueprogrammers.youtubearchive.models.dto.VideoPageResponseDto
import com.trueprogrammers.youtubearchive.models.entity.Status
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.models.exception.NotFoundException
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import com.trueprogrammers.youtubearchive.service.mapper.MetadataParser
import org.apache.commons.validator.routines.UrlValidator
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class VideoService(
    private val videoArchiveRepository: VideoArchiveRepository,
    private val parser: MetadataParser,
    private val ytDlpCliExecutor: YtDlpCliExecutor,
    private val videoArchiver: VideoArchiver
) {
    private val urlValidator = UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES)
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)

    @Transactional
    fun archiveVideo(youtubeUrl: String): String {
        val metadata = getVideoMetadata(youtubeUrl)
        if (!videoArchiver.checkUploadLimitPerDay(metadata.sizeMb)) {
            throw ExceededUploadS3LimitException(ErrorCodeConstants.exceededLimitUpload)
        }
        val archive = videoArchiveRepository.findById(metadata.youtubeId)
        archive.ifPresent {
            if (it.status === Status.ERROR) {
                videoArchiveRepository.delete(it)
            } else {
                throw AlreadyExistsException("Это видео уже сохранено, попробуйте поискать ${it.title} в списках видео")
            }
        }
        return videoArchiver.saveArchive(metadata).id
    }

    private fun getVideoMetadata(url: String): VideoMetadata {
        require(urlValidator.isValid(url)) { ErrorCodeConstants.invalidVideoUrl }
        try {
            val reader = ytDlpCliExecutor.processGettingVideoMetadata(url)
            val line: String? = reader.readLine()
            log.info("metadata line for video $line")
            return parser.metadataFromString(line)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(ErrorCodeConstants.invalidVideoUrl, e)
        }
    }

    fun findByQuery(page: Int, size: Int, query: String?): VideoPageResponseDto {
        val sortedByDate = PageRequest.of(page, size, Sort.by("createdDate").descending())
        val videoPage = if (query.isNullOrBlank()) {
            videoArchiveRepository.findAll(sortedByDate)
        } else {
            videoArchiveRepository.findByTitleContainingIgnoreCase(query, sortedByDate)
        }
        return VideoPageResponseDto(content = videoPage.content, totalPages = videoPage.totalPages)
    }

    fun getById(id: String): VideoArchive {
        return videoArchiveRepository.findById(id)
            .orElseThrow { NotFoundException(ErrorCodeConstants.videoNotFound) }
    }
}
