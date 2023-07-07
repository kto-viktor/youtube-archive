package com.trueprogrammers.youtubearchive.controller.handler

import com.trueprogrammers.youtubearchive.models.exception.AlreadyExistsException
import com.trueprogrammers.youtubearchive.models.exception.ExceededUploadS3LimitException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class ArchiveControllerExceptionHandler {
    @ExceptionHandler(value = [IllegalArgumentException::class, AlreadyExistsException::class])
    fun handleInvalidUrlException(ex: Exception, request: WebRequest) : ResponseEntity<String> {
        return ResponseEntity.badRequest().body(ex.message)
    }

    @ExceptionHandler(value = [ExceededUploadS3LimitException::class])
    fun handleExceededUploadS3LimitException(ex: ExceededUploadS3LimitException, request: WebRequest) : ResponseEntity<String> {
        return ResponseEntity.status(409).body(ex.message)
    }
}