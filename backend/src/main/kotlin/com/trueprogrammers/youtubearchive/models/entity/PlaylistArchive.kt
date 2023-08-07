package com.trueprogrammers.youtubearchive.models.entity

import javax.persistence.*


@Entity
@Table(name = "playlist_archives")
open class PlaylistArchive(
    @Id
    @Column(name = "id", nullable = false)
    open var id: String?=null,
    open val title: String,
    @ManyToMany
    @JoinTable(
        name = "video_in_playlist",
        joinColumns = [JoinColumn(name = "playlist_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "video_id", referencedColumnName = "id")])
    open val videoArchives: MutableList<VideoArchive>
) {

}