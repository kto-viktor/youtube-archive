package com.trueprogrammers.youtubearchive.models.exception

class VideoDownloadException : Exception {
    constructor(): super()
    constructor(message: String?): super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}
