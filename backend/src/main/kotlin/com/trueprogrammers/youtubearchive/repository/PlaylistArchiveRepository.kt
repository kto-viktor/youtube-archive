package com.trueprogrammers.youtubearchive.repository

import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import org.springframework.data.jpa.repository.JpaRepository

interface PlaylistArchiveRepository: JpaRepository<PlaylistArchive, String> {
}