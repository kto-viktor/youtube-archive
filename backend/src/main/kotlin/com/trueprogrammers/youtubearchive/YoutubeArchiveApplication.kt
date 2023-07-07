package com.trueprogrammers.youtubearchive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class YoutubeArchiveApplication

fun main(args: Array<String>) {
	runApplication<YoutubeArchiveApplication>(*args)
}