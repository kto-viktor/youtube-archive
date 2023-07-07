package com.trueprogrammers.youtubearchive.models.entity

import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "video_archives")
open class VideoArchive(
    open val title: String,
    @Column(name = "youtube_url", nullable = false)
    open val youtubeUrl: String,
    @Column(name = "download_url")
    open var downloadUrl: String? = null,
    @Column(name = "size_mb")
    open var sizeMb: Double? = null,
    @Enumerated(value = EnumType.STRING)
    open var status: Status = Status.IN_PROGRESS,
    open var progress: Int = 0,
    @Column(name = "created_date")
    open val createdDate: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", nullable = false)
    open var id: UUID?=null
) {
}

enum class Status {
    IN_PROGRESS,
    DOWNLOADED,
    ERROR
}