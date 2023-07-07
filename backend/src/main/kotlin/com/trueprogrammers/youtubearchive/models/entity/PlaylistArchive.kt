package com.trueprogrammers.youtubearchive.models.entity

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*


@Entity
@Table(name = "playlist_archives")
open class PlaylistArchive(
    open val url: String,
    open val title: String,
    @ManyToMany
    @JoinTable(
        name = "video_in_playlist",
        joinColumns = [JoinColumn(name = "playlist_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "video_id", referencedColumnName = "id")])
    open val videoArchives: MutableList<VideoArchive>,
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