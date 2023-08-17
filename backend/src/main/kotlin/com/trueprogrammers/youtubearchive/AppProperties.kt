package com.trueprogrammers.youtubearchive

import lombok.Getter
import lombok.RequiredArgsConstructor
import lombok.Setter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.lang.NonNull

@ConstructorBinding
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    @NonNull val s3: S3
) {
    @Getter
    @Setter
    @RequiredArgsConstructor
    data class S3(
        @NonNull val uploadDailyLimitGb: Int,
        @NonNull val serviceEndpoint: String,
        @NonNull val signingRegion: String,
        @NonNull val accessKey: String,
        @NonNull val secretKey: String,
        @NonNull val bucketName: String
    )
}
