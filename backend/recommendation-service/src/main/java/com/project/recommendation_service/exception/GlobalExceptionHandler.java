package com.project.recommendation_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<List<ApiError>> hendleIllegalArgumentException(IllegalArgumentException exception) {
        return buildError(
                exception.getMessage(),
                "null value",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<List<ApiError>> hendleContentNotFoundException(ContentNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Content Not Found",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InteractionNotFoundException.class)
    public ResponseEntity<List<ApiError>> hendleInteractionNotFoundException(InteractionNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Interaction Not Found",
                HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<List<ApiError>> buildError(
            String keyError,
            String valueError,
            HttpStatus status) {

        List<ApiError> error = List.of(
                ApiError.builder()
                        .keyError(keyError)
                        .valueError(valueError)
                        .timeStamp(LocalDateTime.now())
                        .build());

        return new ResponseEntity<>(error, status);
    }
}
