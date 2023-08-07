package com.trueprogrammers.youtubearchive.repository

import com.trueprogrammers.youtubearchive.models.entity.VideoArchive
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

interface VideoArchiveRepository: JpaRepository<VideoArchive, String> {
    @Query("SELECT sum(v.sizeMb) from VideoArchive v where v.createdDate between ?1 and ?2")
    fun getTotalSizeInDates(start: LocalDateTime, end: LocalDateTime): Double
}