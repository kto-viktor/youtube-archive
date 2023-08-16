package com.trueprogrammers.youtubearchive.controller.handler

import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import com.trueprogrammers.youtubearchive.models.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ArchiveControllerExceptionHandler {
    @ExceptionHandler(value = [IllegalArgumentException::class, AlreadyExistsException::class])
    fun handleInvalidUrlException(ex: Exception) : ResponseEntity<String> {
        return ResponseEntity.badRequest().body(ex.message)
    }

    @ExceptionHandler(value = [ExceededUploadS3LimitException::class])
    fun handleExceededUploadS3LimitException(
        ex: ExceededUploadS3LimitException
    ) : ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.message)
    }

    @ExceptionHandler(value = [NotFoundException::class])
    fun handleNotFoundException(ex: NotFoundException) : ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.message)
    }
}
