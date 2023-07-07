package com.trueprogrammers.youtubearchive.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.trueprogrammers.youtubearchive.AppProperties
import com.trueprogrammers.youtubearchive.models.dto.VideoMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class S3StorageConnector(
    props: AppProperties
) {
    private val log = LoggerFactory.getLogger(VideoArchiver::class.java)
    private final val credentials =
        AWSStaticCredentialsProvider(BasicAWSCredentials(props.s3.accessKey, props.s3.secretKey))
    private val s3Client = AmazonS3ClientBuilder.standard()
        .withCredentials(credentials)
        .withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                props.s3.serviceEndpoint,
                props.s3.signingRegion
            )
        )
        .build()

    fun uploadToS3(metadata: VideoMetadata): String? {
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