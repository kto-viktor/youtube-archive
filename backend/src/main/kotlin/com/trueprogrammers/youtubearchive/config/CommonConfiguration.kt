package com.trueprogrammers.youtubearchive.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class CommonConfiguration {
    @Bean
    fun getExecutorService(): ExecutorService {
        return Executors.newFixedThreadPool(100)
    }
}
