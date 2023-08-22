package com.trueprogrammers.youtubearchive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class YoutubeArchiveApplication {
    fun main(args: Array<String>) {
        runApplication<YoutubeArchiveApplication>(args = args)
    }

    @Bean
    fun getExecutorService(): ExecutorService {
        return Executors.newFixedThreadPool(100)
    }
}
