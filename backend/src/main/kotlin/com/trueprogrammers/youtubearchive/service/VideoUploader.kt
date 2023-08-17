package com.trueprogrammers.youtubearchive.service

import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import com.trueprogrammers.youtubearchive.repository.VideoArchiveRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class VideoUploader(
    val s3StorageConnector: S3StorageConnector,
    val videoRepository: VideoArchiveRepository,
    val props: AppProperties
) {
    private val log = LoggerFactory.getLogger(VideoUploader::class.java)
    private val generalProgressOfDownloadedVideo = 50
    fun uploadVideo(videoArchive: VideoArchive): String {
        val filename = videoArchive.title + ".mp4"
        val file = File(filename)
        val upload = s3StorageConnector.uploadToS3(file)

        val tracker = Runnable {
            val progress = upload.progress
            while (!upload.isDone) {
                log.debug("progress of uploading video ${videoArchive.title} is ${progress.percentTransferred}")
                val generalVideoProgress = generalProgressOfDownloadedVideo + progress.percentTransferred.toInt() / 2
                videoRepository.updateProgressById(videoArchive.id, generalVideoProgress)
                Thread.sleep(props.upload.updateProgressPeriodMillis)
            }
        }
        val trackerThread = Thread(tracker)
        trackerThread.start()

        upload.waitForCompletion()
        log.info("successfully uploaded video $file.name")
        file.delete()
        return s3StorageConnector.getUploadedVideoUrl(file.name)
    }
}
