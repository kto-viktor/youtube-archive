package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ExecutorService

@Service
class VideoUploader(
    private val s3StorageConnector: S3StorageConnector,
    private val videoRepository: VideoArchiveRepository,
    private val props: AppProperties,
    private val executor: ExecutorService
) {
    private val log = LoggerFactory.getLogger(VideoUploader::class.java)
    private val generalProgressOfDownloadedVideo = 50
    fun uploadVideo(file: File, videoArchive: VideoArchive): String {
        val upload = s3StorageConnector.uploadToS3(file)

        val tracker = Runnable {
            val progress = upload.progress
            while (!upload.isDone) {
                log.debug("progress of uploading video ${videoArchive.title} is ${progress.percentTransferred}")
                val generalVideoProgress =
                    generalProgressOfDownloadedVideo + progress.percentTransferred.toInt() / 2
                videoRepository.updateProgressById(videoArchive.id, generalVideoProgress)
                Thread.sleep(props.upload.updateProgressPeriodMillis)
            }
        }
        executor.execute(tracker)

        upload.waitForCompletion()
        log.info("successfully uploaded video $file.name")
        file.delete()
        return s3StorageConnector.getUploadedVideoUrl(file.name)
    }
}
