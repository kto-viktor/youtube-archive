package com.trueprogrammers.youtubearchive.models.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table(name = "playlist_archives")
open class PlaylistArchive(
    @Id
    @Column(name = "id", nullable = false)
    open var id: String? = null,
    open val title: String,
    @Column(name = "created_date")
    open val createdDate: LocalDateTime = LocalDateTime.now(),
    @ManyToMany
    @JoinTable(
        name = "video_in_playlist",
        joinColumns = [JoinColumn(name = "playlist_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "video_id", referencedColumnName = "id")]
    )
    open val videoArchives: MutableList<VideoArchive>
)
