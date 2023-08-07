package com.trueprogrammers.youtubearchive.models.enums

enum class ErrorEnum(private val message: String) {
    INVALID_VIDEO_URL("Неправильная ссылка - нужна ссылка на youtube-видео"),
    INVALID_PLAYLIST_URL("Неправильная ссылка - нужна ссылка на youtube-плейлист"),
    VIDEO_NOT_FOUND("Видео не найдено"),
    PLAYLIST_NOT_FOUND("Плейлист не найден"),
    EXCEEDED_LIMIT_UPLOAD("Лимит загрузки превышен, попробуйте завтра");

    fun getMessage(): String {
        return message
    }
}