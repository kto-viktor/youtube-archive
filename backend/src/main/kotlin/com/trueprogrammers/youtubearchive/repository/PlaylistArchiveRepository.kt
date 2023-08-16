package com.trueprogrammers.youtubearchive.repository

import com.trueprogrammers.youtubearchive.models.entity.PlaylistArchive
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PlaylistArchiveRepository : JpaRepository<PlaylistArchive, String> {
    fun findByTitleContainingIgnoreCase(title: String, page: Pageable): Page<PlaylistArchive>
}
