package com.trueprogrammers.youtubearchive.repository

import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlaylistArchiveRepository: JpaRepository<PlaylistArchive, UUID> {
}