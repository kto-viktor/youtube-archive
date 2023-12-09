package com.trueprogrammers.youtubearchive.models.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "video_archives")
open class VideoArchive(
    @Id
    @Column(name = "id", nullable = false)
    open var id: String,
    open val title: String,
    @Column(name = "download_url")
    open var downloadUrl: String? = null,
    @Column(name = "size_mb")
    open var sizeMb: Double? = null,
    @Enumerated(value = EnumType.STRING)
    open var status: Status = Status.IN_PROGRESS,
    open var progress: Int = 0,
    @Column(name = "created_date")
    open val createdDate: LocalDateTime = LocalDateTime.now()
)

enum class Status {
    IN_PROGRESS,
    DOWNLOADED,
    ERROR
}
