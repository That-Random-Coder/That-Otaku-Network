package com.project.api_gateway.exception;

import com.auth0.jwt.exceptions.JWTDecodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice
public class exceptionHandling {

    @ExceptionHandler(JWTDecodeException.class)
    public ResponseEntity<List<ApiError>> handJwtException(JWTDecodeException exception){
        List<ApiError> errors = List.of(
                ApiError.builder()
                        .keyError(exception.getMessage())
                        .valueError("Jwts Expire")
                        .timeStamp(LocalDateTime.now())
                        .build()
        );

        return new ResponseEntity<>(errors , HttpStatus.UNAUTHORIZED);
    }

}
